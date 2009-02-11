// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package cmdline;

import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import javax.swing.*;
import javax.imageio.*;
import java.net.*;
import java.util.*;
import java.text.DecimalFormat;
//}}}
/**
* <code>KinImage</code> makes kinemages from images ("kin <- image").
* Note that, cleverly, "KinImage" sounds like "kinemage."  (wild applause)
* Thank you; you're too kind...
*/
public class KinImage //extends ... implements ...
{
//{{{ Constants
//##############################################################################
    DecimalFormat df = new DecimalFormat("###");
//}}}

//{{{ Variable definitions
//##############################################################################
    String                   filename   = null;
    String                   urlname    = null;
    boolean                  verbose    = false;
    String                   imageName  = null;
    JFrame                   frame      = null;
    int                      imageResol = 2;  // take 1 of every n pixels from original image
    HashMap<String, float[]> colors     = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KinImage()
    {
        // Used HSB from KPalette for black instead of white background
        colors = new HashMap<String, float[]>();
        colors.put( "red",       new float[] {0,      100,    100} );
        colors.put( "orange",    new float[] {20,     100,    100} );
        colors.put( "gold",      new float[] {40,     100,    100} );
        colors.put( "yellow",    new float[] {60,     100,    100} );
        colors.put( "lime",      new float[] {80,     100,    100} );
        colors.put( "green",     new float[] {120,    80,     100} );
        colors.put( "sea",       new float[] {150,    100,    100} );
        colors.put( "cyan",      new float[] {180,    100,    85 } );
        colors.put( "sky",       new float[] {210,    75,     95 } );
        colors.put( "blue",      new float[] {240,    70,     100} );
        colors.put( "purple",    new float[] {275,    75,     100} );
        colors.put( "magenta",   new float[] {300,    95,     100} );
        colors.put( "hotpink",   new float[] {335,    100,    100} );
        colors.put( "pink",      new float[] {350,    55,     100} );
        colors.put( "peach",     new float[] {25,     75,     100} );
        colors.put( "lilac",     new float[] {275,    55,     100} );
        colors.put( "pinktint",  new float[] {340,    30,     100} );
        colors.put( "peachtint", new float[] {25,     50,     100} );
        colors.put( "yellowtint",new float[] {60,     50,     100} );
        colors.put( "greentint", new float[] {135,    40,     100} );
        colors.put( "bluetint",  new float[] {220,    40,     100} );
        colors.put( "lilactint", new float[] {275,    35,     100} );
        colors.put( "white",     new float[] {0,      0,      100} );
        colors.put( "gray",      new float[] {0,      0,      50 } );
        colors.put( "brown",     new float[] {20,     45,     75 } );
        colors.put( "deadwhite", new float[] {360,    100,    100} );
        colors.put( "deadblack", new float[] {0,      0,      0  } );
    }
//}}}

//{{{ loadImage
//##############################################################################
    public BufferedImage loadImage()
    {
        BufferedImage image = null;
        try 
        {
            if (filename != null)
            {
                // Read from a file
                File sourceimage = new File(filename);
                image = ImageIO.read(sourceimage);
                imageName = filename.toString().substring(filename.toString().lastIndexOf("/")+1);
            }
            else
            {
                // Read from a URL
                URL url = null;
                if (urlname != null)  url = new URL(urlname);
                else
                {
                    System.err.println("Loading default image from web: "+
                        "http://kinemage.biochem.duke.edu/images/DaveTheMage.jpg");
                    url = new URL("http://kinemage.biochem.duke.edu/images/DaveTheMage.jpg");
                }
                image = ImageIO.read(url);
                imageName = url.toString().substring(url.toString().lastIndexOf("/")+1);
            }
            
        } 
        catch (IOException e)
        {
            System.err.println("problem finding image!");
        }
        return image;
    }
//}}}

//{{{ showImage
//##############################################################################
    /** Uses a Java window to display the starting image */
    public void showImage(Image image)
    {
        frame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(image));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setTitle("\"kin <- image\" from "+imageName+" ... Running");
    }
//}}}

