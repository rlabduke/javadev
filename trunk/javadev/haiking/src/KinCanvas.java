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
    static final int[] red = { 0x300000, 0x400000, 0x500000, 0x600000, 0x680000, 0x780000, 0x880000, 0x900000, 0xa00000, 0xb00000, 0xb80000, 0xc80000, 0xd80000, 0xe80000, 0xf00000, 0xff0000 };
    static final int[] orange = { 0x301000, 0x401800, 0x501800, 0x602000, 0x682000, 0x782800, 0x883000, 0x903000, 0xa03800, 0xb03800, 0xb84000, 0xc84000, 0xd84800, 0xe85000, 0xf05000, 0xff5800 };
    static final int[] gold = { 0x302000, 0x402800, 0x503800, 0x604000, 0x684800, 0x785000, 0x885800, 0x906000, 0xa06800, 0xb07800, 0xb88000, 0xc88800, 0xd89000, 0xe89800, 0xf0a000, 0xffa800 };
    static final int[] yellow = { 0x303000, 0x404000, 0x505000, 0x606000, 0x686800, 0x787800, 0x888800, 0x909000, 0xa0a000, 0xb0b000, 0xb8b800, 0xc8c800, 0xd8d800, 0xe8e800, 0xf0f000, 0xffff00 };
    static final int[] lime = { 0x203000, 0x284000, 0x385000, 0x406000, 0x486800, 0x507800, 0x588800, 0x609000, 0x68a000, 0x78b000, 0x80b800, 0x88c800, 0x90d800, 0x98e800, 0xa0f000, 0xa8ff00 };
    static final int[] green = { 0x83008, 0x104010, 0x105010, 0x106010, 0x186818, 0x187818, 0x188818, 0x209020, 0x20a020, 0x20b020, 0x28b828, 0x28c828, 0x28d828, 0x30e830, 0x30f030, 0x30ff30 };
    static final int[] sea = { 0x3018, 0x4020, 0x5028, 0x6030, 0x6838, 0x7840, 0x8840, 0x9048, 0xa050, 0xb058, 0xb860, 0xc868, 0xd868, 0xe870, 0xf078, 0xff80 };
    static final int[] cyan = { 0x2828, 0x3838, 0x4040, 0x5050, 0x5858, 0x6868, 0x7070, 0x8080, 0x8888, 0x9090, 0xa0a0, 0xa8a8, 0xb8b8, 0xc0c0, 0xd0d0, 0xd8d8 };
    static final int[] sky = { 0x102030, 0x102840, 0x103048, 0x183858, 0x184068, 0x204870, 0x205080, 0x205888, 0x286098, 0x2868a8, 0x3070b0, 0x3078c0, 0x3080c8, 0x3888d8, 0x3890e8, 0x4098f0 };
    static final int[] blue = { 0x101030, 0x101040, 0x181850, 0x202060, 0x202068, 0x282878, 0x282888, 0x303090, 0x3030a0, 0x3838b0, 0x3838b8, 0x4040c8, 0x4040d8, 0x4848e8, 0x4848f0, 0x5050ff };
    static final int[] purple = { 0x201030, 0x301040, 0x381850, 0x401860, 0x481868, 0x502078, 0x582088, 0x682890, 0x7028a0, 0x7828b0, 0x8030b8, 0x8830c8, 0x9038d8, 0xa038e8, 0xa840f0, 0xb040ff };
    static final int[] magenta = { 0x300030, 0x400040, 0x500850, 0x600860, 0x680868, 0x780878, 0x880888, 0x900890, 0xa008a0, 0xb008b0, 0xb808b8, 0xc808c8, 0xd808d8, 0xe808e8, 0xf010f0, 0xff10ff };
    static final int[] hotpink = { 0x300018, 0x400018, 0x500020, 0x600028, 0x680030, 0x780030, 0x880038, 0x900040, 0xa00040, 0xb00048, 0xb80050, 0xc80058, 0xd80058, 0xe80060, 0xf00068, 0xff0068 };
    static final int[] pink = { 0x301820, 0x402020, 0x502028, 0x602830, 0x683038, 0x783840, 0x884048, 0x904050, 0xa04858, 0xb05060, 0xb85868, 0xc85870, 0xd86078, 0xe86878, 0xf07080, 0xff7088 };
    static final int[] peach = { 0x302010, 0x402810, 0x503018, 0x603818, 0x683818, 0x784020, 0x884820, 0x905028, 0xa05828, 0xb06028, 0xb86830, 0xc87030, 0xd87838, 0xe88038, 0xf08840, 0xff9040 };
    static final int[] lilac = { 0x281830, 0x302040, 0x402050, 0x482860, 0x503068, 0x603878, 0x684088, 0x704090, 0x7848a0, 0x8850b0, 0x9058b8, 0x9858c8, 0xa860d8, 0xb068e8, 0xb870f0, 0xc870ff };
    static final int[] pinktint = { 0x302828, 0x403038, 0x503840, 0x604048, 0x684858, 0x785060, 0x886068, 0x906878, 0xa07080, 0xb07888, 0xb88098, 0xc890a0, 0xd898a8, 0xe8a0b8, 0xf0a8c0, 0xffb0d0 };
    static final int[] peachtint = { 0x302818, 0x403020, 0x503828, 0x604030, 0x684838, 0x785840, 0x886040, 0x906848, 0xa07050, 0xb07858, 0xb88860, 0xc89068, 0xd89868, 0xe8a070, 0xf0a878, 0xffb880 };
    static final int[] yellowtint = { 0x303018, 0x404020, 0x505028, 0x606030, 0x686838, 0x787840, 0x888840, 0x909048, 0xa0a050, 0xb0b058, 0xb8b860, 0xc8c868, 0xd8d868, 0xe8e870, 0xf0f078, 0xffff80 };
    static final int[] greentint = { 0x203028, 0x284030, 0x305038, 0x386040, 0x406848, 0x487850, 0x508860, 0x589068, 0x60a070, 0x68b078, 0x70b880, 0x78c890, 0x80d898, 0x88e8a0, 0x90f0a8, 0x98ffb0 };
    static final int[] bluetint = { 0x202830, 0x283040, 0x303850, 0x384060, 0x405068, 0x485878, 0x506088, 0x586890, 0x6078a0, 0x6880b0, 0x7088b8, 0x7890c8, 0x80a0d8, 0x88a8e8, 0x90b0f0, 0x98b8ff };
    static final int[] lilactint = { 0x302030, 0x382840, 0x403050, 0x504060, 0x584868, 0x685078, 0x705888, 0x806090, 0x8868a0, 0x9870b0, 0xa078b8, 0xa880c8, 0xb888d8, 0xc098e8, 0xd0a0f0, 0xd8a8ff };
    static final int[] white = { 0x303030, 0x404040, 0x505050, 0x606060, 0x686868, 0x787878, 0x888888, 0x909090, 0xa0a0a0, 0xb0b0b0, 0xb8b8b8, 0xc8c8c8, 0xd8d8d8, 0xe8e8e8, 0xf0f0f0, 0xffffff };
    static final int[] gray = { 0x181818, 0x202020, 0x282828, 0x303030, 0x383838, 0x404040, 0x404040, 0x484848, 0x505050, 0x585858, 0x606060, 0x686868, 0x686868, 0x707070, 0x787878, 0x808080 };
    static final int[] brown = { 0x281818, 0x302018, 0x382820, 0x483028, 0x503828, 0x584030, 0x604838, 0x705040, 0x785840, 0x805848, 0x906050, 0x986850, 0xa07058, 0xa87860, 0xb88068, 0xc08868 };

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
    Command         cmdShowHide;
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
    Vector          groupList;
    Command         hideOK;
    List            hideList;
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
        cmdShowHide = new Command("Show/hide pts", Command.SCREEN, 3);
        this.addCommand(cmdShowHide);
        cmdTogglePersp = new Command("Perspective", Command.SCREEN, 3);
        this.addCommand(cmdTogglePersp);
        cmdDoubleBuffer = new Command("Dbl buffer", Command.SCREEN, 3);
        this.addCommand(cmdDoubleBuffer);
        
        this.clearKinemage();
    }
