// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fftoys;
import jnt.FFT.*;

import java.awt.*;
import java.awt.image.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import javax.imageio.*;
//import driftwood.*;
//}}}
/**
* <code>ImageFFT</code> performs a 2-D Fourier transform on
* the green channel of a bitmapped image.
* It easily reads and writes PNG images on stdin and stdout.
*
* <p>A key note for using setRGB(): the alpha must equal 255 (0xff000000)
* for the image to be fully opaque!
*
* If the input image is real space, then the magnitude of the output
* corresponds to the structure factors F of the diffraction data.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Feb  7 22:39:21 EST 2004
*/
public class ImageFFT //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
//}}}

//{{{ Constructor(s)
//##############################################################################
    public ImageFFT()
    {
        super();
    }
//}}}

//{{{ imageToData
//##############################################################################
    /**
    * The X index of the image is data rows, the Y index is data columns,
    * which isn't quite intuitive. Tough luck.
    */
    double[] imageToData(BufferedImage img)
    {
        int xlen = img.getWidth();
        int ylen = img.getHeight();
        
        double[] data = new double[xlen * ylen * 2];
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index       = (i*ylen + j) * 2;
                double mag      = ((img.getRGB(i, j) >>>  8) & 0xFF) / 255.0;
                double phi      = ((img.getRGB(i, j) >>> 16) & 0xFF) / 256.0 * (2*Math.PI);
                data[index]     = mag * Math.cos(phi);
                data[index+1]   = mag * Math.sin(phi);
            }
        }
        
        return data;
    }
//}}}

//{{{ dataToImage
//##############################################################################
    /**
    * Unpacks data from a complex, double-precision FFT into a BufferedImage.
    * The minimum and maximum magnitudes of the complex values will correspond
    * to the minimum (0) and maximum (255) values in the green channel of the image.
    * The phase angle will be similarly encoded in the red channel (0 to 2pi).
    */
    BufferedImage dataToImage(double[] data, int xlen, int ylen)
    {
        BufferedImage img = new BufferedImage(xlen, ylen, BufferedImage.TYPE_INT_ARGB);
        
        // Convert complex data to magnitude and phase angle
        double[] mag = new double[xlen*ylen];
        double[] phi = new double[xlen*ylen];
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index       = (i*ylen + j);
                double re       = data[2*index];
                double im       = data[2*index+1];
                mag[index]      = Math.sqrt(re*re + im*im);
                phi[index]      = Math.atan2(im, re);
                if(phi[index] < 0.0)
                    phi[index] += 2*Math.PI;
            }
        }
        
        // Find min and max magnitude
        double max = Double.NEGATIVE_INFINITY;
        for(int idx = 0; idx < xlen*ylen; idx++)
                max = Math.max(max, mag[idx]);
        System.err.println("max="+max);
        // min should always be zero!

        // Write image
        for(int i = 0; i < xlen; i++)
        {
            for(int j = 0; j < ylen; j++)
            {
                int index   = (i*ylen + j);
                //Lots of error: int red     = (int)Math.floor(256.0 * phi[index] / (2*Math.PI));
                int red     = (int)Math.round(256.0 * phi[index] / (2*Math.PI));
                //Additional error: int green   = (int)Math.floor(256.0 * mag[index] / max);
                int green   = (int)Math.round(255.0 * mag[index] / max);
                if(red > 255)   red -= 256;
                if(green > 255) green = 255;
                img.setRGB(i, j, 0xff000000 + (red << 16) + (green << 8));
                //Magnitude only: img.setRGB(i, j, 0xff000000 + (green << 8));
            }
        }
        
        return img;
    }
//}}}

//{{{ transformImage
//##############################################################################
    /**
    * Fourier transforms an image and returns a new image of the same dimensions.
    */
    public BufferedImage transformImage(BufferedImage in)
    {
        double[] data = imageToData(in);
        ComplexDouble2DFFT fft = new ComplexDouble2DFFT(in.getWidth(), in.getHeight());
        fft.transform(data); // runs in place
        return dataToImage(data, in.getWidth(), in.getHeight());
    }
//}}}

