import java.io.*;
import java.util.*;

/**
 * <code>ChemKin</code> converts simple text files to flat kinemages of chemical compounds.
 * Reads from stdin, writes to stdout.
 *
 * <p>File format, in brief. One element per line.
 * Both the current position and orientation (direction) are tracked and updated.
 * All commands take the name of the current point as an optional last parameter.
 *
 * <ul>
 * <li><code>#</code> <i>some string of text</i>
 *   <br>Allows for comments in the source file</li>
 * <li><code>new</code>
 *   <br>Starts a new <code>@group</code> and returns to the origin. Saved locations are not lost.</li>
 * <li><code>break</code>
 *   <br>Starts a new <code>@subgroup</code>.</li>
 * <li><code>snip</code>
 *   <br>Starts a new <code>@vectorlist</code>.</li>
 * <li><code>color</code> <i>magecolor</i>
 *   <br>Changes color and starts a new <code>@vectorlist</code>.</li>
 * <li><code>end</code> (also <code>quit</code>)
 *   <br>Terminates the drawing process. Only for interactive use.</li>
 * <li><code>move</code> <i>x</i> <i>y</i>
 *   <br>Moves to the absolute position (x, y)</li>
 * <li><code>draw</code> <i>x</i> <i>y</i>
 *   <br>Draws a line from the current position to the absolute position (x, y)</li>
 * <li><code>jump</code> <i>angle</i> <i>distance</i>
 *   <br>Turns <i>angle</i> degrees and moves <i>distance</i> units</li>
 * <li><code>bond</code> <i>angle</i> <i>distance</i>
 *   <br>Turns <i>angle</i> degrees and draws a bond <i>distance</i> units long</li>
 * <li><code>dashbond</code> (also <code>-bond</code>) <i>angle</i> <i>distance</i>
 *   <br>Turns <i>angle</i> degrees and draws a dashed bond <i>distance</i> units long</li>
 * <li><code>doublebond</code> <i>angle</i> <i>distance</i> <i>{</i><code>right</code><i>|</i><code>left</code><i>}</i>
 *   <br>Turns <i>angle</i> degrees and draws a double bond <i>distance</i> units long.
 *   Second bond is either on the right or the left.</li>
 * <li><code>dashdoublebond</code> (also <code>-doublebond</code>) <i>angle</i> <i>distance</i> <i>{</i><code>right</code><i>|</i><code>left</code><i>}</i>
 *   <br>Turns <i>angle</i> degrees and draws a one-and-a-half bond <i>distance</i> units long.
 *   Second bond is either on the right or the left.</li>
 * <li><code>circle</code> <i>radius</i>
 *   <br>Draws a circle of the specified radius at the current location.</li>
 * <li><code>arc</code> <i>radius</i> <i>start</i> <i>range</i> <i>[</i><code>arrow</code><i>]</i>
 *   <br>Draws part of a circle, with starting degree measures specified relative to current orientation.</li>
 * <li><code>push</code>
 *   <br>Saves the current location. Can be "nested".</li>
 * <li><code>pop</code>
 *   <br>Restores the current location. Can be "nested".</li>
 * </ul>
 *
<br><pre>
/----------------------------------------------------------------------\
| This program is free software; you can redistribute it and/or modify |
| it under the terms of the GNU General Public License as published by |
| the Free Software Foundation; either version 2 of the License, or    |
| (at your option) any later version.                                  |
|                                                                      |
| This program is distributed in the hope that it will be useful,      |
| but WITHOUT ANY WARRANTY; without even the implied warranty of       |
| MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        |
| GNU General Public License for more details.                         |
\----------------------------------------------------------------------/
</pre>
 *
 * <p>Begun on Mon Mar 25 23:22:16 EST 2002
 * <br>Copyright (C) 2002 by Ian W. Davis
 * <br>Richardson laboratory, Duke University: <a href="http://kinemage.biochem.duke.edu">kinemage.biochem.duke.edu</a>
 *
 */
public class ChemKin //extends ... implements ...
{
    public static void main(String[] args) { new ChemKin().Main(args); }

//##################################################################################################
    // Variables
    // go
    // here

    // current x, y, angle
    float x, y, a;

    // saved x, y, angle
    Stack sx, sy, sa;

    // source of input
    BufferedReader sys_in_reader;
    int linenumber;

