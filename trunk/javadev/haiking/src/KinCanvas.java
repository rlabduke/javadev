// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package kinglite;

import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
//}}}
/**
* <code>KinCanvas</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Jan 28 15:32:36 EST 2005
*/
public class KinCanvas extends Canvas implements CommandListener
{
//{{{ Constants
    static final int ZBUF_BITS = 7;
    static final int ZBUF_SIZE = 1<<ZBUF_BITS;
    static final int TEXT_ANCHOR = Graphics.BASELINE | Graphics.LEFT;
    
    static final int COLOR_BITS = 4;
    static final int[] red = { 0x330000, 0x410000, 0x4e0000, 0x5c0000, 0x690000, 0x770000, 0x850000, 0x920000, 0xa00000, 0xad0000, 0xbb0000, 0xc90000, 0xd60000, 0xe40000, 0xf10000, 0xff0000 };
    static final int[] orange = { 0x331100, 0x411600, 0x4e1a00, 0x5c1f00, 0x692300, 0x772800, 0x852c00, 0x923100, 0xa03500, 0xad3a00, 0xbb3e00, 0xc94300, 0xd64700, 0xe44c00, 0xf15000, 0xff5500 };
    static final int[] gold = { 0x332200, 0x412b00, 0x4e3400, 0x5c3d00, 0x694600, 0x774f00, 0x855800, 0x926100, 0xa06b00, 0xad7400, 0xbb7d00, 0xc98600, 0xd68f00, 0xe49800, 0xf1a100, 0xffaa00 };
    static final int[] yellow = { 0x333300, 0x414100, 0x4e4e00, 0x5c5c00, 0x696900, 0x777700, 0x858500, 0x929200, 0xa0a000, 0xadad00, 0xbbbb00, 0xc9c900, 0xd6d600, 0xe4e400, 0xf1f100, 0xffff00 };
    static final int[] lime = { 0x223300, 0x2b4100, 0x344e00, 0x3d5c00, 0x466900, 0x4f7700, 0x588500, 0x619200, 0x6ba000, 0x74ad00, 0x7dbb00, 0x86c900, 0x8fd600, 0x98e400, 0xa1f100, 0xaaff00 };
    static final int[] green = { 0xa330a, 0xd410d, 0x104e10, 0x125c12, 0x156915, 0x187718, 0x1b851b, 0x1d921d, 0x20a020, 0x23ad23, 0x25bb25, 0x28c928, 0x2bd62b, 0x2ee42e, 0x30f130, 0x33ff33 };
    static final int[] sea = { 0x331a, 0x4120, 0x4e27, 0x5c2e, 0x6935, 0x773c, 0x8542, 0x9249, 0xa050, 0xad57, 0xbb5e, 0xc964, 0xd66b, 0xe472, 0xf179, 0xff80 };
    static final int[] cyan = { 0x2b2b, 0x3737, 0x4242, 0x4e4e, 0x5a5a, 0x6565, 0x7171, 0x7c7c, 0x8888, 0x9393, 0x9f9f, 0xabab, 0xb6b6, 0xc2c2, 0xcdcd, 0xd9d9 };
    static final int[] sky = { 0xc1e30, 0xf263d, 0x132e4a, 0x163757, 0x193f64, 0x1c4771, 0x1f4f7e, 0x23578b, 0x265f98, 0x2967a5, 0x2c6fb2, 0x3077bf, 0x337fcb, 0x3687d8, 0x398fe5, 0x3d97f2 };
    static final int[] blue = { 0xf0f33, 0x131341, 0x17174e, 0x1c1c5c, 0x202069, 0x242477, 0x282885, 0x2c2c92, 0x3030a0, 0x3434ad, 0x3838bb, 0x3c3cc9, 0x4040d6, 0x4444e4, 0x4848f1, 0x4d4dff };
    static final int[] purple = { 0x230d33, 0x2c1041, 0x36144e, 0x3f175c, 0x481a69, 0x521e77, 0x5b2185, 0x652592, 0x6e28a0, 0x772bad, 0x812fbb, 0x8a32c9, 0x9336d6, 0x9d39e4, 0xa63cf1, 0xaf40ff };
    static final int[] magenta = { 0x330333, 0x410341, 0x4e044e, 0x5c055c, 0x690569, 0x770677, 0x850785, 0x920792, 0xa008a0, 0xad09ad, 0xbb09bb, 0xc90ac9, 0xd60bd6, 0xe40be4, 0xf10cf1, 0xff0dff };
    static final int[] hotpink = { 0x330015, 0x41001b, 0x4e0021, 0x5c0026, 0x69002c, 0x770032, 0x850037, 0x92003d, 0xa00043, 0xad0048, 0xbb004e, 0xc90054, 0xd60059, 0xe4005f, 0xf10065, 0xff006a };
    static final int[] pink = { 0x33171c, 0x411d23, 0x4e232a, 0x5c2932, 0x692f39, 0x773640, 0x853c48, 0x92424f, 0xa04857, 0xad4e5e, 0xbb5465, 0xc95a6d, 0xd66074, 0xe4677b, 0xf16d83, 0xff738a };
    static final int[] peach = { 0x331d0d, 0x412410, 0x4e2c14, 0x5c3417, 0x693b1a, 0x77431e, 0x854b21, 0x925225, 0xa05a28, 0xad622b, 0xbb692f, 0xc97132, 0xd67836, 0xe48039, 0xf1883c, 0xff8f40 };
    static final int[] lilac = { 0x271733, 0x321d41, 0x3c234e, 0x47295c, 0x512f69, 0x5c3677, 0x663c85, 0x714292, 0x7b48a0, 0x864ead, 0x9054bb, 0x9b5ac9, 0xa560d6, 0xb067e4, 0xba6df1, 0xc573ff };
    static final int[] pinktint = { 0x332429, 0x412d34, 0x4e373f, 0x5c4049, 0x694a54, 0x77535f, 0x855d6a, 0x926675, 0xa07080, 0xad798b, 0xbb8396, 0xc98ca0, 0xd696ab, 0xe49fb6, 0xf1a9c1, 0xffb3cc };
    static final int[] peachtint = { 0x33241a, 0x412e20, 0x4e3727, 0x5c412e, 0x694b35, 0x77543c, 0x855e42, 0x926849, 0xa07150, 0xad7b57, 0xbb845e, 0xc98e64, 0xd6986b, 0xe4a172, 0xf1ab79, 0xffb580 };
    static final int[] yellowtint = { 0x33331a, 0x414120, 0x4e4e27, 0x5c5c2e, 0x696935, 0x77773c, 0x858542, 0x929249, 0xa0a050, 0xadad57, 0xbbbb5e, 0xc9c964, 0xd6d66b, 0xe4e472, 0xf1f179, 0xffff80 };
    static final int[] greentint = { 0x1f3324, 0x27412d, 0x2f4e37, 0x375c40, 0x3f694a, 0x477753, 0x50855d, 0x589266, 0x60a070, 0x68ad79, 0x70bb83, 0x78c98c, 0x81d696, 0x89e49f, 0x91f1a9, 0x99ffb3 };
    static final int[] bluetint = { 0x1f2533, 0x272f41, 0x2f394e, 0x37435c, 0x3f4d69, 0x475777, 0x506185, 0x586b92, 0x6075a0, 0x687fad, 0x7089bb, 0x7893c9, 0x819dd6, 0x89a7e4, 0x91b1f1, 0x99bbff };
    static final int[] lilactint = { 0x2c2133, 0x372a41, 0x43334e, 0x4e3c5c, 0x5a4569, 0x664d77, 0x715685, 0x7d5f92, 0x8868a0, 0x9471ad, 0xa07abb, 0xab82c9, 0xb78bd6, 0xc394e4, 0xce9df1, 0xdaa6ff };
    static final int[] white = { 0x333333, 0x414141, 0x4e4e4e, 0x5c5c5c, 0x696969, 0x777777, 0x858585, 0x929292, 0xa0a0a0, 0xadadad, 0xbbbbbb, 0xc9c9c9, 0xd6d6d6, 0xe4e4e4, 0xf1f1f1, 0xffffff };
    static final int[] gray = { 0x1a1a1a, 0x202020, 0x272727, 0x2e2e2e, 0x353535, 0x3c3c3c, 0x424242, 0x494949, 0x505050, 0x575757, 0x5e5e5e, 0x646464, 0x6b6b6b, 0x727272, 0x797979, 0x808080 };
    static final int[] brown = { 0x261b15, 0x30221b, 0x3b2920, 0x453026, 0x4f372b, 0x593e31, 0x634637, 0x6e4d3c, 0x785442, 0x825b48, 0x8c624d, 0x966953, 0xa17058, 0xab785e, 0xb57f64, 0xbf8669 };    
    //                              0    1       2     3       4     5      6
    static final int[][] colors = { red, orange, gold, yellow, lime, green, sea,
    //  7     8    9     10      11       12       13    14     15     16
        cyan, sky, blue, purple, magenta, hotpink, pink, peach, lilac, pinktint,
    //  17         18          19         20        21         22     23    24
        peachtint, yellowtint, greentint, bluetint, lilactint, white, gray, brown,
    //  25     26     27     28     29     30     31
        white, white, white, white, white, white, white };
//}}}

//{{{ Variable definitions
//##############################################################################
    KingMain        kMain;
    Command         cmdToggleFlatland;
    Command         cmdPickcenter;
    Command         cmdChooseKin;
    Command         cmdHide;
    Command         cmdTogglePersp;
    Command         cmdDoubleBuffer;
    // For tracking pointer motion
    int             ptrX = 0, ptrY = 0, dragTotal = 0;
    boolean         nearTop = false, doPickcenter = false, doFlatland = false;
    // For drawing
    Font            labelFont;
    Image           backBuffer = null;
    Graphics        gBuffer = null;
    KPoint[]        zbuf;
    View            view;
    boolean         scalingIsDirty = true, usePersp = true, useDblBuf;
    int             clip, clipStep;
    KPoint          tailPt; // tail of the linked list of all points
    KPoint          drawPt; // tail of the list of rotate/draw points
    String          screenMsg = ""; // pointID type info
    long            msgTimeout = Long.MAX_VALUE; // time to disappear, in msec.
    // For profiling
    long            xformTime = 0, drawTime = 0;
    // For hiding point types
    Command         hideOK;
    List            hideList;
    final String[]  hideNames = { "Dots", "Balls", "Lines", "Labels" };
    final int[]     hideMasks = {
        KPoint.MASK_DOT_SMALL | KPoint.MASK_DOT_MEDIUM | KPoint.MASK_DOT_LARGE,
        KPoint.MASK_BALL,
        KPoint.MASK_VECTOR_NODRAW | KPoint.MASK_VECTOR_DRAW1 | KPoint.MASK_VECTOR_DRAW2,
        KPoint.MASK_LABEL
    };
//}}}

//{{{ Constructor(s)
//##############################################################################
    public KinCanvas(KingMain kMain)
    {
        super();
        this.kMain = kMain;
        this.labelFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        this.useDblBuf = !this.isDoubleBuffered(); // do double buffering only if not automatic
        this.clip = Math.min(this.getWidth(), this.getHeight()) / 2;
        this.clipStep = Math.max(5, clip/8);
        this.setCommandListener(this);
        
        cmdToggleFlatland = new Command("Translt", Command.SCREEN, 1);
        this.addCommand(cmdToggleFlatland);
        cmdPickcenter = new Command("Pickctr", Command.SCREEN, 2);
        this.addCommand(cmdPickcenter);
        cmdChooseKin = new Command("Choose kin", Command.SCREEN, 3);
        this.addCommand(cmdChooseKin);
        cmdHide = new Command("Show/hide pts", Command.SCREEN, 3);
        this.addCommand(cmdHide);
        cmdTogglePersp = new Command("Perspective", Command.SCREEN, 3);
        this.addCommand(cmdTogglePersp);
        cmdDoubleBuffer = new Command("Dbl buffer", Command.SCREEN, 3);
        this.addCommand(cmdDoubleBuffer);
        
        this.clearKinemage();
        
        this.hideList = new List("Show/hide", List.MULTIPLE, hideNames, null);
        hideList.setCommandListener(this);
        hideOK = new Command("OK", Command.OK, 1);
        hideList.addCommand(hideOK);
    }
//}}}

//{{{ loadKinemage, clearKinemage
//##############################################################################
    /** Sets a new kinemage for display */
    public void loadKinemage(KPoint tailPt, View view)
    {
        this.tailPt = tailPt;
        this.view   = view;
        this.scalingIsDirty = true;
    }
    
