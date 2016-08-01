// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
//}}}
/**
* <code>ColorPicker</code> is a graphical color picker
* for KPaint objects.
*
* <p>Copyright (C) 2003-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Thu Jun 26 11:46:23 EDT 2003
*/
public class ColorPicker extends TablePane2 implements MouseListener
{
//{{{ Constants
//}}}

//{{{ CLASS: ColorPatch
//##################################################################################################
    /** A little patch of color that responds to mouse clicks. */
    protected class ColorPatch extends JComponent implements MouseListener
    {
        KPaint      paint;
        boolean     isSelected = false;
        
        protected ColorPatch(KPaint paint)
        {
            super();
            this.paint = paint;
            setMinimumSize(patchSize);
            setPreferredSize(patchSize);
            setMaximumSize(patchSize);
            setToolTipText(paint.toString());
            addMouseListener(this);
        }
        
        /** Paints our component to the screen */
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g); // this does nothing b/c we have no UI delegate
            Graphics2D  g2      = (Graphics2D)g;
            Dimension   dim     = this.getSize();
            Paint[]     colors  = paint.getPaints(backgroundMode);
            
            // First band occupies half height, others divide remainder evenly
            int start = 0, height = dim.height/2;
            for(int i = KPaint.COLOR_LEVELS-1 ; i >= 0; i--)
            {
                g2.setPaint(colors[i]);
                g2.fillRect(0, start, dim.width, height);
                start   += height;
                height  = (dim.height - start) / (i<1 ? 1 : i);
            }
            
            if(isSelected)
            {
                g2.setPaint(highlight);
                g2.drawRect(0, 0, dim.width-1, dim.height-1);
            }
        }
        
        public void mouseClicked(MouseEvent ev) { selectPatch(this); }
        public void mouseEntered(MouseEvent ev) {}
        public void mouseExited(MouseEvent ev) {}
        public void mousePressed(MouseEvent ev) {}
        public void mouseReleased(MouseEvent ev) {}
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    protected int                       backgroundMode;
    protected Dimension                 patchSize;
    protected Component                 filler;
    protected SwapBox                   extraPatchBox;
    
    protected Color                     background;
    protected Color                     highlight;
    
    protected ColorPatch                selectedPatch = null;
    
    /** Map&lt;String, ColorPatch&gt; for setSelection() */
    protected Map<String, ColorPatch>   patchMap = new HashMap<String, ColorPatch>();
    protected Map<String, ColorPatch>   extraMap = new HashMap<String, ColorPatch>();
    
    /** List of listeners for ChangeEvents */
    protected Collection<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
    
    /** Seems to be roughly the practical limit for fitting color patches into
    * 'Edit list/point properties' windows (DAK 090507) */
    int maxNumNewColors = 36;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public ColorPicker(int backgroundMode, int patchWidth)
    {
        super();
        this.patchSize      = new Dimension(patchWidth, patchWidth);
        this.filler         = Box.createRigidArea(patchSize);
        this.extraPatchBox  = new SwapBox(null); // empty for now
        this.setBackgroundMode(backgroundMode);
        this.setToolTipText("Click empty space to deselect all colors");
        this.addMouseListener(this);
        
        addPatch(KPalette.red); addPatch(KPalette.pink); addPatch(KPalette.pinktint); addFiller();
        newRow();
        addPatch(KPalette.orange); addPatch(KPalette.peach); addPatch(KPalette.peachtint); addFiller();
        newRow();
        addPatch(KPalette.gold); addFiller(); addFiller(); addFiller();
        newRow();
        addPatch(KPalette.yellow); addPatch(KPalette.yellow); addPatch(KPalette.yellowtint); addFiller();
        newRow();
        addPatch(KPalette.lime); addFiller(); addFiller(); addFiller();
        newRow();
        addPatch(KPalette.green); addPatch(KPalette.sea); addPatch(KPalette.greentint); addFiller();
        newRow();
        addPatch(KPalette.sea); addFiller(); addFiller(); addPatch(KPalette.white);
        newRow();
        addPatch(KPalette.cyan); addFiller(); addFiller(); addPatch(KPalette.gray);
        newRow();
        addPatch(KPalette.sky); addFiller(); addFiller(); addPatch(KPalette.brown);
        newRow();
        addPatch(KPalette.blue); addPatch(KPalette.sky); addPatch(KPalette.bluetint); addFiller();
        newRow();
        addPatch(KPalette.purple); addPatch(KPalette.lilac); addPatch(KPalette.lilactint); addFiller();
        newRow();
        addPatch(KPalette.magenta); addFiller(); addFiller(); addPatch(KPalette.deadwhite);
        newRow();
        addPatch(KPalette.hotpink); addFiller(); addPatch(KPalette.invisible); addPatch(KPalette.deadblack);
        newRow();
        insets(0).hfill(true).vfill(true);
        addCell(extraPatchBox, 4, 1);
    }