    String color = "white";
    String pname = "C", gname = "molecule", sgname = "bond", vlname = "bond";

//##################################################################################################
    /**
    * Constructor
    */
    public ChemKin()
    {
        x = y = a = 0.0f;
        sx = new Stack();
        sy = new Stack();
        sa = new Stack();
        sys_in_reader = new BufferedReader(new InputStreamReader(System.in));
    }

//##################################################################################################
    /**
    * Main() function for running as an application
    */
    public void Main(String[] args)
    {
        parseArguments(args);

        boolean done = false;
        String currline;
        StringTokenizer tok;

        float cx, cy, dist, angle, radius, start, range;
        String cmd = "", s1;
        boolean b1;

        output("@text\nCreated by ChemKin\n@kinemage 1\n@flat");
        cmdNew();

        for(linenumber = 1; (currline = input()) != null && !done; linenumber++)
        {
            try
            {
                currline = currline.toLowerCase().trim();
                tok = new StringTokenizer(currline, " ,\t");
                if(currline.startsWith("#") || currline.equals("")) { /*comment*/ }
                else
                {
                    cmd = tok.nextToken();
                    if(cmd.equals("new"))
                    {
                        if(tok.hasMoreTokens()) gname = tok.nextToken(); // name the point (optional)
                        cmdNew();
                    }
                    else if(cmd.equals("break"))
                    {
                        if(tok.hasMoreTokens()) sgname = tok.nextToken(); // name the point (optional)
                        cmdBreak();
                    }
                    else if(cmd.equals("snip"))
                    {
                        if(tok.hasMoreTokens()) vlname = tok.nextToken(); // name the point (optional)
                        cmdSnip();
                    }
                    else if(cmd.equals("color"))
                    {
                        s1 = tok.nextToken();
                        if(tok.hasMoreTokens()) vlname = tok.nextToken(); // name the point (optional)
                        cmdColor(s1);
                    }
                    else if(cmd.equals("quit") || cmd.equals("end"))
                    {
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdEnd();
                        done = true;
                    }
                    else if(cmd.equals("move"))
                    {
                        cx = Float.parseFloat(tok.nextToken());
                        cy = Float.parseFloat(tok.nextToken());
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdMove(cx, cy);
                    }
                    else if(cmd.equals("draw"))
                    {
                        cx = Float.parseFloat(tok.nextToken());
                        cy = Float.parseFloat(tok.nextToken());
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdDraw(cx, cy);
                    }
                    else if(cmd.equals("push"))
                    {
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdPush();
                    }
                    else if(cmd.equals("pop"))
                    {
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdPop();
                    }
                    else if(cmd.equals("bond"))
                    {
                        angle = Float.parseFloat(tok.nextToken());
                        dist = Float.parseFloat(tok.nextToken());
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdBond(angle, dist);
                    }
                    else if(cmd.equals("jump"))
                    {
                        angle = Float.parseFloat(tok.nextToken());
                        dist = Float.parseFloat(tok.nextToken());
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdJump(angle, dist);
                    }
                    else if(cmd.equals("doublebond"))
                    {
                        angle = Float.parseFloat(tok.nextToken());
                        dist = Float.parseFloat(tok.nextToken());
                        b1 = tok.hasMoreTokens() && tok.nextToken().startsWith("r");
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdDoubleBond(angle, dist, b1);
                    }
                    else if(cmd.equals("-bond") || cmd.equals("dashbond"))
                    {
                        angle = Float.parseFloat(tok.nextToken());
                        dist = Float.parseFloat(tok.nextToken());
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdDashedBond(angle, dist);
                    }
                    else if(cmd.equals("-doublebond") || cmd.equals("dashdoublebond"))
                    {
                        angle = Float.parseFloat(tok.nextToken());
                        dist = Float.parseFloat(tok.nextToken());
                        b1 = tok.hasMoreTokens() && tok.nextToken().startsWith("r");
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdDashedDoubleBond(angle, dist, b1);
                    }
                    else if(cmd.equals("circle"))
                    {
                        radius = Float.parseFloat(tok.nextToken());
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdCircle(radius);
                    }
                    else if(cmd.equals("arc"))
                    {
                        radius = Float.parseFloat(tok.nextToken());
                        start  = Float.parseFloat(tok.nextToken());
                        range  = Float.parseFloat(tok.nextToken());
                        b1 = tok.hasMoreTokens() && tok.nextToken().startsWith("arrow");
                        if(tok.hasMoreTokens()) pname = tok.nextToken(); // name the point (optional)
                        cmdArc(radius, start, range, b1);
                    }
                    else
                    {
                        echo("Error: unrecognized command on line "+linenumber);
                    }
                }//else
            }//try
            catch(NumberFormatException ex)  { echo("Error: misformatted number on line "+linenumber); }
            catch(NoSuchElementException ex) { echo("Error: not enough parameters for '"+cmd+"' on line "+linenumber); }
        }//for
    }

//##################################################################################################
    // Interpret command-line arguments
    void parseArguments(String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            // consumed by another option
            if(args[i] == null) {}
            // this is an option
            else if(args[i].startsWith("-"))
            {
                if(args[i].equals("-h") || args[i].equals("-help")) {
                    echo("ChemKin: makes 2D chemical drawings as kinemages");
                    echo("(see JavaDoc pages for language description)");
                    System.exit(0);
                } else {
                    echo("*** Unrecognized option: "+args[i]);
                }
            }
            // this is a file, etc.
            else
            {
            }
        }
    }