    /** Replaces the current kin with a single point and frees the memory. */
    public void clearKinemage()
    {
        // First allow all this to be gc'd
        this.zbuf = null;
        this.tailPt = this.drawPt = null;
        this.view = null;
        KPoint.pointIDs.clear();

        // Now make sure we don't get NPEs
        this.tailPt = new KPoint(0, 0, 0, KPoint.TYPE_LABEL);
        this.tailPt.setPointID("No kin loaded");
        this.zbuf = new KPoint[ZBUF_SIZE];
        this.view = new View();
        this.scalingIsDirty = true;
    }
//}}}

//{{{ paint
//##############################################################################
    public void paint(Graphics gScreen)
    {
        int w = this.getWidth(), h = this.getHeight();
        int hw = w/2, hh = h/2;
        Graphics g = gScreen;
        if(useDblBuf)
        {
            if(backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h)
            {
                this.backBuffer = null;
                this.gBuffer = null;
                this.backBuffer = Image.createImage(this.getWidth(), this.getHeight());
                this.gBuffer = this.backBuffer.getGraphics();
                this.scalingIsDirty = true;
                System.err.println("Allocated new back buffer.");
            }
            g = gBuffer;
        }

        xformTime = System.currentTimeMillis();
        if(scalingIsDirty || drawPt == null)
        {
            this.drawPt = view.centerAndScale(tailPt, hw*hw + hh*hh + clip*clip);
            scalingIsDirty = false;
            /*int cnt = 0;
            KPoint p = drawPt;
            for( ; p != null; cnt++) p = p.prevDrawable;
            System.err.println(cnt+" points in drawable list");*/
        }
        
        if(usePersp)    view.rotateAndRecenterPersp(drawPt, hw, hh);
        else            view.rotateAndRecenter(drawPt, hw, hh);
        zsort(drawPt, -clip, clip);
        
        drawTime = System.currentTimeMillis();
        xformTime = drawTime - xformTime;

        g.setColor(0, 0, 0);
        g.fillRect(0, 0, w, h);
        zpaint(g);
        
        if(this.msgTimeout >= System.currentTimeMillis())
        {
            g.setColor(255, 255, 255);
            g.drawString(this.screenMsg, 2, h-2, Graphics.BOTTOM | Graphics.LEFT);
        }
        
        if(useDblBuf) gScreen.drawImage(backBuffer, 0, 0, Graphics.TOP | Graphics.LEFT);
        drawTime = System.currentTimeMillis() - drawTime;
    }
//}}}

//{{{ zsort
//##############################################################################
    private void zsort(KPoint pt, int clipBack, int clipFront)
    {
        // Clean old Z-buffer
        for(int i = 0; i < ZBUF_SIZE; i++) zbuf[i] = null;
        
        // Refil new Z-buffer
        int clipDepth = clipFront - clipBack;
        while(pt != null)
        {
            int zdraw = pt.getDrawingZ();
            if(clipBack <= zdraw && zdraw < clipFront)
            {
                int level = ((zdraw - clipBack) << ZBUF_BITS) / clipDepth;
                pt.zchain = zbuf[level];
                zbuf[level] = pt;
            }
            pt = pt.prevDrawable;
        }
    }
//}}}

//{{{ zpaint
//##############################################################################
    private void zpaint(Graphics g)
    {
        KPoint p, q;
        g.setFont(labelFont);
        for(int i = 0; i < ZBUF_SIZE; i++)
        {
            p = zbuf[i];
            int colorIndex = i >> (ZBUF_BITS - COLOR_BITS);
            while(p != null)
            {
                g.setColor( colors[p.getColor()][colorIndex] );
                switch(p.getType())
                {
                //case KPoint.TYPE_VECTOR_NODRAW:
                case KPoint.TYPE_VECTOR_DRAW1:
                    q = p.prev; // not null b/c this is a draw pt
                    g.drawLine(p.x2, p.y2, q.x2, q.y2);
                    break;
                case KPoint.TYPE_VECTOR_DRAW2:
                    q = p.prev; // not null b/c this is a draw pt
                    g.drawLine(p.x2, p.y2, q.x2, q.y2);
                    // There should be a more efficient way to do this, probably
                    // involving bit operations. But dx/dy =? 0 gives div by 0.
                    if(Math.abs(p.x2-q.x2) < Math.abs(p.y2-q.y2))
                        g.drawLine(p.x2+1, p.y2, q.x2+1, q.y2);
                    else
                        g.drawLine(p.x2, p.y2+1, q.x2, q.y2+1);
                    break;
                case KPoint.TYPE_DOT_SMALL:
                    g.drawLine(p.x2, p.y2, p.x2, p.y2);
                    break;
                case KPoint.TYPE_DOT_MEDIUM:
                    g.fillRect(p.x2-1, p.y2-1, 3, 3);
                    break;
                case KPoint.TYPE_DOT_LARGE:
                    g.fillRect(p.x2-2, p.y2-2, 5, 5);
                    break;
                case KPoint.TYPE_BALL:
                    int r = p.getRadius() >> view.getScale();
                    if(r < 1) r = 1;
                    int d = r<<1;
                    g.fillArc(p.x2-r, p.y2-r, d, d, 0, 360);
                    break;
                case KPoint.TYPE_LABEL:
                    g.drawString(p.getPointID(), p.x2, p.y2, TEXT_ANCHOR);
                    break;
                }
                p = p.zchain;
            }
        }
    }
//}}}

//{{{ setMessage, pick
//##############################################################################
    /**
    * Sets the message that will appear on the canvas when the image is redrawn.
    * @param timeout    after this time (from System.currentTimeMillis()),
    *   the message will no longer be drawn during redraws.
    */
    void setMessage(String s, long timeout)
    {
        this.screenMsg = s;
        this.msgTimeout = timeout;
    }
    
