// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.image;

import king.*;
import king.core.*;
import king.points.*;
import king.io.*;

import driftwood.gui.*;
import driftwood.r3.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import javax.swing.*;
import javax.imageio.*;
import java.net.*;
import java.util.*;
import java.text.DecimalFormat;
//}}}
/**
* <code>KinImagePlugin</code> makes kinemages from images (kin <- image).
* 
* The resulting kin will fit within a 400 x 400 box and its lists will have
* the 'screen' keyword so it stays immobile with zooms and flatland drags.
* It should, however, rescale to the screen's max dimension if the KiNG 
* window gets resized.
* 
* Note that, cleverly, "KinImage" sounds like "kinemage."
* (wild applause)  Thank you - you're too kind!
*/
public class KinImagePlugin extends BasicTool
{
//{{{ Constants
//##############################################################################
    DecimalFormat df  = new DecimalFormat("###");
    DecimalFormat df2 = new DecimalFormat("#.##");
    DecimalFormat df3 = new DecimalFormat("#.#");
//}}}

//{{{ Variable definitions
//##############################################################################
    /** Take 1 of every n pixels from original image */
    int                     imageResol   = 1;
    
    BufferedImage           image        = null;
    String                  imageName    = null;
    JFrame                  imageFrame   = null;
    
    HashSet<Triple>         newColorSet  = null;
    HashMap<Triple, String> colorMap     = null;
    
    /** For scaling down to 400 x 400 */
    int[]                   rawBounds    = null;
    int[]                   scaledBounds = null;
    
