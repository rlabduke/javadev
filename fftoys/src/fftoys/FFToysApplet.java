// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package fftoys;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>FFToysApplet</code> has not yet been documented.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Tue Feb 10 10:03:19 EST 2004
*/
public class FFToysApplet extends JApplet
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    JTabbedPane     tabpane = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public FFToysApplet()
    {
        super();
    }
//}}}

//{{{ init
//##############################################################################
    public void init()
    {
        // Build the GUI
        tabpane = new JTabbedPane();

        DataManager dataman         = new DataManager(makeTestImage3());
        RealPanel   realPane        = new RealPanel(this, dataman);
        ReciprocalPanel recipPane   = new ReciprocalPanel(this, dataman);
        
        tryAddingImage(realPane, "img");
        tryAddingImage(realPane, "img0");
        int i = 1;
        while(tryAddingImage(realPane, "img"+i)) i++;
        
        realPane.addChoosableImage("Sample 1", makeTestImage1());
        realPane.addChoosableImage("Sample 2", makeTestImage2());
        realPane.addChoosableImage("Sample 3", makeTestImage3());
        realPane.addChoosableImage("Sample 4", makeTestImage4());

        tabpane.insertTab("Real space", null, realPane, "", 0);
        tabpane.insertTab("Reciprocal space", null, recipPane, "", 1);
        
        this.setContentPane(tabpane);
        this.validate();
    }
//}}}

//{{{ tryAddingImage
//##############################################################################
    /**
    * Looks for a PARAM of the given name, and if found, tries loading the
    * image file whose URL (absolute or relative) is specified there.
    * Returns true on success and false on failure.
    */
    public boolean tryAddingImage(RealPanel realPane, String paramName)
    {
        String imgName = this.getParameter(paramName);
        if(imgName == null) return false;
        
        try
        {
            URL imgURL = new URL(this.getDocumentBase(), imgName);
            Image img1 = this.getImage(imgURL);
            if(img1 == null) return false;
            
            BufferedImage img2 = DataManager.copyImage(img1);
            int lastSlash = imgName.lastIndexOf("/")+1;
            int lastDot   = imgName.lastIndexOf(".");
            if(lastDot == -1) lastDot = imgName.length();
            realPane.addChoosableImage(imgName.substring(lastSlash, lastDot), img2);
            return true;
        }
        catch(MalformedURLException ex)
        {
            System.err.println("<PARAM> "+paramName+" specified an unresolvable URL.");
            return false;
        }
        catch(IllegalArgumentException ex)
        {
            System.err.println("<PARAM> "+paramName+" specified an non-loadable image URL.");
            return false;
        }
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
                    img.setRGB(i, j, 0xffffffff);
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
                    img.setRGB(i, j, 0xffffffff);
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
                    img.setRGB(i, j, 0xffffffff);
                else if(i%8 == 0 && j%4 == 0)
                    img.setRGB(i, j, 0xff7f7f7f);
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
        g.setColor(Color.white);
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
}//class

