package king;
import java.awt.*;
import java.awt.geom.*;
//import java.io.*;
//import java.util.*;
//import javax.swing.*;
//import duke.kinemage.util.*;

/**
* <code>DotPoint</code> representings a contact dot.
* These dots are not depth-cued by size, though the size can be set at time of creation.
* "Dots" are actually square, so a radius between 1 and 3 is probably desireable.
*
* <p>Begun on Sat Apr 27 09:42:35 EDT 2002
* <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class DotPoint extends KPoint // implements ...
{
//##################################################################################################
    /**
    * Creates a new data point representing a "dot".
    *
    * @param list the list that contains this point
    * @param label the pointID of this point
    */
    public DotPoint(KList list, String label)
    {
        super(list, label);
    }
    
//##################################################################################################
    /**
    * Draws this primitive represented by this point to the specified graphics context.
    *
    * @param g the Graphics2D to draw to
    * @param xoff the offset in x (half the display width)
    * @param yoff the offset in y (half the display height)
    * @param engine the Engine calling this function, which contains many interesting public fields
    */
    public void render(Graphics2D g, float xoff, float yoff, Engine engine)
    {
        int maincolor = getPaintIndex(engine);
        if(maincolor < 0) return;// -1 ==> invisible
        Color[] colors = engine.palette.getEntry(maincolor);

        int width, off, xpt, ypt;
        width = getWidth(engine);
        off = width/2;
        xpt = (int)(x+xoff-off);
        ypt = (int)(yoff-y-off);
        
        g.setPaint( colors[engine.colorCue] );
        g.fillRect(xpt, ypt, width, width);
    }

    /**
    * Draws the primitive represented by this point to the specified graphics context, with high quality graphics suitable for publication.
    * This code was copied directly from render(), with minor modifications!
    *
    * @param g the Graphics2D to draw to
    * @param xoff the offset in x (half the display width)
    * @param yoff the offset in y (half the display height)
    * @param engine the Engine calling this function, which contains many interesting public fields
    */
    public void renderForPrinter(Graphics2D g, float xoff, float yoff, Engine engine)
    {
        int maincolor = getPaintIndex(engine);
        if(maincolor < 0) return;// -1 ==> invisible
        Color[] colors = engine.palette.getEntry(maincolor);

        float width, off, xpt, ypt;
        width = getWidth(engine);
        off = width/2f;
        xpt = x + xoff - off;
        ypt = yoff - y - off;
        
        g.setPaint( colors[engine.colorCue] );
        g.setStroke(ColorManager.pen0);
        g.fill(new Ellipse2D.Float(xpt, ypt, width, width));
    }
    
    // Default way of finding the right line width to use, given the settings in the engine
    // Dots never do depth cueing by width, because big square dots look crappy.
    int getWidth(Engine engine)
    {
        if(engine.thinLines) return 1;
        else if(parent != null) return parent.width;
        else return 2;
    }
}//class
