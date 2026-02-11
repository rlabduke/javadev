package jnt.FFT;
/** Computes the FFT of 3 dimensional complex, double precision data.
  * The data is stored in a 1-dimensional array in Row-Major order.
  * The physical layout in the array data, of the mathematical data d[i,j,k] is as follows:
  *<PRE>
  *    Re(d[i,j,k]) = data[i*slicespan + j*rowspan + 2*k]
  *    Im(d[i,j,k]) = data[i*slicespan + j*rowspan + 2*k + 1]
  *</PRE>
  * where <code>rowspan</code> must be at least 2*nz (it defaults to 2*nz),
  * and <code>slicespan</code> must be at least ny*rowspan (it defaults to ny*rowspan).
  * The transformed data is returned in the original data array in
  * wrap-around order along each dimension.
  *
  * @author Bruce R. Miller bruce.miller@nist.gov (original 2D version)
  * @author Extended to 3D for crystallographic FFT support
  */
public class ComplexDouble3DFFT {
  int nx, ny, nz;
  ComplexDoubleFFT xFFT, yFFT, zFFT;

  /** Create an FFT for transforming nx*ny*nz points of Complex, double precision
    * data. */
  public ComplexDouble3DFFT(int nx, int ny, int nz) {
    if ((nx <= 0) || (ny <= 0) || (nz <= 0))
      throw new IllegalArgumentException("The array dimensions must be >0 : "+nx+","+ny+","+nz);
    this.nx = nx;
    this.ny = ny;
    this.nz = nz;
    zFFT = new ComplexDoubleFFT_Mixed(nz);
    yFFT = (ny == nz ? zFFT : new ComplexDoubleFFT_Mixed(ny));
    xFFT = (nx == nz ? zFFT : (nx == ny ? yFFT : new ComplexDoubleFFT_Mixed(nx)));
  }

  protected void checkData(double data[], int rowspan, int slicespan){
    if (rowspan < 2*nz)
      throw new IllegalArgumentException("The row span "+rowspan+
					 " is shorter than the row length "+2*nz);
    if (slicespan < ny*rowspan)
      throw new IllegalArgumentException("The slice span "+slicespan+
					 " is shorter than the slice length "+ny*rowspan);
    if (nx*slicespan > data.length)
      throw new IllegalArgumentException("The data array is too small for "+
					 nx+"x"+ny+"x"+nz+" data.length="+data.length);
  }

  /** Compute the Fast Fourier Transform of data leaving the result in data.
    * The array data must be dimensioned (at least) 2*nx*ny*nz, consisting of
    * alternating real and imaginary parts. */
  public void transform(double data[]) {
    transform(data, 2*nz, ny*2*nz); }

  /** Compute the Fast Fourier Transform of data leaving the result in data. */
  public void transform(double data[], int rowspan, int slicespan) {
    checkData(data, rowspan, slicespan);
    // Transform along z (innermost, stride=2, packed complex)
    for(int i=0; i<nx; i++)
      for(int j=0; j<ny; j++)
        zFFT.transform(data, i*slicespan + j*rowspan, 2);
    // Transform along y (stride=rowspan)
    for(int i=0; i<nx; i++)
      for(int k=0; k<nz; k++)
        yFFT.transform(data, i*slicespan + 2*k, rowspan);
    // Transform along x (stride=slicespan)
    for(int j=0; j<ny; j++)
      for(int k=0; k<nz; k++)
        xFFT.transform(data, j*rowspan + 2*k, slicespan);
  }

  /** Compute the (unnormalized) inverse FFT of data, leaving it in place.*/
  public void backtransform(double data[]) {
    backtransform(data, 2*nz, ny*2*nz); }

  /** Compute the (unnormalized) inverse FFT of data, leaving it in place.*/
  public void backtransform(double data[], int rowspan, int slicespan) {
    checkData(data, rowspan, slicespan);
    // Backtransform along x (stride=slicespan)
    for(int j=0; j<ny; j++)
      for(int k=0; k<nz; k++)
        xFFT.backtransform(data, j*rowspan + 2*k, slicespan);
    // Backtransform along y (stride=rowspan)
    for(int i=0; i<nx; i++)
      for(int k=0; k<nz; k++)
        yFFT.backtransform(data, i*slicespan + 2*k, rowspan);
    // Backtransform along z (innermost, stride=2)
    for(int i=0; i<nx; i++)
      for(int j=0; j<ny; j++)
        zFFT.backtransform(data, i*slicespan + j*rowspan, 2);
  }

  /** Return the normalization factor.
   * Multiply the elements of the backtransform'ed data to get the normalized inverse.*/
  public double normalization(){
    return 1.0/((double) nx*ny*nz); }

  /** Compute the (normalized) inverse FFT of data, leaving it in place.*/
  public void inverse(double data[]) {
    inverse(data, 2*nz, ny*2*nz); }

  /** Compute the (normalized) inverse FFT of data, leaving it in place.*/
  public void inverse(double data[], int rowspan, int slicespan) {
    backtransform(data, rowspan, slicespan);
    double norm = normalization();
    for(int i=0; i<nx; i++)
      for(int j=0; j<ny; j++)
        for(int k=0; k<nz; k++){
          data[i*slicespan + j*rowspan + 2*k]   *= norm;
          data[i*slicespan + j*rowspan + 2*k+1] *= norm; }
  }
}
