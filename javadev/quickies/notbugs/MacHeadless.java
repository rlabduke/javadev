import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
* <code>MacHeadless</code> demonstrates that headless support is broken in 1.4.2_03
* Only it doesn't -- I can't reproduce this weird error.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Mon Feb 23 09:51:15 EST 2004
*/
public class MacHeadless //extends ... implements ...
{
    public static void main(String[] args) throws IOException
    {
        // If we fail to do this, Java will crash
        // if this program is run from a non-graphical
        // (e.g. script) environment.
            System.err.println("=== Checkpoint #1 ===");
        System.setProperty("java.awt.headless", "true");
        
            System.err.println("=== Checkpoint #2 ===");
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
            System.err.println("=== Checkpoint #3 ===");
        Graphics2D g2 = img.createGraphics();
            System.err.println("=== Checkpoint #4 ===");
        g2.setFont(new Font("Serif", Font.PLAIN, 12));
            System.err.println("=== Checkpoint #5 ===");
        g2.drawString("I'm gonna get you sucka", 50, 50); // Kills it
            System.err.println("=== Checkpoint #6 - you'll never see this one! ===");
        ImageIO.write(img, "jpg", System.out);
    }
}//class

