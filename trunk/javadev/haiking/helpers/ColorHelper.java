// (jEdit options) :folding=explicit:collapseFolds=1:
/**
* <code>ColorHelper</code> has not yet been documented.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Feb  2 16:52:11 EST 2005
*/
public class ColorHelper //extends ... implements ...
{
    /** The number of different depth-cueing levels available */
    public static final int COLOR_LEVELS = 16;
    
    /**
    * The minimum value multiplier on a black background.
    * Smaller numbers mean we fade closer to black before clipping.
    */
    //static final float      BVAL            = 0.36f;
    static final float      BVAL            = 0.20f;
    
    /** The minimum value multiplier on a white background. */
    static final float      WVAL            = 0.40f;
    /** The minimum saturation multiplier on a white background. */
    static final float      WSAT            = 0.36f;

    static public void main(String[] args)
    {
        // Create standard entries
        //                                                          name        hue     bSat    wSat    bVal    wVal
        //int[] defaultColor = createHSV("default",   0,      0,      0,      100,    0);
        int[] red          = createHSV("red",       0,      100,    100,    100,    80);
        int[] orange       = createHSV("orange",    20,     100,    100,    100,    90);
        //int[] rust         = createHSV("rust",      20,     100,    100,    100,    90);
        int[] gold         = createHSV("gold",      40,     100,    100,    100,    90);
        int[] yellow       = createHSV("yellow",    60,     100,    100,    100,    90);
        int[] lime         = createHSV("lime",      80,     100,    100,    100,    85);
        int[] green        = createHSV("green",     120,    80,     90,     100,    75);
        int[] sea          = createHSV("sea",       150,    100,    100,    100,    85);
        //int[] seagreen     = createHSV("seagreen",  150,    100,    100,    100,    85);
        int[] cyan         = createHSV("cyan",      180,    100,    85,     85,     80);
        int[] sky          = createHSV("sky",       210,    75,     80,     95,     90);
        //int[] skyblue      = createHSV("skyblue",   210,    75,     80,     95,     90);
        int[] blue         = createHSV("blue",      240,    70,     80,     100,    100);
        int[] purple       = createHSV("purple",    275,    75,     100,    100,    85);
        int[] magenta      = createHSV("magenta",   300,    95,     100,    100,    90);
        int[] hotpink      = createHSV("hotpink",   335,    100,    100,    100,    90);
        int[] pink         = createHSV("pink",      350,    55,     75,     100,    90);
        int[] peach        = createHSV("peach",     25,     75,     75,     100,    90);
        int[] lilac        = createHSV("lilac",     275,    55,     75,     100,    80);
        int[] pinktint     = createHSV("pinktint",  340,    30,     100,    100,    55);
        int[] peachtint    = createHSV("peachtint", 25,     50,     100,    100,    60);
        int[] yellowtint   = createHSV("yellowtint",60,     50,     100,    100,    75);
        //int[] paleyellow   = createHSV("paleyellow",60,     50,     100,    100,    75);
        int[] greentint    = createHSV("greentint", 135,    40,     100,    100,    35);
        int[] bluetint     = createHSV("bluetint",  220,    40,     100,    100,    50);
        int[] lilactint    = createHSV("lilactint", 275,    35,     100,    100,    45);
        int[] white        = createHSV("white",     0,      0,      0,      100,    0);
        int[] gray         = createHSV("gray",      0,      0,      0,      50,     40);
        //int[] grey         = createHSV("grey",      0,      0,      0,      50,     40);
        int[] brown        = createHSV("brown",     20,     45,     45,     75,     55);
        
        //int[] deadwhite    = createSolid("deadwhite", Color.white);
        //int[] deadblack    = createSolid("deadblack", Color.black);
        //int[] black        = createSolid("black", Color.black);
        //int[] invisible    = createInvisible("invisible");
        
        //int[] defaultColor = white;
    }
    
    /**
    * Creates a new color definition based on hue (0-360), saturation (0-100),
    * and relative value (0-100; usually 75-100).
    */
    static public int[] createHSV(String name, float hue, float blackSat, float whiteSat, float blackVal, float whiteVal)
    {
        if(name == null)
            throw new NullPointerException("Must give paint a name");
        
        hue         /= 360f;
        blackSat    /= 100f;
        whiteSat    /= 100f;
        blackVal    /= 100f;
        whiteVal    /= 100f;
        
        // value decreases going back
        int[] bcolors = new int[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
        {
            bcolors[i] = getHSB(hue, blackSat,
                //( 0.36f + 0.64f*i/(COLOR_LEVELS-1) )*blackVal );
                ( BVAL + (1-BVAL)*i/(COLOR_LEVELS-1) )*blackVal );
        }
        
        // value increases, saturation decreases going back
        /*int[] wcolors = new int[COLOR_LEVELS];
        for(int i = 0; i < COLOR_LEVELS; i++)
        {
            wcolors[i] = getHSB(hue,
                //( 0.36f + 0.64f*i/(COLOR_LEVELS-1) )*whiteSat,
                //Math.min(1f, 1f + (whiteVal-1f)*( 0.40f + 0.60f*i/(COLOR_LEVELS-1) )) ); 
                ( WSAT + (1-WSAT)*i/(COLOR_LEVELS-1) )*whiteSat,
                Math.min(1f, 1f + (whiteVal-1f)*( WVAL + (1-WVAL)*i/(COLOR_LEVELS-1) )) ); 
        }*/
        
        System.out.print("    static final int[] "+name+" = { ");
        for(int i = 0; i < COLOR_LEVELS; i++)
        {
            if(i != 0) System.out.print(", ");
            System.out.print("0x"+Integer.toHexString(bcolors[i]));
        }
        System.out.println(" };");
        
        return bcolors;
    }
    
    static private int getHSB(float hue, float sat, float val)
    { return java.awt.Color.HSBtoRGB(hue, sat, val) & 0xffffff; }
}//class