//}}}

//{{{ loadKinemage, clearKinemage
//##############################################################################
    /** Sets a new kinemage for display */
    public void loadKinemage(KPoint tailPt, View view, Vector groupList)
    {
        this.tailPt     = tailPt;
        this.view       = view;
        this.groupList  = groupList;
        this.scalingIsDirty = true;
        
        this.zbuf = new KPoint[ZBUF_SIZE];
        
        KGroup.processGroups(groupList, tailPt);

        this.hideList = KGroup.makeList(this.groupList, "Show/hide");
        hideList.setCommandListener(this);
        hideOK = new Command("OK", Command.OK, 1);
        hideList.addCommand(hideOK);
    }
    
    /** Replaces the current kin with a single point and frees the memory. */
    public void clearKinemage()
    {
        // First allow all this to be gc'd
        this.zbuf = null;
        this.tailPt = this.drawPt = null;
        this.view = null;
        this.groupList = null;
        KPoint.pointIDs.clear();

        // Now make sure we don't get NPEs
        KPoint p = new KPoint(0, 0, 0, KPoint.TYPE_LABEL);
        p.setPointID("No kin loaded");
        loadKinemage(p, new View(), new Vector());
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
        else if(c == cmdShowHide)
        {
            KGroup.toChoice(groupList, hideList);
            Display.getDisplay(kMain).setCurrent(hideList);
        }
        else if(c == hideOK)
        {
            Display.getDisplay(kMain).setCurrent(this);
            KGroup.fromChoice(groupList, hideList);
            KGroup.processGroups(groupList, tailPt);
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

