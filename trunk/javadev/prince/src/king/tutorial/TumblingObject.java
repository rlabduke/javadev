package king.tutorial;

import king.core.*;
import king.points.*;
import king.painters.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
/**
* <code>TumblingObject</code> creates a decorative spiral ornament and sets it rotating.
*
* <p>Copyright (C) 2006 by Ian W. Davis. All rights reserved.
* <br>Begun on Fri Dec  8 13:27:16 EST 2006
*/
public class TumblingObject extends JApplet implements ActionListener
{
    Kinemage            kin     = null;
    KView               view    = null;
    Engine2D            engine  = null;
    HighQualityPainter  painter = null;
    Timer               timer   = null;

    public void init()
    {
        super.init();
        kin = createKinemage();
        view = new KView(kin);
        view.setSpan(1.2f * view.getSpan());
        engine = new Engine2D();
        engine.usePerspective = true;
        painter = new HighQualityPainter(true);
        timer = new Timer(1000 / 30, this);
    }
    
    public void start()
    {
        super.start();
        timer.start();
    }
    
    public void stop()
    {
        timer.stop();
        super.stop();
    }
    
    public void destroy()
    {
        kin = null;
        view = null;
        engine = null;
        painter = null;
        super.destroy();
    }

    Kinemage createKinemage()
    {
        Kinemage k = new Kinemage();
        KGroup g = new KGroup();
        k.add(g);
        
        KPaint[] colors = { KPalette.red, KPalette.green, KPalette.gold,
            KPaint.createLightweightHSV("silver", 240, 3, 90, 240, 3, 10) };
        for(int c = 0; c < colors.length; c++)
        {
            KList list = new KList(KList.BALL);
            list.setColor(colors[c]);
            list.setRadius(0.05f);
            g.add(list);
            double offset = (2.0 * Math.PI * c) / colors.length;
            for(double y = -1; y <= 1; y += 0.05)
            {
                double r = 1 - Math.abs(y);
                double theta = (2.0 * Math.PI * y) + offset;
                BallPoint pt = new BallPoint("");
                pt.setXYZ(r * Math.cos(theta), y, r * Math.sin(theta));
                list.add(pt);
            }
        }
        
        return k;
    }

    public void actionPerformed(ActionEvent ev)
    {
        view.rotateY( (float) Math.toRadians(1.0) );
        this.repaint();
    }
    
    public void paint(Graphics g)
    {
        painter.setGraphics( (Graphics2D) g );
        engine.render(kin, view, this.getBounds(), painter);
    }
}//class

