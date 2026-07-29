// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.isosurface;

import java.io.*;
import java.nio.*;
import java.util.*;
import driftwood.util.SoftLog;
import jnt.FFT.ComplexDouble3DFFT;
//}}}
/**
* <code>MtzVertexSource</code> manages vertex information built
* from electron density maps computed via inverse FFT from
* MTZ reflection files containing map coefficients (e.g. FWT/PHWT).
*
* <p>MTZ files contain structure factor amplitudes and phases rather
* than pre-computed density grids. This class reads the reflection data,
* auto-detects appropriate F/PHI column pairs, and computes the
* real-space electron density via inverse 3D FFT.
*
* <p>Copyright (C) 2003 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Jun 12 2024
*/
public class MtzVertexSource extends CrystalVertexSource
{
//{{{ Constants
    /** Map kinds; these match the type strings understood by king's EDMapWindow. */
    public static final String KIND_2FOFC     = "2Fo-Fc";
    public static final String KIND_FOFC      = "Fo-Fc";
    public static final String KIND_ANOMALOUS = "anomalous";

    /**
    * Known {F, PHI, kind} column triples, in priority order for auto-detection.
    * A file may contain several of these; all matches are reported by
    * getAvailableCoefficients(), and any one of them can be selected.
    */
    static final String[][] COLUMN_PAIRS = {
        {"FWT",           "PHWT",           KIND_2FOFC},    // Refmac weighted map
        {"2FOFCWT",       "PH2FOFCWT",      KIND_2FOFC},    // Phenix 2Fo-Fc weighted
        {"2FOFC",         "PH2FOFC",        KIND_2FOFC},    // Phenix 2Fo-Fc (older naming)
        {"2FOFCWT_fill",  "PH2FOFCWT_fill", KIND_2FOFC},    // Phenix filled map
        {"F_phi.F",       "F_phi.phi",      KIND_2FOFC},    // BUSTER
        {"DELFWT",        "PHDELWT",        KIND_FOFC},     // Refmac difference map
        {"FOFCWT",        "PHFOFCWT",       KIND_FOFC},     // Phenix Fo-Fc weighted
        {"FOFC",          "PHFOFC",         KIND_FOFC},     // Fo-Fc difference (older naming)
        {"ANOM",          "PHANOM",         KIND_ANOMALOUS},// Phenix anomalous difference
        {"DELFAN",        "PHDELAN",        KIND_ANOMALOUS},// Refmac anomalous difference
    };
//}}}

//{{{ Variable definitions
//##############################################################################
    byte[]              fileBytes;                  // entire MTZ file buffered in memory
    float[]             data            = null;     // holds the density grid after FFT
    boolean             littleEndian    = false;

    // MTZ header info
    int                 ncol;                       // number of columns per reflection
    int                 nrefl;                      // number of reflections
    int                 headerOffset;               // byte offset to start of text header

    // Column indices for the data we need
    int                 hCol = -1, kCol = -1, lCol = -1;
    int                 fCol = -1, phiCol = -1;
    String              fColName, phiColName;       // names of selected columns
    String              mapKind;                    // KIND_xxx for the selected columns

    // Requested columns, or null to take the first recognized pair in the file
    String              wantF = null, wantPhi = null;

    // All recognized {F, PHI, kind} triples present in this file, in priority order
    List<String[]>      availablePairs = new ArrayList<String[]>();

    // Resolution limit
    double              dmin = -1;                  // high-resolution limit in Angstroms

    // All column info (for error messages)
    String[]            colLabels;
    String[]            colTypes;

    // Space group symmetry operations (parsed from SYMM records)
    int                 nsym;                       // number of symmetry operations
    int[][]             symRotation;                // nsym x 9 (flattened 3x3 rotation matrices)
    double[][]          symTranslation;             // nsym x 3 (translation vectors)
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Creates a new VertexSource from an MTZ file containing map coefficients.
    * All data will be read and the density map will be computed via FFT.
    * @param in the source of MTZ data
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt or
    *         no recognized map coefficient columns are found
    */
    public MtzVertexSource(InputStream in) throws IOException
    {
        this(in, true);
    }

