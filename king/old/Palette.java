// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
 * <code>Palette</code> encodes flexible color palettes for kinemage rendering.
 * This class replaces MagePaints, but not in a compatible way.
 * It allows for the (future) possibility of kinemages defining new colors, and of re-defining standard colors.
 *
 * <p>HSV inline color definitions are handled in Kinemage.java, but
 * here are the HSV-style definintions of the classic kinemage colors:
 * <ul>
 * <li>red: 0 100 1.0</li>
 * <li>orange/rust: 20 100 1.0</li>
 * <li>gold:40 100 1.0</li>
 * <li>yellow: 60 100 1.0</li>
 * <li>lime: 90 100 1.0</li>
 * <li>green: 120 100 1.0</li>
 * <li>sea/seagreen: 150 100 1.0</li>
 * <li>cyan: 180 100 0.75 (orig), 180 100 0.85 (new)</li>
 * <li>sky/skyblue: 215 75 1.0 (orig), 210 75 1.0 (new)</li>
 * <li>blue: 240 75 1.0</li>
 * <li>purple: 270 75 1.0</li>
 * <li>magenta: 300 100 1.0</li>
 * <li>hotpink: 335 100 1.0</li>
 * <li>pink:350 55 1.0</li>
 * <li>peach:25 50 1.0</li>
 * <li>lilac: 275 55 0.95</li>
 * <li>pinktint: 340 30 1.0</li>
 * <li>peachtint: 25 50 1.0</li>
 * <li>yellowtint/paleyellow: 60 50 1.0</li>
 * <li>greentint: 135 40 1.0</li>
 * <li>bluetint: 220 35 1.0</li>
 * <li>lilactint: 275 30 1.0</li>
 * <li>white: 0 0 1.0</li>
 * <li>gray/grey: 0 0 0.5</li>
 * <li>brown: 20 50 0.75</li>
 * </ul>
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Wed Sep 18 08:54:44 EDT 2002
*/
public class Palette //extends ... implements ...
{
//{{{ Constants & pens
    public static final int COLOR_LEVELS = 5;
    