//{{{ doKin
//##############################################################################
    /** Prints out sparse kinemage dotlist starting from pixel array */
    public void doKin(BufferedImage image)
    {
        int w = image.getWidth();
        int h = image.getHeight();
        
        int[] pixels = new int[w * h];
        
        System.out.println("@flat");
        System.out.println("@group {"+imageName+"} dominant");
        System.out.println("@dotlist {pixels}");
        
        for (int i = 0; i < pixels.length; i += imageResol)
        {
            int x      = i % w;
            int y      = h - i / w;
            int y_flip =     i / w;  // Java images start with row 0 at top!
            pixels[i] = image.getRGB(x, y_flip);
            
            if (x % imageResol == 0 && y % imageResol == 0)
            {
                Color col = new Color(pixels[i]);
                int[] rgb = new int[] {col.getRed(), col.getBlue(), col.getGreen()};
                float[] hsb0to1 = Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], new float[3]);
                float[] hsb = new float[] {hsb0to1[0]*360f, hsb0to1[1]*100f, hsb0to1[2]*100f}; // 0-360, 0-100, 0-100
                String kcolor = getKinColor(hsb);
                
                float[] khsb = null;
                for (Iterator iter = colors.keySet().iterator(); iter.hasNext(); )
                {
                    String c = (String)iter.next();
                    if (c.equals(kcolor))  khsb = colors.get(c);
                }
                
                if (!kcolor.equals("deadblack")) System.out.println("{pixel "+x+","+y+" hsb "+df.format(hsb[0])+","
                    +df.format(hsb[1])+","+df.format(hsb[2])+" "+kcolor+"}"+kcolor+" "+x+" "+y);
            }
        }
        
        frame.setTitle("\"kin <- image\" from "+imageName+" ... Done!");
    }
//}}}

//{{{ getKinColor
//##############################################################################
    /** Tries to match up HSB triplet (h 0-360, s 0-100, v 0-100) to kinemage 
    * color, based on "cylindrical distance" in HSB space.  Assumes HSB is 
    * close enough to HSV as to be indistinguishable */
    public String getKinColor(float[] hsb)
    {
        double minDist = 361;
        String bestColor = null;
        
        for (Iterator iter = colors.keySet().iterator(); iter.hasNext(); )
        {
            String color = (String)iter.next();
            float[] ref = colors.get(color);
            
            float hDist1 = Math.abs( hsb[0]     - ref[0] ); // 359     - 1 =>        => 359
            float hDist2 = Math.abs( hsb[0]-360 - ref[0] ); // 359-360 - 1 => -1 - 1 =>   2
            float hDist = (hDist1 < hDist2 ? hDist1 : hDist2); // account for 0 <=> 360 wrap
            float sDist = Math.abs( hsb[1] - ref[1] );
            float bDist = Math.abs( hsb[2] - ref[2] );
            double dist = Math.sqrt( Math.pow(hDist,2) + Math.pow(sDist,2) + Math.pow(bDist,2) );
            
            if (dist < minDist)
            {
                minDist = dist;
                bestColor = color;
            }
            // else keep current best color
        }
        
        if (verbose) System.err.println("kin color nearest to "+hsb[0]+" "+hsb[1]+" "+hsb[2]+" is "+bestColor);
        return bestColor;
    }
//}}}