    /**
    * Creates a new VertexSource from an MTZ file containing map coefficients.
    * @param in the source of MTZ data
    * @param readData if false, only header info will be read (no FFT computed)
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt or
    *         no recognized map coefficient columns are found
    */
    public MtzVertexSource(InputStream in, boolean readData) throws IOException
    {
        this(in, readData, null, null);
    }

    /**
    * Creates a new VertexSource from an MTZ file containing map coefficients,
    * using a specific pair of map coefficient columns.
    * @param in the source of MTZ data
    * @param readData if false, only header info will be read (no FFT computed)
    * @param fLabel the label of the amplitude column to use, or null to
    *        take the first recognized pair present in the file
    * @param phiLabel the label of the phase column to use, or null
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt or
    *         the requested map coefficient columns are not found
    */
    public MtzVertexSource(InputStream in, boolean readData, String fLabel, String phiLabel) throws IOException
    {
        this.wantF = fLabel;
        this.wantPhi = phiLabel;

        // Auto-detect a gzipped input stream
        in = new BufferedInputStream(in);
        in.mark(10);
        if(in.read() == 31 && in.read() == 139)
        {
            // We've found the gzip magic numbers...
            in.reset();
            in = new java.util.zip.GZIPInputStream(in);
        }
        else in.reset();

        // Buffer entire file into memory (MTZ header is at end of file,
        // so we need random access)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        in.close();
        fileBytes = baos.toByteArray();

        super.init(readData);
    }

    /**
    * Creates a new VertexSource that reuses an already-buffered MTZ file.
    * The byte array is shared by reference, not copied.
    */
    private MtzVertexSource(byte[] fileBytes, String fLabel, String phiLabel) throws IOException
    {
        this.fileBytes = fileBytes;
        this.wantF = fLabel;
        this.wantPhi = phiLabel;

        super.init(true);
    }
//}}}

//{{{ forCoefficients, getAvailableCoefficients, getFColumnName, getPhiColumnName, getMapKind
//##############################################################################
    /**
    * Computes a second density map from this same MTZ file using a different
    * pair of map coefficient columns, e.g. the difference map alongside the
    * 2Fo-Fc map. The file is not re-read from disk; only the FFT is redone.
    * @param fLabel the label of the amplitude column to use
    * @param phiLabel the label of the phase column to use
    * @throws IllegalStateException if this source's file buffer has already been released
    * @throws IOException if the buffered data is malformed
    */
    public MtzVertexSource forCoefficients(String fLabel, String phiLabel) throws IOException
    {
        if(fileBytes == null)
            throw new IllegalStateException("MTZ file buffer has already been released");
        return new MtzVertexSource(fileBytes, fLabel, phiLabel);
    }

    /**
    * Returns the recognized {F, PHI, kind} column triples present in this file,
    * in priority order, where kind is one of the KIND_xxx constants.
    * Never empty -- the constructor throws if nothing is recognized.
    */
    public List<String[]> getAvailableCoefficients()
    { return Collections.unmodifiableList(availablePairs); }

    /** Returns the label of the amplitude column this map was computed from. */
    public String getFColumnName()
    { return fColName; }

    /** Returns the label of the phase column this map was computed from. */
    public String getPhiColumnName()
    { return phiColName; }