    void setMessage(String s)
    { setMessage(s, Long.MAX_VALUE); }
    
    KPoint pick(int x, int y)
    {
        for(int i = 0; i < ZBUF_SIZE; i++)
        {
            KPoint p = zbuf[i];
            while(p != null)
            {
                int dx = x - p.x2, dy = y - p.y2;
                if(dx*dx + dy*dy <= 13) return p;
                p = p.zchain;
            }
        }
        return null;
    }
//}}}

//{{{ pointerPressed/Released/Dragged
//##############################################################################
    public void pointerPressed(int x, int y)
    {
        this.ptrX = x;
        this.ptrY = y;
        this.dragTotal = 0;
        this.nearTop = (y <= this.getHeight() / 8);
    }
    
    public void pointerReleased(int x, int y)
    {
        if(this.dragTotal < 3 && doPickcenter) // a nominal "click"
        {
            KPoint p = pick(x, y);
            if(p != null)
            {
                view.cx = p.x0;
                view.cy = p.y0;
                view.cz = p.z0;
                doPickcenter = false;
                scalingIsDirty = true;
                repaint();
            }
        }
    }
    
    public void pointerDragged(int x, int y)
    {
        int dx = x-ptrX, dy = y-ptrY;
        if(doFlatland)
        {
            if(dx != 0 || dy != 0)
            {
                view.translate(dx, -dy);
                scalingIsDirty = true;
            }
        }
        else if(nearTop)
        {
            if(dx != 0) view.rotate(3, -2*dx);
        }
        else
        {
            if(dx != 0) view.rotate(2, 2*dx);
            if(dy != 0) view.rotate(1, 2*dy);
        }

        this.ptrX = x;
        this.ptrY = y;
        this.dragTotal += Math.abs(dx) + Math.abs(dy);
        if(dx != 0 || dy != 0) this.repaint();
    }
//}}}

//{{{ keyPressed, keyRepeated
//##############################################################################
    public void keyPressed(int keyCode)
    {
        int gameAction = getGameAction(keyCode);
        switch(gameAction)
        {
        case DOWN:
            view.setScale(view.getScale()-1);
            this.scalingIsDirty = true;
            repaint();
            break;
        case UP:
            view.setScale(view.getScale()+1);
            this.scalingIsDirty = true;
            repaint();
            break;
        case LEFT:
            this.clip = Math.max(clipStep, clip - clipStep);
            repaint();
            break;
        case RIGHT:
            this.clip += clipStep;
            repaint();
            break;
        case FIRE:
            this.commandAction(cmdToggleFlatland, this);
            break;
        case GAME_A:
            this.commandAction(cmdPickcenter, this);
            break;
        case GAME_B:
            this.commandAction(cmdChooseKin, this);
            break;
        }
    }
    