//##################################################################################################
    /**
    * Start a new group.
    */
    public void cmdNew()
    {
        output("@group {"+gname+"} dominant animate");
        cmdBreak();

        x = y = a = 0.0f;
        cmdMove(x, y);
    }

//##################################################################################################
    /**
    * Breaks the chain by starting a new subgroup
    */
    public void cmdBreak()
    {
        output("@subgroup {"+sgname+"}");
        cmdSnip();

        cmdMove(x, y);
    }

//##################################################################################################
    /**
    * Breaks the chain by starting a new list
    */
    public void cmdSnip()
    {
        output("@vectorlist {"+vlname+"} color= "+color);

        cmdMove(x, y);
    }

//##################################################################################################
    /**
    * Breaks the chain by starting a new list and changing color
    */
    public void cmdColor(String c)
    {
        color = c;
        cmdSnip();
    }

//##################################################################################################
    /**
    * Ends the structure. (No effect)
    */
    public void cmdEnd()
    {
    }

//##################################################################################################
    /**
    * Moves to an absolute location
    *
    * @param cx absolute x coordinate
    * @param cy absolute y coordinate
    */
    public void cmdMove(float cx, float cy)
    {
        x = cx;
        y = cy;
        output("{"+pname+"}P "+x+" "+y+" 0.0");
    }

//##################################################################################################
    /**
    * Draws a line from the present position to an absolute location
    *
    * @param cx absolute x coordinate
    * @param cy absolute y coordinate
    */
    public void cmdDraw(float cx, float cy)
    {
        x = cx;
        y = cy;
        output("{"+pname+"} "+x+" "+y+" 0.0");
    }

//##################################################################################################
    /**
    * Saves the current position and direction.
    */
    public void cmdPush()
    {
        sx.push(new Float(x));
        sy.push(new Float(y));
        sa.push(new Float(a));
    }

//##################################################################################################
    /**
    * Restores the previous position and direction
    */
    public void cmdPop()
    {
        try {
            x = ((Float)sx.pop()).floatValue();
            y = ((Float)sy.pop()).floatValue();
            a = ((Float)sa.pop()).floatValue();
        } catch(EmptyStackException ex) { echo("Error: no values to restore on line "+linenumber); }

        cmdMove(x, y);
    }

//##################################################################################################
    /**
    * Draws a single bond
    *
    * @param angle angle to turn to (relative)
    * @param dist distance to move (relative)
    */
    public void cmdBond(float angle, float dist)
    {
        a += angle;
        x += dist * (float)Math.cos( a * Math.PI/180.0 );
        y += dist * (float)Math.sin( a * Math.PI/180.0 );
        cmdDraw(x, y);
    }

//##################################################################################################
    /**
    * Moves without drawing a bond
    *
    * @param angle angle to turn to (relative)
    * @param dist distance to move (relative)
    */
    public void cmdJump(float angle, float dist)
    {
        a += angle;
        x += dist * (float)Math.cos( a * Math.PI/180.0 );
        y += dist * (float)Math.sin( a * Math.PI/180.0 );
        cmdMove(x, y);
    }