    /** Returns the KIND_xxx constant for the columns this map was computed from. */
    public String getMapKind()
    { return mapKind; }
//}}}

//{{{ readHeader
//##################################################################################################
    /**
    * Decodes information from the MTZ file header.
    * The MTZ header is located at the end of the file; its position
    * is indicated by bytes 4-7 of the file.
    * @throws IOException if there's an I/O error or premature end of file
    * @throws IllegalArgumentException if the file format is corrupt
    */
    void readHeader() throws IOException
    {
        if(fileBytes.length < 20)
            throw new IllegalArgumentException("File too small to be a valid MTZ file");

        // Validate magic bytes
        if(fileBytes[0] != 'M' || fileBytes[1] != 'T'
        || fileBytes[2] != 'Z' || fileBytes[3] != ' ')
            throw new IllegalArgumentException("Bad MTZ format: 'MTZ ' missing at beginning of file");

        // Detect endianness from machine stamp at bytes 8-11
        // Byte 8: 0x44 = little-endian (IEEE), 0x11 = big-endian (IEEE)
        int stamp = fileBytes[8] & 0xFF;
        if(stamp == 0x44)
            littleEndian = true;
        else if(stamp == 0x11)
            littleEndian = false;
        else
        {
            // Unknown stamp; try little-endian as default (most common on modern hardware)
            SoftLog.err.println("MTZ: Unknown machine stamp 0x" + Integer.toHexString(stamp)
                + ", assuming little-endian");
            littleEndian = true;
        }

        // Read header location (word offset from start of file, 1-based)
        int headerWords = getInt(4);
        headerOffset = (headerWords - 1) * 4;

        if(headerOffset < 20 || headerOffset >= fileBytes.length)
            throw new IllegalArgumentException("Bad MTZ format: invalid header offset " + headerWords);

        // Parse text header records (80 chars each)
        ArrayList<String> colLabelList = new ArrayList<String>();
        ArrayList<String> colTypeList = new ArrayList<String>();
        ArrayList<String> symmOps = new ArrayList<String>();
        int hColCount = 0;

        int pos = headerOffset;
        while(pos + 80 <= fileBytes.length)
        {
            String record = new String(fileBytes, pos, 80).trim();
            pos += 80;

            if(record.startsWith("END"))
                break;

            if(record.startsWith("NCOL"))
            {
                String[] parts = record.split("\\s+");
                if(parts.length >= 4)
                {
                    ncol = Integer.parseInt(parts[1]);
                    nrefl = Integer.parseInt(parts[2]);
                }
            }
            else if(record.startsWith("CELL"))
            {
                String[] parts = record.split("\\s+");
                if(parts.length >= 7)
                {
                    aLength = Double.parseDouble(parts[1]);
                    bLength = Double.parseDouble(parts[2]);
                    cLength = Double.parseDouble(parts[3]);
                    alpha   = Double.parseDouble(parts[4]);
                    beta    = Double.parseDouble(parts[5]);
                    gamma   = Double.parseDouble(parts[6]);
                }
            }
            else if(record.startsWith("RESO"))
            {
                // RESO <min_1/d^2> <max_1/d^2>
                String[] parts = record.split("\\s+");
                if(parts.length >= 3)
                {
                    double maxReso = Double.parseDouble(parts[2]);
                    if(maxReso > 0)
                        dmin = 1.0 / Math.sqrt(maxReso);
                }
            }
            else if(record.startsWith("COLUMN"))
            {
                // COLUMN <label> <type> <min> <max> <dataset_id>
                String[] parts = record.split("\\s+");
                if(parts.length >= 3)
                {
                    String label = parts[1];
                    String type = parts[2];
                    colLabelList.add(label);
                    colTypeList.add(type);

                    // Track H/K/L columns (type 'H')
                    if("H".equals(type))
                    {
                        if(hColCount == 0) hCol = colLabelList.size() - 1;
                        else if(hColCount == 1) kCol = colLabelList.size() - 1;
                        else if(hColCount == 2) lCol = colLabelList.size() - 1;
                        hColCount++;
                    }
                }
            }
            else if(record.startsWith("SYMM"))
            {
                String op = record.substring(4).trim();
                if(op.length() > 0) symmOps.add(op);
            }
        }

        colLabels = colLabelList.toArray(new String[0]);
        colTypes = colTypeList.toArray(new String[0]);

        // Parse symmetry operations
        if(symmOps.isEmpty())
            symmOps.add("X, Y, Z"); // default: identity only
        nsym = symmOps.size();
        symRotation = new int[nsym][9];
        symTranslation = new double[nsym][3];
        for(int i = 0; i < nsym; i++)
            parseSymmetryOp(symmOps.get(i), symRotation[i], symTranslation[i]);
        SoftLog.err.println("MTZ: " + nsym + " symmetry operations");

        // Validate H/K/L columns found
        if(hCol < 0 || kCol < 0 || lCol < 0)
            throw new IllegalArgumentException("MTZ file does not contain H, K, L index columns");

        // Find every recognized F/PHI column pair present in this file.
        // A typical Refmac or Phenix file holds both a 2Fo-Fc and a difference set.
        Map<String, Integer> labelIndex = new HashMap<String, Integer>();
        for(int i = 0; i < colLabels.length; i++)
            labelIndex.put(colLabels[i], i);

        availablePairs.clear();
        for(String[] pair : COLUMN_PAIRS)
        {
            if(labelIndex.containsKey(pair[0]) && labelIndex.containsKey(pair[1]))
                availablePairs.add(pair);
        }

        if(availablePairs.isEmpty())
        {
            StringBuilder sb = new StringBuilder();
            sb.append("No recognized map coefficient columns found in MTZ file.\n");
            sb.append("Available columns:\n");
            for(int i = 0; i < colLabels.length; i++)
                sb.append("  ").append(colLabels[i]).append(" (type ").append(colTypes[i]).append(")\n");
            sb.append("Expected one of: ");
            for(int i = 0; i < COLUMN_PAIRS.length; i++)
            {
                if(i > 0) sb.append(", ");
                sb.append(COLUMN_PAIRS[i][0]).append("/").append(COLUMN_PAIRS[i][1]);
            }
            throw new IllegalArgumentException(sb.toString());
        }

        // Select the requested pair, or the highest-priority one present
        String[] chosen = null;
        if(wantF != null && wantPhi != null)
        {
            for(String[] pair : availablePairs)
            {
                if(wantF.equals(pair[0]) && wantPhi.equals(pair[1])) { chosen = pair; break; }
            }
            if(chosen == null)
                throw new IllegalArgumentException("MTZ file does not contain the requested map"
                    +" coefficient columns "+wantF+"/"+wantPhi);
        }
        else chosen = availablePairs.get(0);

        fCol        = labelIndex.get(chosen[0]).intValue();
        phiCol      = labelIndex.get(chosen[1]).intValue();
        fColName    = chosen[0];
        phiColName  = chosen[1];
        mapKind     = chosen[2];

        SoftLog.err.println("MTZ: Using columns " + fColName + "/" + phiColName
            + " (" + mapKind + ") for map coefficients");
        SoftLog.err.println("MTZ: " + nrefl + " reflections, " + ncol + " columns");
        SoftLog.err.println("MTZ: Cell " + aLength + " " + bLength + " " + cLength
            + " " + alpha + " " + beta + " " + gamma);
    }
//}}}

//{{{ readData
//##################################################################################################
    /**
    * Reads reflection data from the MTZ file, then computes the electron
    * density map via inverse 3D FFT.
    * @throws IOException if there's an I/O error
    * @throws IllegalArgumentException if the file data is corrupt
    */
    void readData() throws IOException
    {
        // Read all reflections and find max HKL indices
        int[] hArr = new int[nrefl];
        int[] kArr = new int[nrefl];
        int[] lArr = new int[nrefl];
        float[] fArr = new float[nrefl];
        float[] phiArr = new float[nrefl];
        int maxH = 0, maxK = 0, maxL = 0;
        int validCount = 0;

        // Reflection data starts at byte 80 (after the 20-byte file header,
        // padded to the first 80-byte record boundary)
        int dataStart = 80;
        int recordSize = ncol * 4; // bytes per reflection record

        for(int r = 0; r < nrefl; r++)
        {
            int offset = dataStart + r * recordSize;
            if(offset + recordSize > headerOffset)
                break; // don't read past the header

            int h = Math.round(getFloat(offset + hCol * 4));
            int k = Math.round(getFloat(offset + kCol * 4));
            int l = Math.round(getFloat(offset + lCol * 4));
            float f = getFloat(offset + fCol * 4);
            float phi = getFloat(offset + phiCol * 4);

            // Skip missing reflections (MTZ missing number flag)
            if(Float.isNaN(f) || Float.isNaN(phi)) continue;
            if(f <= 0.0f) continue; // skip zero/negative amplitudes

            hArr[validCount] = h;
            kArr[validCount] = k;
            lArr[validCount] = l;
            fArr[validCount] = f;
            phiArr[validCount] = phi;
            validCount++;

            maxH = Math.max(maxH, Math.abs(h));
            maxK = Math.max(maxK, Math.abs(k));
            maxL = Math.max(maxL, Math.abs(l));
        }

        SoftLog.err.println("MTZ: " + validCount + " valid reflections read");
        SoftLog.err.println("MTZ: Max indices H=" + maxH + " K=" + maxK + " L=" + maxL);

        // Determine FFT grid dimensions.
        // Must be at least 2*max+1 (Nyquist criterion).
        // Also aim for ~d_min/3 real-space sampling for uniform resolution,
        // using cell parameters and resolution from the RESO header record.
        int minNx = 2 * maxH + 1;
        int minNy = 2 * maxK + 1;
        int minNz = 2 * maxL + 1;
        if(dmin > 0)
        {
            double sampleRate = 3.0; // standard oversampling: grid spacing = d_min/3
            minNx = Math.max(minNx, (int)Math.ceil(aLength * sampleRate / dmin));
            minNy = Math.max(minNy, (int)Math.ceil(bLength * sampleRate / dmin));
            minNz = Math.max(minNz, (int)Math.ceil(cLength * sampleRate / dmin));
        }
        int nx = goodFFTSize(minNx);
        int ny = goodFFTSize(minNy);
        int nz = goodFFTSize(minNz);

        SoftLog.err.println("MTZ: FFT grid dimensions " + nx + " x " + ny + " x " + nz);

        // Populate the reciprocal-space complex grid
        // Layout: interleaved real/imaginary, row-major [nx][ny][nz]
        int rowspan = 2 * nz;
        int slicespan = ny * rowspan;
        double[] grid = new double[nx * slicespan];

        for(int r = 0; r < validCount; r++)
        {
            int h = hArr[r];
            int k = kArr[r];
            int l = lArr[r];
            double f = fArr[r];
            double phiDeg = phiArr[r];

            // Apply each space group symmetry operation to expand from
            // asymmetric unit to full reciprocal sphere
            for(int s = 0; s < nsym; s++)
            {
                int[] rot = symRotation[s];
                double[] trans = symTranslation[s];

                // Reciprocal-space transformation: h' = (h,k,l) * W
                int hp = h * rot[0] + k * rot[3] + l * rot[6];
                int kp = h * rot[1] + k * rot[4] + l * rot[7];
                int lp = h * rot[2] + k * rot[5] + l * rot[8];

                // Phase shift: phi' = phi - 360 * h'.t
                double phiP = phiDeg - 360.0 * (hp * trans[0] + kp * trans[1] + lp * trans[2]);
                double phiPRad = phiP * Math.PI / 180.0;

                double re = f * Math.cos(phiPRad);
                double im = f * Math.sin(phiPRad);

                // Place F(h',k',l') on the grid
                int hi = ((hp % nx) + nx) % nx;
                int ki = ((kp % ny) + ny) % ny;
                int li = ((lp % nz) + nz) % nz;
                int idx = hi * slicespan + ki * rowspan + 2 * li;
                grid[idx]     = re;
                grid[idx + 1] = im;

                // Place Friedel mate F(-h',-k',-l') = conjugate
                if(hp != 0 || kp != 0 || lp != 0)
                {
                    int hf = ((-hp % nx) + nx) % nx;
                    int kf = ((-kp % ny) + ny) % ny;
                    int lf = ((-lp % nz) + nz) % nz;
                    int fidx = hf * slicespan + kf * rowspan + 2 * lf;
                    grid[fidx]     = re;
                    grid[fidx + 1] = -im;
                }
            }
        }

        // Forward 3D FFT to get real-space density.
        // The crystallographic convention rho = (1/V) * sum F(h)*exp(-2*pi*i*h.r)
        // matches the forward FFT sign convention exp(-2*pi*i*k*n/N).
        // The FFT computes the sum without the 1/V factor, so we normalize afterward.
        ComplexDouble3DFFT fft = new ComplexDouble3DFFT(nx, ny, nz);
        fft.transform(grid, rowspan, slicespan);

        // Compute unit cell volume for normalization:
        // V = abc * sqrt(1 - cos^2(alpha) - cos^2(beta) - cos^2(gamma) + 2*cos(alpha)*cos(beta)*cos(gamma))
        double cosA = Math.cos(Math.toRadians(alpha));
        double cosB = Math.cos(Math.toRadians(beta));
        double cosG = Math.cos(Math.toRadians(gamma));
        double volume = aLength * bLength * cLength
            * Math.sqrt(1.0 - cosA*cosA - cosB*cosB - cosG*cosG + 2.0*cosA*cosB*cosG);
        SoftLog.err.println("MTZ: Unit cell volume = " + volume + " A^3");

        // Extract real parts into the density data array, normalizing by 1/V
        data = new float[nx * ny * nz];
        double sum = 0, sumSq = 0;
        double invV = 1.0 / volume;
        for(int i = 0; i < nx; i++)
        {
            for(int j = 0; j < ny; j++)
            {
                for(int k = 0; k < nz; k++)
                {
                    float val = (float)(grid[i * slicespan + j * rowspan + 2 * k] * invV);
                    data[i + j * nx + k * nx * ny] = val;
                    sum += val;
                    sumSq += (double) val * val;
                }
            }
        }

        // Free the complex grid
        grid = null;

        // Set CrystalVertexSource grid parameters
        // The FFT covers the full unit cell
        aSteps = nx;    bSteps = ny;    cSteps = nz;
        aMin   = 0;     bMin   = 0;     cMin   = 0;
        aMax   = nx-1;  bMax   = ny-1;  cMax   = nz-1;
        aCount = nx;    bCount = ny;    cCount = nz;

        // Compute statistics from the density
        int total = nx * ny * nz;
        mean = sum / total;
        sigma = Math.sqrt(sumSq / total - mean * mean);

        SoftLog.err.println("MTZ computed: mean = " + mean + "; sigma = " + sigma);

        // Nothing reads the raw file after the FFT, and holding it would pin a full
        // copy of the MTZ for the life of the map window. Other instances sharing
        // this array (see forCoefficients) keep their own references.
        fileBytes = null;
    }
//}}}

//{{{ getInt, getFloat
//##############################################################################
    /**
    * Returns an int from fileBytes at the given byte offset,
    * respecting the littleEndian flag.
    */
    protected int getInt(int byteOffset)
    {
        if(littleEndian)
        {
            return ((fileBytes[byteOffset+3] & 0xFF) << 24) |
                   ((fileBytes[byteOffset+2] & 0xFF) << 16) |
                   ((fileBytes[byteOffset+1] & 0xFF) <<  8) |
                   ((fileBytes[byteOffset]   & 0xFF));
        }
        else
        {
            return ((fileBytes[byteOffset]   & 0xFF) << 24) |
                   ((fileBytes[byteOffset+1] & 0xFF) << 16) |
                   ((fileBytes[byteOffset+2] & 0xFF) <<  8) |
                   ((fileBytes[byteOffset+3] & 0xFF));
        }
    }

