// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.core;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import driftwood.r3.*;
import driftwood.util.*;;
//}}}
/**
* <code>Engine2D</code> does "traditional" drawing using java.awt.Graphics objects,
* based on the transformed coordinates.
*
* <p>Begun on Mon Apr 22 17:21:31 EDT 2002
* <br>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
*/
public class Engine2D extends Engine
{
//{{{ Variables
//##################################################################################################
    // READ ONLY: Parameters for painting points
    public Painter      painter         = null;
    public boolean      useStereo       = false;
    public float        stereoRotation  = 0;
    public Font         bigFont, smallFont;
    
    Rectangle           canvasRect = new Rectangle();
    boolean             transparentBackground = false; // used only for certain export features
//}}}
    
//{{{ Constructor
//##################################################################################################
    /**
    * Creates a new rendering engine.
    */
    public Engine2D()
    {
        super();
        
        stereoRotation  = (float)Math.toRadians(6.0);
        bigFont         = new Font("SansSerif", Font.PLAIN, 24);
        smallFont       = new Font("SansSerif", Font.PLAIN, 12);
    }
//}}}
    
//{{{ render
//##################################################################################################
    /**
    * Transforms the given Transformable and renders it to a graphics context.
    * @param xformable      the Transformable that will be transformed and rendered
    * @param view           a KView representing the current rotation/zoom/clip
    * @param bounds         the bounds of the area to render to
    * @param painter        the Painter that should be used for rendering stuff this pass
    */
    public void render(Transformable xformable, KView view, Rectangle bounds, Painter painter)
    {
        // The game plan:
        // 1. Paint background
        // 2. Load drawing variables
        // 3. For each region (there are 2 for stereo):
        //  a. Transform the coordinates from model-space to screen-space,
        //     add transformed objects to Z-buffer.
        //  b. Paint all objects in Z-buffer, from back to front.
        
        this.painter = painter;
        this.canvasRect.setBounds(bounds);
        
        // Get colors and prepare the graphics device
        painter.setViewport(bounds.x, bounds.y, bounds.width, bounds.height);
        Color backgroundClearColor;
        if(whiteBackground)
        {
            if(monochrome)  backgroundMode = KPaint.WHITE_MONO;
            else            backgroundMode = KPaint.WHITE_COLOR;
            backgroundClearColor = KPaint.white;
        }
        else
        {
            if(monochrome)  backgroundMode = KPaint.BLACK_MONO;
            else            backgroundMode = KPaint.BLACK_COLOR;
            backgroundClearColor = KPaint.black;
        }
        
        if(this.transparentBackground)
        {
            backgroundClearColor = new Color(
                backgroundClearColor.getRed(),
                backgroundClearColor.getGreen(),
                backgroundClearColor.getBlue(),
                0); // alpha = 0  -->  transparent
            this.transparentBackground = false; // disabled after one pass
        }
        
        painter.clearCanvas(backgroundClearColor);
        
        // Set some last-minute drawing variables
        markerSize          = (bigMarkers   ? 2         : 1);
        labelFont           = (bigLabels    ? bigFont   : smallFont);
        painter.setFont(labelFont);
        
        if(useStereo)
        {
            int halfwidth = Math.max(0, bounds.width/2 - 10);
            // This way, toggling cross-eye vs wall-eye just swaps the two images!
            // This makes figure-making much easier, as you can easily do both versions.
            KView leftView = (KView)view.clone(), rightView = (KView)view.clone();
            if(stereoRotation < 0)  leftView.rotateY(stereoRotation);
            else                    rightView.rotateY(-stereoRotation);

            Rectangle clipRgn = new Rectangle();
            clipRgn.setBounds(  bounds.x, bounds.y, halfwidth, bounds.height);
            painter.setViewport(bounds.x, bounds.y, halfwidth, bounds.height);
            renderLoop(xformable,  leftView, clipRgn);
            
            clipRgn.setBounds(  (bounds.x + bounds.width - halfwidth), bounds.y, halfwidth, bounds.height);
            painter.setViewport((bounds.x + bounds.width - halfwidth), bounds.y, halfwidth, bounds.height);
            renderLoop(xformable, rightView, clipRgn);
        
            // Have to re-activate all of the screen for drawing during overpaint
            painter.setViewport(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        else//!useStereo
        {
            renderLoop(xformable, view, bounds);
        }
    }
//}}}

//{{{ renderLoop
//##################################################################################################
    /**
    * Transforms the given Transformable and renders it to a graphics context.
    * @param xformable      the Transformable that will be transformed and rendered
    * @param view           a KView representing the current rotation/zoom/clip
    * @param bounds         the bounds of the area to render to.
    *   Note that this function does not clip g to ensure that it only paints within these bounds!
    */
    void renderLoop(Transformable xformable, KView view, Rectangle bounds)
    {
        this.transform(xformable, view, bounds);
        
        // Now paint them to the graphics
        for(int i = 0; i <= TOP_LAYER; i++)
        {
            // Calculate depth-cueing constants for this level
            if(cueIntensity)    colorCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
            else                colorCue = KPaint.COLOR_LEVELS - 1;
            if(cueThickness)    widthCue = (KPaint.COLOR_LEVELS*i)/(TOP_LAYER+1);
            else                widthCue = (KPaint.COLOR_LEVELS-1) / 2;
            
            //if(colorCue >= KPaint.COLOR_LEVELS)
            //    SoftLog.err.println("colorCue = "+colorCue+"; i = "+i+"; TOP_LAYER = "+TOP_LAYER);
            
            // Render all non-screen points at this level (faster to not use iterators)
            ArrayList<KPoint>   zb      = zbuffer[i];
            ArrayList<KList>    pnt     = parents[i];
            for(int j = 0, end_j = zb.size(); j < end_j; j++)
            {
                KPoint  pt  = (KPoint) zb.get(j);
                KList   l   = (KList) pnt.get(j);
                if(l == null)
                {
                    renderPoint(pt);
                }
                else // see setActingParent() for an explanation
                {
                    KList oldPnt = pt.getParent();
                    pt.setParent(l);
                    renderPoint(pt);
                    pt.setParent(oldPnt);
                }
            }
        }
    }    
//}}}

//{{{ renderPoint
//##################################################################################################
    /**
    * Renders a single point provided by renderLoop().
    * This method was added so multiple for-loops in renderLoop()
    * go through the exact same point-rendering procedure.
    */
    void renderPoint(KPoint pt)
    {
        // The stored parent at this point is the proper one,
        // even if we're doing the weird acting parent hijinx.
        KList parent = pt.getParent();
        if(!parent.getScreen()) pt.paint2D(this);
        else
        {
            // Screen-oriented hack (DAK 090506)
            // Seems like Ian took pains to speed up operations in this 
            // method, but my hack addition here should be accessed only
            // rarely and thus shouldn't slow KiNG down too much.
            int oldColorCue = colorCue;
            int oldWidthCue = widthCue;
            colorCue = KPaint.COLOR_LEVELS/2;//-1; 
            widthCue = KPaint.COLOR_LEVELS/2;//-1; 
            pt.paint2D(this);
            colorCue = oldColorCue;
            widthCue = oldWidthCue;
        }
    }
//}}}

//{{{ getCanvasSize, setTransparentBackground
//##################################################################################################
    /** Returns the size of the last rendering operation. */
    public Dimension getCanvasSize()
    {
        return new Dimension(canvasRect.width, canvasRect.height);
    }

    /** Sets the canvas size, for use when bypassing render() (e.g. VBO path). */
    public void setCanvasSize(int width, int height)
    {
        this.canvasRect.setSize(width, height);
    }

    /**
    * Makes the background be rendered as transparent for the next pass ONLY.
    * This is useful for certain export features that don't want a black/white
    * box hanging around behind the image.
    */
    public void setTransparentBackground()
    { this.transparentBackground = true; }
//}}}

//{{{ paintZBuffer
//##################################################################################################
    /**
    * Paints the current zbuffer contents using the given painter.
    * Used for rendering overlay geometry (markers, measures) that was
    * transformed via transform() but not painted through the normal
    * render pipeline (e.g. when the VBO renderer handles main geometry).
    */
    public void paintZBuffer(Painter p)
    {
        this.painter = p;
        p.setFont(labelFont);

        for(int i = 0; i <= TOP_LAYER; i++)
        {
            // No depth cueing for overlay — render at full brightness
            colorCue = KPaint.COLOR_LEVELS - 1;
            widthCue = (KPaint.COLOR_LEVELS - 1) / 2;

            ArrayList<KPoint>   zb  = zbuffer[i];
            ArrayList<KList>    pnt = parents[i];
            for(int j = 0, end_j = zb.size(); j < end_j; j++)
            {
                KPoint pt = zb.get(j);
                KList  l  = pnt.get(j);
                if(l == null)
                {
                    pt.paint2D(this);
                }
                else
                {
                    KList oldPnt = pt.getParent();
                    pt.setParent(l);
                    pt.paint2D(this);
                    pt.setParent(oldPnt);
                }
            }
        }
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class