/* OLD CODE
//{{{ getKinColor
//##############################################################################
    // Tries to match up HSB triplet (h 0-360, s 0-100, v 0-100) to kinemage 
    // color, based on three separate distances in HSB space.  Assumes HSB is 
    // close enough to HSV as to be indistinguishable
    public String getKinColor(float[] hsb)
    {
        float minHdist = 361;
        String bestColor = null;
        for (Iterator iter = colors.keySet().iterator(); iter.hasNext(); )
        {
            String color = (String)iter.next();
            float[] ref = colors.get(color);
            float hDist1 = Math.abs( hsb[0]     - ref[0] ); // 359     - 1 =>        => 359
            float hDist2 = Math.abs( hsb[0]-360 - ref[0] ); // 359-360 - 1 => -1 - 1 =>   2
            float hDist = (hDist1 < hDist2 ? hDist1 : hDist2); // account for 0 <=> 360 wrap
            if (hDist < minHdist)
            {
                minHdist = hDist;
                bestColor = color;
            }
            else if (hDist == minHdist) // tie on hue
            {
                bestColor = breakHueTie(hsb, minHdist, bestColor);
            }
            // else keep current best hue
        }
        
        if (verbose) System.err.println("kin color nearest to "+hsb[0]+" "+hsb[1]+" "+hsb[2]+" is "+bestColor);
        return bestColor;
    }
//}}}
//{{{ break[Hue/Sat]Tie
//##############################################################################
    public String breakHueTie(float[] hsb, float h, String bestColor)
    {
        // Get colors for which we'll break the hue tie
        HashMap<String, float[]> hColors = new HashMap<String, float[]>();
        for (Iterator iter = colors.keySet().iterator(); iter.hasNext(); )
        {
            String color = (String)iter.next();
            float[] ref = colors.get(color);
            if (ref[0] == h) hColors.put(color, ref);
        }
        
        float minSdist = 101;
        for (Iterator iter = hColors.keySet().iterator(); iter.hasNext(); )
        {
            String color = (String)iter.next();
            float[] ref = colors.get(color);
            float sDist = Math.abs( hsb[1] - ref[1] );
            if (sDist < minSdist)
            {
                minSdist = sDist;
                bestColor = color;
            }
            else if (sDist == minSdist)
            {
                bestColor = breakSatTie(hsb, h, minSdist, bestColor);
            }
            // else keep current best sat
        }
        
        return bestColor;
    }

    public String breakSatTie(float[] hsb, float h, float s, String bestColor)
    {
        // Get colors for which we'll break the hue tie
        HashMap<String, float[]> hsColors = new HashMap<String, float[]>();
        for (Iterator iter = colors.keySet().iterator(); iter.hasNext(); )
        {
            String color = (String)iter.next();
            float[] ref = colors.get(color);
            if (ref[0] == h && ref[1] == s) hsColors.put(color, ref);
        }
        
        float minBdist = 101;
        for (Iterator iter = hsColors.keySet().iterator(); iter.hasNext(); )
        {
            String color = (String)iter.next();
            float[] ref = colors.get(color);
            float bDist = Math.abs( hsb[2] - ref[2] );
            if (bDist < minBdist)
            {
                minBdist = bDist;
                bestColor = color;
            }
            else if (bDist == minBdist)
            {
                if (verbose) System.err.println("Can't tell if "+hsb[0]+" "+hsb[1]+" "+hsb[2]
                    +" is closer to "+bestColor+" or "+color);
            }
            // else keep current best bri
        }
        
        return bestColor;
    }
//}}}
*/

//{{{ Main, main
//##############################################################################
    /**
    * Main() function for running as an application
    */
    public void Main() throws IOException
    {
        BufferedImage image = loadImage();
        if (image != null)
        {
            showImage(image);
            doKin(image);
        }
        else System.err.println("couldn't find image!");
    }

    public static void main(String[] args)
    {
        KinImage mainprog = new KinImage();
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
        catch(IOException ex) { ex.printStackTrace(); }
    }
//}}}

//{{{ parseArguments
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
//}}}

//{{{ showHelp, streamcopy
//##############################################################################
    // Displays help information
    void showHelp(boolean showAll)
    {
        if(showAll)
        {
            InputStream is = getClass().getResourceAsStream("KinImage.help");
            if(is == null)
                System.err.println("\n*** Unable to locate help information in 'KinImage.help' ***\n");
            else
            {
                try { streamcopy(is, System.out); }
                catch(IOException ex) { ex.printStackTrace(); }
            }
        }
        System.err.println("KinImage");
        System.err.println("Copyright (C) 2009 by Daniel A. Keedy. All rights reserved.");
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
        filename = arg;
    }
    
    void interpretFlag(String flag, String param)
    {
        if(flag.equals("-help") || flag.equals("-h"))
        {
            showHelp(true);
            System.exit(0);
        }
        else if(flag.equals("-verbose") || flag.equals("-v"))
        {
            verbose = true;
        }
        else if(flag.equals("-url"))
        {
            urlname = param;
        }
        else if(flag.equals("-resolution") || flag.equals("resol") || flag.equals("r"))
        {
            try { imageResol = Integer.parseInt(param); }
            catch (NumberFormatException nfe)
            { System.err.println("Can't format "+param+" as integer for image resolution!"); }
        }
        else if(flag.equals("-dummy_option"))
        {
            // handle option here
        }
        else throw new IllegalArgumentException("'"+flag+"' is not recognized as a valid flag");
    }
//}}}
}//class