    /**
    * Returns a float from fileBytes at the given byte offset,
    * respecting the littleEndian flag.
    */
    protected float getFloat(int byteOffset)
    {
        return Float.intBitsToFloat(getInt(byteOffset));
    }
//}}}

//{{{ goodFFTSize
//##############################################################################
    /**
    * Returns the smallest integer >= minSize that factors entirely
    * into the primes {2, 3, 5, 7}, which are efficiently handled
    * by the mixed-radix FFT algorithm.
    */
    static int goodFFTSize(int minSize)
    {
        if(minSize <= 1) return 1;
        int n = minSize;
        while(true)
        {
            int m = n;
            while(m % 2 == 0) m /= 2;
            while(m % 3 == 0) m /= 3;
            while(m % 5 == 0) m /= 5;
            while(m % 7 == 0) m /= 7;
            if(m == 1) return n;
            n++;
        }
    }
//}}}

//{{{ parseSymmetryOp
//##############################################################################
    /**
    * Parses a symmetry operation string like "X, Y, Z" or "-Y, X-Y, Z+2/3"
    * into a rotation matrix (flattened 3x3 integers) and translation vector
    * (3 doubles in fractional coordinates).
    * @param op the symmetry operation string from the SYMM header record
    * @param rotation a 9-element array to fill: rotation[row*3+col] = W[row][col]
    * @param translation a 3-element array to fill with the translation vector
    */
    static void parseSymmetryOp(String op, int[] rotation, double[] translation)
    {
        String[] terms = op.split(",");
        if(terms.length != 3)
            throw new IllegalArgumentException("Invalid symmetry operation: " + op);

        for(int row = 0; row < 3; row++)
        {
            String term = terms[row].trim().toUpperCase();
            rotation[row * 3]     = 0; // coefficient of X
            rotation[row * 3 + 1] = 0; // coefficient of Y
            rotation[row * 3 + 2] = 0; // coefficient of Z
            translation[row]      = 0.0;

            int sign = 1;
            int i = 0;
            while(i < term.length())
            {
                char c = term.charAt(i);
                if(c == '+')      { sign = 1;  i++; }
                else if(c == '-') { sign = -1; i++; }
                else if(c == 'X') { rotation[row * 3]     += sign; sign = 1; i++; }
                else if(c == 'Y') { rotation[row * 3 + 1] += sign; sign = 1; i++; }
                else if(c == 'Z') { rotation[row * 3 + 2] += sign; sign = 1; i++; }
                else if(c == ' ') { i++; }
                else if(Character.isDigit(c))
                {
                    int num = c - '0';
                    if(i + 2 < term.length() && term.charAt(i + 1) == '/')
                    {
                        // Fraction like 1/2, 2/3, etc.
                        int den = term.charAt(i + 2) - '0';
                        translation[row] += sign * (double)num / den;
                        sign = 1;
                        i += 3;
                    }
                    else if(i + 1 < term.length() && Character.isLetter(term.charAt(i + 1)))
                    {
                        // Integer coefficient before a variable, e.g. "2X"
                        char next = Character.toUpperCase(term.charAt(i + 1));
                        if(next == 'X')      rotation[row * 3]     += sign * num;
                        else if(next == 'Y') rotation[row * 3 + 1] += sign * num;
                        else if(next == 'Z') rotation[row * 3 + 2] += sign * num;
                        sign = 1;
                        i += 2;
                    }
                    else i++;
                }
                else i++;
            }
        }
    }
//}}}

//{{{ hasData
//##################################################################################################
    /**
    * Returns true iff density data was computed at the time of creation.
    */
    public boolean hasData()
    {
        return (data != null);
    }
//}}}

//{{{ get/setValue
//##################################################################################################
    /**
    * Returns the value at the specified grid point,
    * where the indexes i, j, and k have been adjusted
    * to start from 0 (i.e. i==0 means aMin, j==1 means bMin+1, etc.)
    */
    public double getValue(int i, int j, int k)
    {
        return (double)data[ i + j*aCount + k*aCount*bCount ];
    }

    /**
    * Sets the value at the specified grid point,
    * where the indexes i, j, and k have been adjusted
    * to start from 0 (i.e. i==0 means aMin, j==1 means bMin+1, etc.)
    */
    protected void setValue(int i, int j, int k, double d)
    {
        data[ i + j*aCount + k*aCount*bCount ] = (float)d;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class