//##################################################################################################
    /**
    * Draws a double bond
    *
    * @param angle angle to turn to (relative)
    * @param dist distance to move (relative)
    * @param right if <code>true</code>, draws second bond to the right
    */
    public void cmdDoubleBond(float angle, float dist, boolean right)
    {
        float width = dist / 10.0f;
        float turn = (right ? -90.0f : 90.0f);

        cmdBond(angle, dist);
        cmdPush();
        cmdJump(turn, width);
        cmdJump(turn, width/2);
        cmdBond(0.0f, dist - width);
        cmdPop();
    }

//##################################################################################################
    /**
    * Draws a dashed bond
    * @param angle angle to turn to (relative)
    * @param dist distance to move (relative)
    */
    public void cmdDashedBond(float angle, float dist)
    {
        cmdBond(angle, 0.140f*dist);
        cmdJump(0.0f, 0.145f*dist);
        cmdBond(0.0f, 0.140f*dist);
        cmdJump(0.0f, 0.145f*dist);
        cmdBond(0.0f, 0.140f*dist);
        cmdJump(0.0f, 0.145f*dist);
        cmdBond(0.0f, 0.140f*dist);
    }

//##################################################################################################
    /**
    * Draws a half-dashed double bond
    *
    * @param angle angle to turn to (relative)
    * @param dist distance to move (relative)
    * @param right if <code>true</code>, draws second bond to the right
    */
    public void cmdDashedDoubleBond(float angle, float dist, boolean right)
    {
        float width = dist / 10.0f;
        float turn = (right ? -90.0f : 90.0f);

        cmdBond(angle, dist);
        cmdPush();
        cmdJump(turn, width);
        cmdJump(turn, width/2);
        cmdDashedBond(0.0f, dist - width);
        cmdPop();
    }

//##################################################################################################
//##################################################################################################
    /**
    * Draws a circle at the current location
    *
    * @param radius radius of circle
    */
    public void cmdCircle(float radius)
    {
        cmdPush();

        boolean first = true;
        float startx = x, starty = y;

        for(double angle = 0.0; angle <= 360.0; angle += 5.0)
        {
            x = startx + radius * (float)Math.cos( angle * Math.PI/180.0 );
            y = starty + radius * (float)Math.sin( angle * Math.PI/180.0 );

            if(first)
            {
                cmdMove(x, y);
                first = false;
            }
            else
            {
                cmdDraw(x, y);
            }
        }

        cmdPop();
    }

//##################################################################################################
    /**
    * Draws an arc from a circle at the current location.
    *
    * @param radius radius of circle
    * @param start starting point for arc (degrees, relative)
    * @param end ending point for arc (degrees, relative)
    * @param cw if <code>true</code>, draw clockwise instead of counter-clockwise
    * @param arrowhead if <code>true</code>, put a double-headed arrow at end of arc
    */
    public void cmdArc(float radius, float start, float range, boolean arrowhead)
    {
        cmdPush();

        double step = 5.0;
        boolean cw = (range < 0.0f);
        if(cw) step = -step;
        start = a + start;
        float end = start + range;

        boolean first = true;
        float startx = x, starty = y;
        double angle;

        for(angle = start; (cw ? angle >= end : angle <= end); angle += step)
        {
            x = startx + radius * (float)Math.cos( angle * Math.PI/180.0 );
            y = starty + radius * (float)Math.sin( angle * Math.PI/180.0 );

            if(first)
            {
                cmdMove(x, y);
                first = false;
            }
            else
            {
                cmdDraw(x, y);
            }
        }

        if(arrowhead)
        {
            angle += -3*step;

            cmdPush();
            x = startx + 1.1f * radius * (float)Math.cos( angle * Math.PI/180.0 );
            y = starty + 1.1f * radius * (float)Math.sin( angle * Math.PI/180.0 );
            cmdDraw(x, y);

            cmdPop();
            x = startx + 0.9f * radius * (float)Math.cos( angle * Math.PI/180.0 );
            y = starty + 0.9f * radius * (float)Math.sin( angle * Math.PI/180.0 );
            cmdDraw(x, y);
        }

        cmdPop();
    }

//##################################################################################################
    // Convenience function for debugging
    void echo(String s) { System.err.println(s); }
    // Convenience function
    void output(String s) { System.out.println(s); }
    // Convenience function
    String input()
    {
        try {
            return sys_in_reader.readLine();
        } catch(IOException ex) { return null; }
    }

}//class