    JFileChooser  fileChooser  = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KinImagePlugin(ToolBox tb)
    {
        super(tb);
    }
//}}}

//{{{ onStart
//##################################################################################################
    public void onStart(ActionEvent ev)
    {
	    loadImage();
        showImage();
        doKin(); // actually based on Vince's doKin2() from cmdline.KinImage
    }
//}}}

//{{{ loadImage
//##############################################################################
    public void loadImage()
    {
        String imageSource = askImageSource();
        if(imageSource.equals("local file"))
        {
            // Create file chooser on demand
            if(fileChooser == null) makeFileChooser();
            if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(kMain.getTopWindow()))
            {
                try 
                {
                    File f = fileChooser.getSelectedFile();
                    image = ImageIO.read(f);
                    imageName = f.getName();
                }
                catch(IOException ex) {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                }
                catch(IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                    "Wrong file format was chosen, or file is corrupt:\n"+ex.getMessage(),
                    "Sorry!", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        else //imageSource.equals("URL")
        {
            try
            {
                URL url = new URL("http://kinemage.biochem.duke.edu/images/DaveTheMage.jpg");
                image = ImageIO.read(url);
                imageName = url.toString().substring(url.toString().lastIndexOf("/")+1);
            }
            catch(IOException ex) {
                JOptionPane.showMessageDialog(kMain.getTopWindow(),
                "An I/O error occurred while loading the file:\n"+ex.getMessage(),
                "Sorry!", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        //// Ask for desired kin resolution relative to starting image
        //imageResol = askResol();
    }
//}}}

//{{{ askImageSource, askResol, askAppend
//##############################################################################
    private String askImageSource()
    {
        Object[] choices = {"local file", "DaveTheMage from web"};
        String choice = (String) JOptionPane.showInputDialog(
            kMain.getTopWindow(), "Please indicate the source of your image.", 
            "Choose", JOptionPane.PLAIN_MESSAGE, null, choices, "local file");
        return choice;
    }
    
    private int askResol()
    {
        int resol = 1; // CHANGE LATER
        
        //?????????????
        
        return resol;
    }
    
    private boolean askAppend()
    {
        Object[] choices = {"new kinemage", "append to current kinemage"};
        String choice = (String) JOptionPane.showInputDialog(
            kMain.getTopWindow(), "Make new kin w/ this image or append to current kin?", 
            "Choose", JOptionPane.PLAIN_MESSAGE, null, choices, "new kinemage");
        return (choice.equals("new kinemage") ? false : true);
    }
//}}}

//{{{ makeFileChooser
//##################################################################################################
    void makeFileChooser()
    {
        // Make accessory for file chooser
        TablePane acc = new TablePane();

        // Make actual file chooser -- will throw an exception if we're running as an applet
        fileChooser = new JFileChooser();
        String currDir = System.getProperty("user.dir");
        if(currDir != null) fileChooser.setCurrentDirectory(new File(currDir));
        
        fileChooser.setAccessory(acc);
    }
//}}}

//{{{ showImage
//##############################################################################
    /** Uses a Java window to display the starting image */
    public void showImage()
    {
        imageFrame = new JFrame();
        JLabel label = new JLabel(new ImageIcon(image));
        imageFrame.getContentPane().add(label, BorderLayout.CENTER);
        imageFrame.pack();
        imageFrame.setVisible(true);
        imageFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        imageFrame.setTitle("kin << image from "+imageName+" .. running");
        imageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
//}}}

//{{{ doKin
//##############################################################################
    /** Puts together the coordinates for a kin, scaled to fit 
    * inside a 400 x 400 square, from the provided image */
    private void doKin()
	{
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = new int[w * h];
        
        // Make custom color set (thanks, Vince!)
        newColorSet = new HashSet<Triple>();
        colorMap    = new HashMap<Triple, String>();
        for (int i = 0; i < pixels.length; i += imageResol)
        {
            int x      = i % w;
            int y      = h - i / w;
            int y_flip =     i / w;  // Java images start with row 0 at top!
            pixels[i] = image.getRGB(x, y_flip);
            
            if (x % imageResol == 0 && y % imageResol == 0)
            {
                Color col = new Color(pixels[i]);
                int[] rgb = new int[] {col.getRed(), col.getGreen(), col.getBlue()};
                float[] hsb0to1 = Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], new float[3]);
                float[] hsb = new float[] {hsb0to1[0]*360f, hsb0to1[1]*100f, hsb0to1[2]*100f}; // 0-360, 0-100, 0-100
                Triple hsbTrip = new Triple((double)Math.rint(hsb[0]/5)*5,(double)Math.rint(hsb[1]/5)*5,(double)Math.rint(hsb[2]/5)*5);
                newColorSet.add(hsbTrip);
            }
        }
        int colorInt = 0;
        for (Triple hsbTrip : newColorSet)
        {
            colorMap.put(hsbTrip, "color"+colorInt);
            colorInt++;
        }
        
        // Get parameters for Mage-like scaling later
        calcBounds(pixels, w, h);
        
        // Instead of printing out dots in kin format to stdout, as when 
        // KinImage was in cmdline, now we append to the existing Kinemage 
        // object or make a new oneSystem.out.println("@group {"+imageName+"} dominant");
        boolean append = askAppend();
        plot(pixels, append);
    }
//}}}

//{{{ calcBounds
//##############################################################################
    /** Scales x,y coordinates so image centered on 0,0 and larger dimension 
    * of the two spans from -200 to 200, like Mage 'screen' lists */
    public void calcBounds(int[] pixels, int w, int h)
    {
        // Get bounds of data in raw image
        rawBounds = new int[4]; // xMin, xMax, yMin, yMax
        rawBounds[0] = Integer.MAX_VALUE;  rawBounds[1] = Integer.MIN_VALUE;
        rawBounds[2] = Integer.MAX_VALUE;  rawBounds[3] = Integer.MIN_VALUE;
        for(int i = 0; i < pixels.length; i += imageResol)
        {
            int x      = i % w;
            //int y_flip =     i / w;  // Java images start with row 0 at top!
            int y      = h - i / w;
            
            if(x < rawBounds[0]) rawBounds[0] = x;
            if(x > rawBounds[1]) rawBounds[1] = x;
            if(y < rawBounds[2]) rawBounds[2] = y;
            if(y > rawBounds[3]) rawBounds[3] = y;
        }
        
        // Scale x,y to Mage-like dimensions (wider of two -> -200 to 200)
        // Raw mins should always be 0, but let's play it safe and be flexible anyway
        scaledBounds = new int[4]; // xMin, xMax, yMin, yMax
        if(rawBounds[1]-rawBounds[0] > rawBounds[3]-rawBounds[2]) // x range wider
        {
            double scale = 400.0 / (1.0*rawBounds[1]-rawBounds[0]);
            double diff  = ((rawBounds[1]-rawBounds[0]) - (rawBounds[3]-rawBounds[2])) * scale;
            //System.err.println("scaled x range "+df2.format(diff)+" greater than scaled y range");
            
            // x: -200 to +200
            scaledBounds[0] = -200;
            scaledBounds[1] =  200;
            // y: -?? to +?? where ?? < 200
            scaledBounds[2] = -200 + (int) Math.round(0.5*diff);
            scaledBounds[3] =  200 - (int) Math.round(0.5*diff);
            
        }
        else // y range wider or same
        {
            double scale = 400.0 / (1.0*rawBounds[3]-rawBounds[2]);
            double diff  = ((rawBounds[3]-rawBounds[2]) - (rawBounds[1]-rawBounds[0])) * scale;
            //System.err.println("scaled y range "+df2.format(diff)+" greater than scaled x range");
            
            // x: -?? to +?? where ?? < 200
            scaledBounds[0] = -200 + (int) Math.round(0.5*diff);
            scaledBounds[1] =  200 - (int) Math.round(0.5*diff);
            // y: -200 to +200
            scaledBounds[2] = -200;
            scaledBounds[3] =  200;
        }
        
        //System.err.println("x range: ("+rawBounds[0]+","+rawBounds[1]+")"
        //    +" scales to ("+scaledBounds[0]+","+scaledBounds[1]+")");
        //System.err.println("y range: ("+rawBounds[2]+","+rawBounds[3]+")"
        //    +" scales to ("+scaledBounds[2]+","+scaledBounds[3]+")");
    }
//}}}

//{{{ getScaled[X,Y]
//##############################################################################
    /** Returns Mage 'screen'-like scaled x coordinate */
    public double getScaledX(int x)
    {
        double x_scaled_min  = 1.0*scaledBounds[0];
        double x_scaled_span = 1.0*scaledBounds[1] - 1.0*scaledBounds[0];
        double x_actual_frac = (1.0*x - 1.0*rawBounds[0]) / (1.0*rawBounds[1] - 1.0*rawBounds[0]);
        double x2 = x_scaled_min + (x_scaled_span * x_actual_frac);
        return x2;
    }
    
    /** Returns Mage 'screen'-like scaled y coordinate */
    public double getScaledY(int y)
    {
        double y_scaled_min  = 1.0*scaledBounds[2];
        double y_scaled_span = 1.0*scaledBounds[3] - 1.0*scaledBounds[2];
        double y_actual_frac = (1.0*y - 1.0*rawBounds[2]) / (1.0*rawBounds[3] - 1.0*rawBounds[2]);
        double y2 = y_scaled_min + (y_scaled_span * y_actual_frac);
        return y2;
    }
//}}}

//{{{ plot
//##############################################################################
    /** Actually plots the kin to a KiNG Kinemage object */
    private void plot(int[] pixels, boolean append)
	{
        // Dotlist
        KList list = new KList(KList.DOT, "pixels");
        list.setDimension(2);
        list.setScreen(true);
        ArrayList<KPaint> newKPaints = addPoints(list, pixels); // actually modifies list
        
        // Group
        KGroup group = new KGroup(imageName);
        group.setDominant(true);
        group.add(list);
        
        // Kinemage
        if(append) // append if possible; new kin otherwise
        {
            Kinemage kin = kMain.getKinemage();
            if(kin == null)  kin = new Kinemage(KinfileParser.DEFAULT_KINEMAGE_NAME+"1");
            kin.atFlat = true;
            kin.add(group);
            for(KPaint kp : newKPaints)  kin.addPaint(kp);
            if(kMain.getKinemage() == null)
                kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));
            System.err.println("appended kin w/ group '"+imageName+"' to current kin");
        }
        else // new kin
        {
            Kinemage kin = new Kinemage(imageName);
            kin.atFlat = true;
            kin.add(group);
            for(KPaint kp : newKPaints)  kin.addPaint(kp);
            kMain.getStable().append(Arrays.asList(new Kinemage[] {kin}));
            System.err.println("made kin w/ group '"+imageName+"'");
        }
        
        imageFrame.setTitle("kin << image from "+imageName+" .. done!");
        
    }
//}}}

//{{{ addPoints
//##############################################################################
    /** Adds DotPoints to the passed KList object.
    * Also returns list of custom colors as KPaints */
    private ArrayList<KPaint> addPoints(KList list, int[] pixels)
	{
        ArrayList<KPaint> newKPaints = new ArrayList<KPaint>();
        
        int w = image.getWidth();
        int h = image.getHeight();
        
        for (int i = 0; i < pixels.length; i += imageResol)
        {
            int x      = i % w;
            int y_flip =     i / w;  // Java images start with row 0 at top!
            int y      = h - i / w;
            pixels[i] = image.getRGB(x, y_flip);
            
            if (x % imageResol == 0 && y % imageResol == 0)
            {
                // This pixel necessary for desired "resolution"
                
                // Scaled coords a la Mage's screen keyword
                double x2 = getScaledX(x);
                double y2 = getScaledY(y);
                //System.err.println("("+x+","+y+") scaled to ("+df2.format(x2)+","+df2.format(y2)+")");
                
                // Probably-way-too-complicated color manipulation
                Color col = new Color(pixels[i]);
                int[] rgb = new int[] {col.getRed(), col.getGreen(), col.getBlue()};
                float[] hsb0to1 = Color.RGBtoHSB(rgb[0], rgb[1], rgb[2], new float[3]);
                float[] hsb = new float[] {hsb0to1[0]*360f, hsb0to1[1]*100f, hsb0to1[2]*100f}; // 0-360, 0-100, 0-100
                Triple hsbTrip = new Triple((double)Math.rint(hsb[0]/5)*5,(double)Math.rint(hsb[1]/5)*5,(double)Math.rint(hsb[2]/5)*5);
                String kcolor = colorMap.get(hsbTrip);
                
                if(!(hsb[0] == 0 && hsb[1] == 0 && hsb[2] == 0)) // i.e. not black
                {
                    // Dotpoint
                    DotPoint point = new DotPoint(
                        "pixel "+df3.format(x2)+", "+df3.format(y2));
                    point.setXYZ(x2, y2, 0);
                    
                    KPaint c = KPaint.createLightweightHSV(kcolor, 
                        hsb[0], hsb[1], hsb[2], hsb[0], hsb[1], hsb[2]);
                    point.setColor(c);
                    newKPaints.add(c);
                    
                    list.add(point);
                }
            }
        }
        
        return newKPaints;
    }
//}}}

//{{{ getToolsMenuItem, getHelpAnchor, toString
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    { return new JMenuItem(new ReflectiveAction("Kin <- Image", null, this, "onStart")); }
    
    public JMenuItem getHelpMenuItem()
    { return new JMenuItem(new ReflectiveAction(this.toString(), null, this, "onHelp")); }
    
    public String toString()
    { return "Kin <- Image"; }
//}}}

}//class