//{{{ makeTestImage{1, 2}
//##############################################################################
    BufferedImage makeTestImage1()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < img.getWidth(); i++)
        {
            for(int j = 0; j < img.getHeight(); j++)
            {
                if(i%2 == 0 && j%2 == 0)
                    img.setRGB(i, j, 0xff00ff00);
                else
                    img.setRGB(i, j, 0xff000000);
            }
        }
        return img;
    }

    BufferedImage makeTestImage2()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < img.getWidth(); i++)
        {
            for(int j = 0; j < img.getHeight(); j++)
            {
                if(i%6 == 0 && j%11 == 0)
                    img.setRGB(i, j, 0xff00ff00);
                else
                    img.setRGB(i, j, 0xff000000);
            }
        }
        return img;
    }
//}}}

//{{{ makeTestImage{3, 4}
//##############################################################################
    BufferedImage makeTestImage3()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        for(int i = 0; i < img.getWidth(); i++)
        {
            for(int j = 0; j < img.getHeight(); j++)
            {
                if(i%32 == 0 && j%8 == 0)
                    img.setRGB(i, j, 0xff00ff00);
                else if(i%8 == 0 && j%4 == 0)
                    img.setRGB(i, j, 0xff007f00);
                else
                    img.setRGB(i, j, 0xff000000);
            }
        }
        return img;
    }
    
    BufferedImage makeTestImage4()
    {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, 400, 400);
        g.setColor(Color.green);
        g.fillOval( 50,  50, 100, 100);
        g.fillOval(250,  50, 100, 100);
        g.fillOval( 50, 250, 100, 100);
        g.fillOval(250, 250, 100, 100);
        g.dispose();
        return img;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main()
    {
        BufferedImage img = makeTestImage4();
        try { ImageIO.write(img, "png", new File("fftest-orig.png")); }
        catch(IOException ex) { ex.printStackTrace(); }
        img = transformImage(img);
        try { ImageIO.write(img, "png", new File("fftest-xform.png")); }
        catch(IOException ex) { ex.printStackTrace(); }
        img = transformImage(img);
        try { ImageIO.write(img, "png", new File("fftest-back.png")); }
        catch(IOException ex) { ex.printStackTrace(); }
    }

    public static void main(String[] args)
    {
        ImageFFT mainprog = new ImageFFT();
        try
        {
            mainprog.parseArguments(args);
            mainprog.Main();
        }
        catch(IllegalArgumentException ex)
        {
            ex.printStackTrace();
            System.err.println();
            mainprog.showHelp(true);
            System.err.println();
            System.err.println("*** Error parsing arguments: "+ex.getMessage());
            System.exit(1);
        }
    }
//}}}

//{{{ parseArguments, showHelp
//##############################################################################
    /**
    * Parse the command-line options for this program.
    * @param args the command-line options, as received by main()
    * @throws IllegalArgumentException if any argument is unrecognized, ambiguous, missing
    *   a required parameter, has a malformed parameter, or is otherwise unacceptable.
    */
    void parseArguments(String[] args)
    {
        String  arg, flag, param;
        boolean interpFlags = true;
        
        for(int i = 0; i < args.length; i++)
        {
            arg = args[i];
            if(!arg.startsWith("-") || !interpFlags || arg.equals("-"))
            {
                // This is probably a filename or something
                interpretArg(arg);
            }
            else if(arg.equals("--"))
            {
                // Stop treating things as flags once we find --
                interpFlags = false;
            }
            else
            {
                // This is a flag. It may have a param after the = sign
                int eq = arg.indexOf('=');
                if(eq != -1)
                {
                    flag    = arg.substring(0, eq);
                    param   = arg.substring(eq+1);
                }
                else
                {
                    flag    = arg;
                    param   = null;
                }
                
                try { interpretFlag(flag, param); }
                catch(NullPointerException ex)
                { throw new IllegalArgumentException("'"+arg
                    +"' expects to be followed by a parameter"); }
            }
        }//for(each arg in args)
    }
    
    // Display help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("ImageFFT.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'ImageFFT.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("fftoys.ImageFFT");
        System.err.println("Copyright (C) 2004 by Ian W. Davis. All rights reserved.");
    }

    // Copies src to dst until we hit EOF
    void streamcopy(InputStream src, OutputStream dst) throws IOException
    {
        byte[] buffer = new byte[2048];
        int len;
        while((len = src.read(buffer)) != -1) dst.write(buffer, 0, len);
    }
//}}}

//{{{ interpretArg, interpretFlag
//##############################################################################
    void interpretArg(String arg)
    {
        // Handle files, etc. here
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class