//}}}

//{{{ addPatch, addFiller, selectPatch
//##################################################################################################
    private void addPatch(KPaint paint)
    {
        ColorPatch patch = new ColorPatch(paint);
        this.addCell(patch);
        patchMap.put(paint.toString(), patch);
    }
    
    private void addFiller()
    {
        this.addCell(filler);
    }
    
    protected void selectPatch(ColorPatch newSelection)
    {
        if(selectedPatch != null)
        {
            selectedPatch.isSelected = false;
            selectedPatch.repaint();
        }
        
        selectedPatch = newSelection;
        if(selectedPatch != null)
        {
            selectedPatch.isSelected = true;
            selectedPatch.repaint();
        }
        
        fireStateChanged();
    }
//}}}

//{{{ add/removeChangeListener, fireStateChanged
//##################################################################################################
    public void addChangeListener(ChangeListener l)
    {
        changeListeners.add(l);
    }
    
    public void removeChangeListener(ChangeListener l)
    {
        changeListeners.remove(l);
    }
    
    /** Notifies all listeners and repaints this component */
    protected void fireStateChanged()
    {
        ChangeEvent ev = new ChangeEvent(this);
        for(ChangeListener l : changeListeners)
            l.stateChanged(ev);
    }
//}}}

//{{{ get/set{Selection, BackgroundMode}
//##################################################################################################
    /** Returns the selected KPaint, or null for none */
    public KPaint getSelection()
    {
        if(selectedPatch == null) return null;
        else return selectedPatch.paint;
    }
    
    public void setSelection(String color)
    {
        ColorPatch          patch = patchMap.get(color);
        if(patch == null)   patch = extraMap.get(color);
        selectPatch(patch);
    }
    
    public void setSelection(KPaint color)
    {
        if(color == null)   setSelection((String)null);
        else                setSelection(color.toString());
    }
    
    /** Returns one of the KPaint background mode integers. */
    public int getBackgroundMode()
    { return backgroundMode; }
    
    public void setBackgroundMode(int mode)
    {
        this.backgroundMode = mode;
        
        if(backgroundMode == KPaint.BLACK_COLOR)
        {
            background  = KPaint.black;
            highlight  = KPaint.white;
        }
        else if(backgroundMode == KPaint.BLACK_MONO)
        {
            background  = KPaint.black;
            highlight  = KPaint.white;
        }
        else if(backgroundMode == KPaint.WHITE_COLOR)
        {
            background  = KPaint.white;
            highlight  = KPaint.black;
        }
        else if(backgroundMode == KPaint.WHITE_MONO)
        {
            background  = KPaint.white;
            highlight  = KPaint.black;
        }
        
        setOpaque(true);
        setBackground(background);
        repaint();
    }
//}}}

//{{{ setExtras
//##################################################################################################
    /**
    * Allows this component to display extra colors, e.g. from colorsets.
    * Usually fed the output of Kinemage.getNewPaintMap().values().
    * @param kPaints a Collection&lt;KPaint&gt;; may be null.
    */
    public void setExtras(Collection<KPaint> kPaints)
    {
        extraPatchBox.setTarget(null);
        extraMap.clear();
        if(kPaints == null) return;
        
        int i = 0;
        TablePane2 tp = new TablePane2();
        tp.setOpaque(false); // lets black/white show through
        for(KPaint paint : kPaints)
        {
            if(i >= maxNumNewColors)
            {
                System.err.println("Too many user-defined colors: "+kPaints.size()+" (max = "+maxNumNewColors+")");
                break;
            }
            ColorPatch patch = new ColorPatch(paint);
            tp.addCell(patch);
            extraMap.put(paint.toString(), patch);
            if(++i % 4 == 0) tp.newRow();
        }
        
        extraPatchBox.setTarget(tp);
    }
//}}}

//{{{ mouse{Clicked, Entered, Exited, Pressed, Released}
//##################################################################################################
    public void mouseClicked(MouseEvent ev) { selectPatch(null); }
    public void mouseEntered(MouseEvent ev) {}
    public void mouseExited(MouseEvent ev) {}
    public void mousePressed(MouseEvent ev) {}
    public void mouseReleased(MouseEvent ev) {}
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