    public static BasicStroke     pen0 = new BasicStroke(0, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    public static BasicStroke[][] pens = new BasicStroke[7][5];
    static
    {
        for(int i = 0; i < 7; i++)
        {
            for(int j = 0; j < 5; j++)
            {
                pens[i][j] = new BasicStroke((i+1)*KPoint.widthScale[j], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            }
        }
    }
//}}}

//{{{ Mage palette
//##################################################################################################
    /**
    * Initialize the table of paint colors using Dave's scheme and color values.
    * 0 is dead black, 255 is dead white.
    * Normal colors are in 10 blocks of 25 (1-25, 26-50, etc)
    * The blocks go from dim (back of the z-buffer) to bright (front of the z-buffer)
    * and alternate black background, white background.
    */
    static Color[] magepaints = new Color[256];
    
    static
    {//setcolorpalette
        magepaints[  0] = new Color(   0,   0,   0);//deadblack index== 0 
        magepaints[  1] = new Color(  93,   0,   0);//red        0 B
        magepaints[  2] = new Color(  89,  27,   0);//orange     0 B
        magepaints[  3] = new Color(  95,  55,   0);//gold       0 B
        magepaints[  4] = new Color(  82,  78,   0);//yellow     0 B
        magepaints[  5] = new Color(  44,  82,   0);//lime       0 B
        magepaints[  6] = new Color(   7,  85,   0);//green      0 B
        magepaints[  7] = new Color(   0,  82,  31);//sea        0 B
        magepaints[  8] = new Color(   0,  78,  69);//cyan       0 B
        magepaints[  9] = new Color(  20,  48,  85);//sky        0 B
        magepaints[ 10] = new Color(  31,  31,  89);//blue       0 B
        magepaints[ 11] = new Color(  50,  23,  93);//purple     0 B
        magepaints[ 12] = new Color(  82,   0,  74);//magenta    0 B
        magepaints[ 13] = new Color(  89,   0,  39);//hotpink    0 B
        magepaints[ 14] = new Color( 115,  50,  60);//pink       0 B
        magepaints[ 15] = new Color(  85,  45, 100);//lilac      0 B
        magepaints[ 16] = new Color(  95,  45,  30);//peach      0 B
        magepaints[ 17] = new Color( 105,  65,  45);//peachtint  0 B
        magepaints[ 18] = new Color(  85,  85,  45);//yellowtint 0 B
        magepaints[ 19] = new Color(  54,  89,  66);//greentint  0 B
        magepaints[ 20] = new Color(  60,  78,  89);//bluetint   0 B
        magepaints[ 21] = new Color(  80,  70, 100);//lilactint  0 B
        magepaints[ 22] = new Color( 115,  80,  90);//pinktint   0 B
        magepaints[ 23] = new Color(  74,  74,  74);//white      0 B
        magepaints[ 24] = new Color(  35,  35,  35);//gray       0 B
        magepaints[ 25] = new Color(  70,  46,  35);//brown      0 B
        magepaints[ 26] = new Color( 255, 160, 160);//red        0 W
        magepaints[ 27] = new Color( 250, 180, 120);//orange     0 W
        magepaints[ 28] = new Color( 230, 175, 160);//gold       0 W
        magepaints[ 29] = new Color( 240, 230, 200);//yellow     0 W
        magepaints[ 30] = new Color( 170, 210, 160);//lime       0 W
        magepaints[ 31] = new Color( 187, 218, 171);//green      0 W
        magepaints[ 32] = new Color( 170, 240, 190);//sea        0 W
        magepaints[ 33] = new Color( 160, 215, 215);//cyan       0 W
        magepaints[ 34] = new Color( 117, 171, 214);//sky        0 W
        magepaints[ 35] = new Color( 160, 160, 255);//blue       0 W
        magepaints[ 36] = new Color( 179, 140, 214);//purple     0 W
        magepaints[ 37] = new Color( 230, 117, 210);//magenta    0 W
        magepaints[ 38] = new Color( 255, 120, 150);//hotpink    0 W
        magepaints[ 39] = new Color( 250, 160, 170);//pink       0 W
        magepaints[ 40] = new Color( 190, 140, 210);//lilac      0 W
        magepaints[ 41] = new Color( 195, 110,  80);//peach      0 W
        magepaints[ 42] = new Color( 195, 110,  80);//peachtint  0 W
        magepaints[ 43] = new Color( 170, 160, 120);//yellowtint 0 W
        magepaints[ 44] = new Color( 140, 210, 171);//greentint  0 W
        magepaints[ 45] = new Color( 135, 171, 210);//bluetint   0 W
        magepaints[ 46] = new Color( 185, 151, 210);//lilactint  0 W
        magepaints[ 47] = new Color( 200, 120, 120);//pinktint   0 W
        magepaints[ 48] = new Color( 156, 156, 156);//white      0 W
        magepaints[ 49] = new Color( 175, 175, 175);//gray       0 W
        magepaints[ 50] = new Color( 230, 170, 145);//brown      0 W
        magepaints[ 51] = new Color( 136,   0,   0);//red        1 B
        magepaints[ 52] = new Color( 132,  39,   0);//orange     1 B
        magepaints[ 53] = new Color( 135,  78,   0);//gold       1 B
        magepaints[ 54] = new Color( 121, 117,   0);//yellow     1 B
        magepaints[ 55] = new Color(  66, 123,   0);//lime       1 B
        magepaints[ 56] = new Color(  11, 128,   0);//green      1 B
        magepaints[ 57] = new Color(   0, 125,  50);//sea        1 B
        magepaints[ 58] = new Color(   0, 102,  92);//cyan       1 B
        magepaints[ 59] = new Color(  31,  68, 125);//sky        1 B
        magepaints[ 60] = new Color(  39,  39, 132);//blue       1 B
        magepaints[ 61] = new Color(  75,  27, 132);//purple     1 B
        magepaints[ 62] = new Color( 125,   0, 113);//magenta    1 B
        magepaints[ 63] = new Color( 128,   0,  54);//hotpink    1 B
        magepaints[ 64] = new Color( 150,  65,  78);//pink       1 B
        magepaints[ 65] = new Color( 110,  60, 140);//lilac      1 B
        magepaints[ 66] = new Color( 135,  70,  40);//peach      1 B
        magepaints[ 67] = new Color( 142,  95,  65);//peachtint  1 B
        magepaints[ 68] = new Color( 125, 125,  67);//yellowtint 1 B
        magepaints[ 69] = new Color(  85, 132,  97);//greentint  1 B
        magepaints[ 70] = new Color(  90, 113, 132);//bluetint   1 B
        magepaints[ 71] = new Color( 115,  95, 140);//lilactint  1 B
        magepaints[ 72] = new Color( 150, 105, 117);//pinktint   1 B
        magepaints[ 73] = new Color( 101, 101, 101);//white      1 B
        magepaints[ 74] = new Color(  46,  46,  46);//gray       1 B
        magepaints[ 75] = new Color(  89,  58,  42);//brown      1 B
        magepaints[ 76] = new Color( 255, 120, 120);//red        1 W
        magepaints[ 77] = new Color( 250, 160,  90);//orange     1 W
        magepaints[ 78] = new Color( 230, 175, 120);//gold       1 W
        magepaints[ 79] = new Color( 240, 230, 150);//yellow     1 W
        magepaints[ 80] = new Color( 170, 210, 120);//lime       1 W
        magepaints[ 81] = new Color( 144, 210, 128);//green      1 W
        magepaints[ 82] = new Color( 140, 230, 170);//sea        1 W
        magepaints[ 83] = new Color( 120, 208, 208);//cyan       1 W
        magepaints[ 84] = new Color(  93, 160, 214);//sky        1 W
        magepaints[ 85] = new Color( 120, 120, 255);//blue       1 W
        magepaints[ 86] = new Color( 164, 109, 214);//purple     1 W
        magepaints[ 87] = new Color( 226,  93, 210);//magenta    1 W
        magepaints[ 88] = new Color( 255,  90, 140);//hotpink    1 W
        magepaints[ 89] = new Color( 245, 150, 160);//pink       1 W
        magepaints[ 90] = new Color( 180, 130, 200);//lilac      1 W
        magepaints[ 91] = new Color( 195, 110,  60);//peach      1 W
        magepaints[ 92] = new Color( 195, 110,  60);//peachtint  1 W
        magepaints[ 93] = new Color( 170, 160,  90);//yellowtint 1 W
        magepaints[ 94] = new Color( 105, 179, 128);//greentint  1 W
        magepaints[ 95] = new Color( 100, 128, 179);//bluetint   1 W
        magepaints[ 96] = new Color( 160, 118, 180);//lilactint  1 W
        magepaints[ 97] = new Color( 190,  90,  90);//pinktint   1 W
        magepaints[ 98] = new Color( 117, 117, 117);//white      1 W
        magepaints[ 99] = new Color( 156, 156, 156);//gray       1 W
        magepaints[100] = new Color( 208, 152, 125);//brown      1 W
        magepaints[101] = new Color( 187,   0,   0);//red        2 B
        magepaints[102] = new Color( 179,  54,   0);//orange     2 B
        magepaints[103] = new Color( 175, 110,   0);//gold       2 B
        magepaints[104] = new Color( 171, 164,   0);//yellow     2 B
        magepaints[105] = new Color(  93, 171,   0);//lime       2 B
        magepaints[106] = new Color(  15, 179,   0);//green      2 B
        magepaints[107] = new Color(   0, 171,  66);//sea        2 B
        magepaints[108] = new Color(   0, 136, 123);//cyan       2 B
        magepaints[109] = new Color(  42,  92, 167);//sky        2 B
        magepaints[110] = new Color(  46,  46, 183);//blue       2 B
        magepaints[111] = new Color( 100,  35, 175);//purple     2 B
        magepaints[112] = new Color( 171,   0, 160);//magenta    2 B
        magepaints[113] = new Color( 175,   0,  70);//hotpink    2 B
        magepaints[114] = new Color( 185,  80,  95);//pink       2 B
        magepaints[115] = new Color( 135,  75, 180);//lilac      2 B
        magepaints[116] = new Color( 175,  95,  50);//peach      2 B
        magepaints[117] = new Color( 180, 125,  85);//peachtint  2 B
        magepaints[118] = new Color( 168, 168,  90);//yellowtint 2 B
        magepaints[119] = new Color( 117, 187, 140);//greentint  2 B
        magepaints[120] = new Color( 120, 140, 183);//bluetint   2 B
        magepaints[121] = new Color( 158, 120, 180);//lilactint  2 B
        magepaints[122] = new Color( 185, 130, 145);//pinktint   2 B
        magepaints[123] = new Color( 164, 164, 164);//white      2 B
        magepaints[124] = new Color(  66,  66,  66);//gray       2 B
        magepaints[125] = new Color( 117,  74,  58);//brown      2 B
        magepaints[126] = new Color( 255,  80,  80);//red        2 W
        magepaints[127] = new Color( 250, 140,  60);//orange     2 W
        magepaints[128] = new Color( 230, 175,  80);//gold       2 W
        magepaints[129] = new Color( 240, 230, 100);//yellow     2 W
        magepaints[130] = new Color( 170, 210,  80);//lime       2 W
        magepaints[131] = new Color( 101, 203,  85);//green      2 W
        magepaints[132] = new Color( 110, 220, 150);//sea        2 W
        magepaints[133] = new Color(  80, 200, 200);//cyan       2 W
        magepaints[134] = new Color(  70, 148, 214);//sky        2 W
        magepaints[135] = new Color(  80,  80, 255);//blue       2 W
        magepaints[136] = new Color( 148,  82, 214);//purple     2 W
        magepaints[137] = new Color( 222,  70, 207);//magenta    2 W
        magepaints[138] = new Color( 255,  60, 130);//hotpink    2 W
        magepaints[139] = new Color( 240, 140, 150);//pink       2 W
        magepaints[140] = new Color( 170, 120, 190);//lilac      2 W
        magepaints[141] = new Color( 195, 110,  40);//peach      2 W
        magepaints[142] = new Color( 195, 110,  40);//peachtint  2 W
        magepaints[143] = new Color( 170, 160,  60);//yellowtint 2 W
        magepaints[144] = new Color(  70, 148,  85);//greentint  2 W
        magepaints[145] = new Color(  75,  85, 148);//bluetint   2 W
        magepaints[146] = new Color( 135,  85, 150);//lilactint  2 W
        magepaints[147] = new Color( 180,  60,  60);//pinktint   2 W
        magepaints[148] = new Color(  78,  78,  78);//white      2 W
        magepaints[149] = new Color( 136, 136, 136);//gray       2 W
        magepaints[150] = new Color( 185, 133, 106);//brown      2 W
        magepaints[151] = new Color( 230,   0,   0);//red        3 B
        magepaints[152] = new Color( 226,  74,   0);//orange     3 B
        magepaints[153] = new Color( 215, 137,   0);//gold       3 B
        magepaints[154] = new Color( 218, 210,   0);//yellow     3 B
        magepaints[155] = new Color( 117, 216,   0);//lime       3 B
        magepaints[156] = new Color(  19, 222,   0);//green      3 B
        magepaints[157] = new Color(   0, 214,  85);//sea        3 B
        magepaints[158] = new Color(   0, 171, 154);//cyan       3 B
        magepaints[159] = new Color(  54, 120, 222);//sky        3 B
        magepaints[160] = new Color(  54,  54, 226);//blue       3 B
        magepaints[161] = new Color( 125,  42, 222);//purple     3 B
        magepaints[162] = new Color( 218,   0, 203);//magenta    3 B
        magepaints[163] = new Color( 222,   0,  85);//hotpink    3 B
        magepaints[164] = new Color( 220,  95, 112);//pink       3 B
        magepaints[165] = new Color( 160,  90, 220);//lilac      3 B
        magepaints[166] = new Color( 215, 120,  60);//peach      3 B
        magepaints[167] = new Color( 217, 155, 105);//peachtint  3 B
        magepaints[168] = new Color( 210, 210, 112);//yellowtint 3 B
        magepaints[169] = new Color( 140, 230, 164);//greentint  3 B
        magepaints[170] = new Color( 142, 162, 226);//bluetint   3 B
        magepaints[171] = new Color( 185, 145, 220);//lilactint  3 B
        magepaints[172] = new Color( 220, 155, 172);//pinktint   3 B
        magepaints[173] = new Color( 214, 214, 214);//white      3 B
        magepaints[174] = new Color(  93,  93,  93);//gray       3 B
        magepaints[175] = new Color( 140,  93,  70);//brown      3 B
        magepaints[176] = new Color( 255,  40,  40);//red        3 W
        magepaints[177] = new Color( 250, 120,  30);//orange     3 W
        magepaints[178] = new Color( 230, 175,  40);//gold       3 W
        magepaints[179] = new Color( 240, 230,  50);//yellow     3 W
        magepaints[180] = new Color( 170, 210,  40);//lime       3 W
        magepaints[181] = new Color(  58, 195,  42);//green      3 W
        magepaints[182] = new Color(  80, 210, 130);//sea        3 W
        magepaints[183] = new Color(  40, 192, 192);//cyan       3 W
        magepaints[184] = new Color(  46, 132, 214);//sky        3 W
        magepaints[185] = new Color(  40,  40, 255);//blue       3 W
        magepaints[186] = new Color( 132,  54, 214);//purple     3 W
        magepaints[187] = new Color( 218,  42, 207);//magenta    3 W
        magepaints[188] = new Color( 255,  30, 120);//hotpink    3 W
        magepaints[189] = new Color( 235, 130, 140);//pink       3 W
        magepaints[190] = new Color( 160, 110, 180);//lilac      3 W
        magepaints[191] = new Color( 195, 110,  20);//peach      3 W
        magepaints[192] = new Color( 195, 110,  20);//peachtint  3 W
        magepaints[193] = new Color( 170, 160,  30);//yellowtint 3 W
        magepaints[194] = new Color(  39, 121,  42);//greentint  3 W
        magepaints[195] = new Color(  45,  42, 122);//bluetint   3 W
        magepaints[196] = new Color( 118,  42, 125);//lilactint  3 W
        magepaints[197] = new Color( 170,  30,  30);//pinktint   3 W
        magepaints[198] = new Color(  39,  39,  39);//white      3 W
        magepaints[199] = new Color( 117, 117, 117);//gray       3 W
        magepaints[200] = new Color( 163, 114,  88);//brown      3 W
        magepaints[201] = new Color( 255,   0,   0);//red        4 B
        magepaints[202] = new Color( 255,  93,   0);//orange     4 B
        magepaints[203] = new Color( 255, 165,   0);//gold       4 B
        magepaints[204] = new Color( 255, 246,   0);//yellow     4 B
        magepaints[205] = new Color( 140, 251,   0);//lime       4 B
        magepaints[206] = new Color(  23, 255,   0);//green      4 B
        magepaints[207] = new Color(   0, 250, 109);//sea        4 B
        magepaints[208] = new Color(   0, 205, 185);//cyan       4 B
        magepaints[209] = new Color(  64, 149, 255);//sky        4 B
        magepaints[210] = new Color(  62,  62, 255);//blue       4 B
        magepaints[211] = new Color( 150,  54, 255);//purple     4 B
        magepaints[212] = new Color( 255,   0, 234);//magenta    4 B
        magepaints[213] = new Color( 255,   0, 101);//hotpink    4 B
        magepaints[214] = new Color( 255, 110, 130);//pink       4 B
        magepaints[215] = new Color( 185, 105, 255);//lilac      4 B
        magepaints[216] = new Color( 255, 140,  60);//peach      4 B
        magepaints[217] = new Color( 255, 185, 125);//peachtint  4 B
        magepaints[218] = new Color( 250, 250, 135);//yellowtint 4 B
        magepaints[219] = new Color( 152, 255, 179);//greentint  4 B
        magepaints[220] = new Color( 160, 180, 255);//bluetint   4 B
        magepaints[221] = new Color( 220, 170, 255);//lilactint  4 B
        magepaints[222] = new Color( 255, 180, 200);//pinktint   4 B
        magepaints[223] = new Color( 255, 255, 255);//white      4 B
        magepaints[224] = new Color( 125, 125, 125);//gray       4 B
        magepaints[225] = new Color( 175, 117,  89);//brown      4 B
        magepaints[226] = new Color( 255,   0,   0);//red        4 W
        magepaints[227] = new Color( 250, 100,   0);//orange     4 W
        magepaints[228] = new Color( 230, 175,   0);//gold       4 W
        magepaints[229] = new Color( 240, 230,   0);//yellow     4 W
        magepaints[230] = new Color( 170, 210,   0);//lime       4 W
        magepaints[231] = new Color(  15, 187,   0);//green      4 W
        magepaints[232] = new Color(  50, 200, 110);//sea        4 W
        magepaints[233] = new Color(   0, 185, 185);//cyan       4 W
        magepaints[234] = new Color(  19, 117, 214);//sky        4 W
        magepaints[235] = new Color(   0,   0, 255);//blue       4 W
        magepaints[236] = new Color( 117,  19, 214);//purple     4 W
        magepaints[237] = new Color( 214,   0, 207);//magenta    4 W
        magepaints[238] = new Color( 255,   0, 110);//hotpink    4 W
        magepaints[239] = new Color( 230, 120, 130);//pink       4 W
        magepaints[240] = new Color( 150, 100, 170);//lilac      4 W
        magepaints[241] = new Color( 195, 110,   0);//peach      4 W
        magepaints[242] = new Color( 195, 110,   0);//peachtint  4 W
        magepaints[243] = new Color( 170, 160,   0);//yellowtint 4 W
        magepaints[244] = new Color(   7,  93,  15);//greentint  4 W
        magepaints[245] = new Color(  15,  20, 110);//bluetint   4 W
        magepaints[246] = new Color( 100,  15, 110);//lilactint  4 W
        magepaints[247] = new Color( 160,   0,   0);//pinktint   4 W
        magepaints[248] = new Color(   0,   0,   0);//white      4 W
        magepaints[249] = new Color(  97,  97,  97);//gray       4 W
        magepaints[250] = new Color( 142,  95,  70);//brown      4 W
        magepaints[251] = new Color(   0,   0,   0);//unused index== 251 
        magepaints[252] = new Color(   0,   0,   0);//unused index== 252 
        magepaints[253] = new Color(   0,   0,   0);//unused index== 253 
        magepaints[254] = new Color(   0,   0,   0);//unused index== 254 
        magepaints[255] = new Color( 255, 255, 255);//deadwhite index== 255 
    }//setcolorpalette
//}}}

//{{{ Variable definitions
//##################################################################################################
    Color       background;
    ArrayList   foreground;
    HashMap     nametable;
    HashMap     indextable;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Protected constructor.
    */
    Palette()
    {
        background = Color.black;
        foreground = new ArrayList(51);
        nametable  = new HashMap(51);
        indextable = new HashMap(51);
    }
//}}}

//{{{ addEntry()
//##################################################################################################
    /**
    * Defines a new color name in the current palette.
    * @param name the name of the new color
    * @param colors an array of at least COLOR_LEVELS colors, from furthest to closest
    * @return the integer index of this color
    */
    public int addEntry(String name, Color[] colors)
    {
        if(colors.length < COLOR_LEVELS) throw new IllegalArgumentException("Must supply at least "+COLOR_LEVELS+" Color objects.");
        
        Integer colorkey;
        if(nametable.containsKey(name))
        {
            colorkey = (Integer)nametable.get(name);
            //echo("Replacing color '"+indextable.get(colorkey)+"' with '"+name+"'");
            foreground.set(colorkey.intValue(), colors.clone());
            // 1-to-1 mapping of ints to names
            nametable.put(name, colorkey);
            indextable.put(colorkey, name);
        }
        else
        {
            colorkey = new Integer(foreground.size());
            //echo("Adding color '"+name+"' with key "+colorkey);
            foreground.add(colors.clone());
            // 1-to-1 mapping of ints to names
            nametable.put(name, colorkey);
            indextable.put(colorkey, name);
        }
        
        return colorkey.intValue();
    }
//}}}

//{{{ getXXX(), hasColor()
//##################################################################################################
    /** Retrieves the index of the named color, or -1 if that color is unknown. */
    public int getIndex(String name)
    {
        Integer colorkey = (Integer)nametable.get(name);
        if(colorkey == null) return -1;
        else return colorkey.intValue();
    }
    
    /** Returns the name associated with this index, or null if none is registered. */
    public String getName(int colorkey)
    { return (String)indextable.get(new Integer(colorkey)); }
    
    /** Gets an array of Colors for the specified index. No bounds checking is done for speed. */
    public Color[] getEntry(int colorkey)
    { return (Color[])foreground.get(colorkey); }
    
    /** Gets the background Color for this Palette */
    public Color getBackground()
    { return background; }
    
    /** Returns true if the named color is known to this palette */
    public boolean hasColor(String name)
    {
        if(name.equalsIgnoreCase("invisible")) return true;
        else return nametable.containsKey(name);
    }
//}}}

//{{{ makeBlackPalette()
//##################################################################################################
    /** Creates a default palette with a black background */
    public static Palette makeBlackPalette()
    {
        Palette p = new Palette();
        p.background = Color.black;
        
        // Match standard names to their entries in the old color palette
        String[] names = {"default", "red", "orange", "rust", "gold", "yellow", "lime", "green", "sea", "seagreen", "cyan", "sky", "skyblue", "blue", "purple", "magenta", "hotpink", "pink", "lilac", "peach", "peachtint", "paleyellow", "yellowtint", "greentint", "bluetint", "lilactint", "pinktint", "white", "gray", "grey", "brown"};
        int[] indices  = { 23,        1,     2,        2,      3,      4,        5,      6,       7,     7,          8,      9,     9,         10,     11,       12,        13,        14,     15,      16,      17,          18,           18,           19,          20,         21,          22,         23,      24,     24,     25    };
        Color[] colors = new Color[5];
        
        for(int i = 0; i < names.length; i++)
        {
            colors[0] = magepaints[ indices[i] +   0 ];
            colors[1] = magepaints[ indices[i] +  50 ];
            colors[2] = magepaints[ indices[i] + 100 ];
            colors[3] = magepaints[ indices[i] + 150 ];
            colors[4] = magepaints[ indices[i] + 200 ];
            p.addEntry(names[i], colors);
        }
        
        colors = new Color[] {Color.white, Color.white, Color.white, Color.white, Color.white};
        p.addEntry("deadwhite", colors);
        
        colors = new Color[] {Color.black, Color.black, Color.black, Color.black, Color.black};
        p.addEntry("deadblack", colors);
        p.addEntry("black"    , colors);
        
        return p;
    }
//}}}

//{{{ makeWhitePalette()
//##################################################################################################
    /** Creates a default palette with a white background */
    public static Palette makeWhitePalette()
    {
        Palette p = new Palette();
        p.background = Color.white;
        
        // Match standard names to their entries in the old color palette
        String[] names = {"default", "red", "orange", "rust", "gold", "yellow", "lime", "green", "sea", "seagreen", "cyan", "sky", "skyblue", "blue", "purple", "magenta", "hotpink", "pink", "lilac", "peach", "peachtint", "paleyellow", "yellowtint", "greentint", "bluetint", "lilactint", "pinktint", "white", "gray", "grey", "brown"};
        int[] indices  = { 23,        1,     2,        2,      3,      4,        5,      6,       7,     7,          8,      9,     9,         10,     11,       12,        13,        14,     15,      16,      17,          18,           18,           19,          20,         21,          22,         23,      24,     24,     25    };
        Color[] colors = new Color[5];
        
        for(int i = 0; i < names.length; i++)
        {
            colors[0] = magepaints[ indices[i] +  25 ];
            colors[1] = magepaints[ indices[i] +  75 ];
            colors[2] = magepaints[ indices[i] + 125 ];
            colors[3] = magepaints[ indices[i] + 175 ];
            colors[4] = magepaints[ indices[i] + 225 ];
            p.addEntry(names[i], colors);
        }
        
        colors = new Color[] {Color.white, Color.white, Color.white, Color.white, Color.white};
        p.addEntry("deadwhite", colors);
        
        colors = new Color[] {Color.black, Color.black, Color.black, Color.black, Color.black};
        p.addEntry("deadblack", colors);
        p.addEntry("black"    , colors);
        
        return p;
    }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}
}//class