    public void keyRepeated(int keyCode)
    { keyPressed(keyCode); }
//}}}

//{{{ commandAction
//##############################################################################
    public void commandAction(Command c, Displayable s)
    {
        if(c == cmdChooseKin)
        {
            Display.getDisplay(kMain).setCurrent(kMain.kLoader);
        }
        else if(c == cmdToggleFlatland)
            this.doFlatland = !this.doFlatland;
        else if(c == cmdPickcenter)
            this.doPickcenter = true;
        else if(c == cmdTogglePersp)
        {
            this.usePersp = !this.usePersp;
            repaint();
        }
        else if(c == cmdDoubleBuffer)
            this.useDblBuf = !this.useDblBuf && !this.isDoubleBuffered();
        else if(c == cmdHide)
        {
            // Sync display in list to current settings of our View
            int hideMask = this.view.getHideMask();
            boolean[] toShow = new boolean[hideList.size()];
            for(int i = 0; i < toShow.length; i++)
                toShow[i] = ((hideMask & hideMasks[i]) != hideMasks[i]);
            hideList.setSelectedFlags(toShow);
            Display.getDisplay(kMain).setCurrent(hideList);
        }
        else if(c == hideOK)
        {
            Display.getDisplay(kMain).setCurrent(this);
            int hideMask = 0;
            boolean[] toShow = new boolean[hideList.size()];
            hideList.getSelectedFlags(toShow);
            for(int i = 0; i < toShow.length; i++)
            {
                if(!toShow[i]) hideMask |= hideMasks[i];
            }
            this.view.setHideMask(hideMask);
            this.scalingIsDirty = true;
            repaint();
        }
        else kMain.commandAction(c, s);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

