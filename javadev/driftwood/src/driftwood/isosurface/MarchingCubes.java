// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.isosurface;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//}}}
/**
* <code>MarchingCubes</code> is an implementation of the Marching Cubes
* algorithm that can be used for either solid isosurfaces (triangles)
* or efficient wireframe-mesh isosurfaces in kinemage format and related formats.
*
* <p>Changes made from the original paper include renumbering the vertices and edges
* (0-7 and 0-11 instead of 1-8 and 1-12) and using a right-handed coordinate system.
* <p>The original paper was
* <br>William E. Lorenson &amp; Harvey E. Cline (1987) Marching cubes: a high resolution
* 3D surface construction algorithm. ACM Computer Graphics, <b>21</b>(4):163-169
*
* <h3>Wireframe mesh mode</h3>
* The output was optimized by dropping edges on the "high" faces of the cube [those bordering
* (i+1,j+1,k+1) instead of (i,j,k)]. Dropping these edges prevents edge duplication when adjacent
* cubes are processed, at the cost of leaving a slightly "ragged" edge at the high-index boundaries.
*
* <p>Surfaces connectivity patterns were specified by hand for 128 cases, using the
* diagrams from the original pattern. The known issue of "holes" in some surfaces
* was not addressed, because the holes are never noticeable in a wireframe mesh.
* Also, some regions were left as 4-sided, possibly non-planar polygons,
* rather than being arbitrarily divided into 2 triangles.
*
* <h3>Triangle strip isosurface mode</h3>
* Connectivity patterns were specified by hand for the 15 exemplars from the original paper.
* Rotational symmetry was resolved automatically by a Monte Carlo algorithm.
* Again, the issue of potential holes in the surface was not addressed,
* although occasional holes have been observed in the surfaces we generate.
* Many people have tackled this problem, but most of the solutions seem so complicated
* to implement that it's not worthwhile at the present time. For instance, see:
* <br>Evgeni V. Chernyaev, "Marching Cubes 33: Construction of Topologically Correct Isosurfaces"
*
* <p>Copyright (C) 2003-2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Feb  9 17:18:29 EST 2003
*
* @see EdgePlotter for a description of how to use the output from this class.
*/
public class MarchingCubes //extends ... implements ...
{
//{{{ createSparseVectorTable
//##################################################################################################
    static private int[][] createSparseVectorTable()
    {
        int[][] marchingCubes = new int[256][];
        
        // -1 is the flag for a break in the polyline
        marchingCubes[0] = new int[] {};
        marchingCubes[1] = new int[] {0,3,8,0};
        marchingCubes[2] = new int[] {1,0,9};
        marchingCubes[3] = new int[] {1,3,8,9};
        marchingCubes[4] = new int[] {1,2};
        marchingCubes[5] = new int[] {1,2,-1,0,3,8,0};
        marchingCubes[6] = new int[] {2,0,9};
        marchingCubes[7] = new int[] {9,8,10,-1,2,3,8};
        marchingCubes[8] = new int[] {2,3,11};
        marchingCubes[9] = new int[] {11,8,0,2};
        marchingCubes[10] = new int[] {2,3,11,-1,1,0,9};
        marchingCubes[11] = new int[] {9,8,11,9,-1,1,2};
        marchingCubes[12] = new int[] {1,3,11};
        marchingCubes[13] = new int[] {10,8,11,-1,1,0,8};
        marchingCubes[14] = new int[] {0,3,11,9,0};
        marchingCubes[15] = new int[] {9,8,11};
        marchingCubes[16] = new int[] {4,8,7};
        marchingCubes[17] = new int[] {7,3,0,4};
        marchingCubes[18] = new int[] {1,0,9,-1,4,8,7};
        marchingCubes[19] = new int[] {1,3,7,1,-1,4,9};
        marchingCubes[20] = new int[] {1,2,-1,4,8,7};
        marchingCubes[21] = new int[] {1,2,-1,7,3,0,4};
        marchingCubes[22] = new int[] {2,0,9,-1,4,8,7};
        marchingCubes[23] = new int[] {2,3,7,9,2,7,-1,4,9};
        marchingCubes[24] = new int[] {2,3,11,-1,4,8,7};
        marchingCubes[25] = new int[] {2,0,4,2,-1,7,11};
        marchingCubes[26] = new int[] {1,0,9,-1,11,3,2,-1,7,8,4};
        marchingCubes[27] = new int[] {1,2,4,9,-1,7,11};
        marchingCubes[28] = new int[] {1,3,11,-1,4,8,7};
        marchingCubes[29] = new int[] {11,7,0,10,7,-1,4,0,1};
        marchingCubes[30] = new int[] {0,3,11,9,0,-1,4,8,7};
        marchingCubes[32] = new int[] {4,9};
        marchingCubes[33] = new int[] {0,3,8,0,-1,4,9};
        marchingCubes[34] = new int[] {1,0,4};
        marchingCubes[35] = new int[] {1,3,5,-1,4,8,3};
        marchingCubes[36] = new int[] {1,2,-1,4,9};
        marchingCubes[37] = new int[] {1,2,-1,4,9,-1,0,3,8,0};
        marchingCubes[38] = new int[] {0,2,4,0};
        marchingCubes[39] = new int[] {2,4,8,3,2};
        marchingCubes[40] = new int[] {2,3,11,-1,4,9};
        marchingCubes[41] = new int[] {2,0,8,11,-1,4,9};
        marchingCubes[42] = new int[] {2,3,11,-1,1,0,4};
        marchingCubes[43] = new int[] {4,8,11,-1,1,2,5,8,2};
        marchingCubes[44] = new int[] {1,3,11,-1,4,9};
        marchingCubes[45] = new int[] {11,8,10,-1,1,0,8,-1,4,9};
        marchingCubes[46] = new int[] {11,3,0,4,10,3,4};
        marchingCubes[48] = new int[] {7,8,9};
        marchingCubes[49] = new int[] {7,3,5,-1,9,0,3};
        marchingCubes[50] = new int[] {0,1,7,8,0};
        marchingCubes[51] = new int[] {1,3,7};
        marchingCubes[52] = new int[] {1,2,-1,7,8,9};
        marchingCubes[53] = new int[] {7,3,5,-1,9,0,3,-1,1,2};
        marchingCubes[54] = new int[] {2,0,8,7,10,0,7};
        marchingCubes[56] = new int[] {7,8,9,-1,2,3,11};
        marchingCubes[57] = new int[] {11,7,9,2,7,-1,9,0,2};
        marchingCubes[58] = new int[] {0,1,7,8,0,-1,2,3,11};
        marchingCubes[60] = new int[] {7,8,9,-1,1,3,11};
        marchingCubes[64] = new int[] {};
        marchingCubes[65] = new int[] {0,3,8,0};
        marchingCubes[66] = new int[] {1,0,9};
        marchingCubes[67] = new int[] {1,3,8,9};
        marchingCubes[68] = new int[] {1,2};
        marchingCubes[69] = new int[] {1,2,-1,0,3,8,0};
        marchingCubes[70] = new int[] {2,0,6,-1,0,9};
        marchingCubes[71] = new int[] {2,3,8,9,6,3,9};
        marchingCubes[72] = new int[] {2,3,11};
        marchingCubes[73] = new int[] {2,0,8,11};
        marchingCubes[74] = new int[] {1,0,9,-1,2,3,11};
        marchingCubes[75] = new int[] {8,9,11,8,-1,1,2};
        marchingCubes[76] = new int[] {1,3,5,-1,3,11};
        marchingCubes[77] = new int[] {11,8,0,1,6,8,1};
        marchingCubes[78] = new int[] {9,0,3,11,9};
        marchingCubes[80] = new int[] {4,8,7};
        marchingCubes[81] = new int[] {4,0,3,7};
        marchingCubes[82] = new int[] {1,0,9,-1,4,8,7};
        marchingCubes[83] = new int[] {1,3,7,1,-1,4,9};
        marchingCubes[84] = new int[] {1,2,-1,4,8,7};
        marchingCubes[85] = new int[] {1,2,-1,4,0,3,7};
        marchingCubes[86] = new int[] {2,0,6,-1,0,9,-1,4,8,7};
        marchingCubes[88] = new int[] {2,3,11,-1,4,8,7};
        marchingCubes[89] = new int[] {0,2,4,0,-1,7,11};
        marchingCubes[90] = new int[] {4,8,7,-1,2,3,11,-1,1,0,9};
        marchingCubes[92] = new int[] {1,3,5,-1,3,11,-1,4,8,7};
        marchingCubes[96] = new int[] {4,9};
        marchingCubes[97] = new int[] {4,9,-1,0,3,8,0};
        marchingCubes[98] = new int[] {4,0,6,-1,0,1};
        marchingCubes[99] = new int[] {1,3,8,4,10,3,4};
        marchingCubes[100] = new int[] {1,2,4,9};
        marchingCubes[101] = new int[] {1,2,4,9,-1,0,3,8,0};
        marchingCubes[102] = new int[] {2,0,4};
        marchingCubes[104] = new int[] {4,9,-1,2,3,11};
        marchingCubes[105] = new int[] {4,9,-1,2,0,8,11};
        marchingCubes[106] = new int[] {4,0,6,-1,0,1,-1,2,3,11};
        marchingCubes[108] = new int[] {9,4,11,1,4,-1,11,3,1};
        marchingCubes[112] = new int[] {9,8,10,-1,7,8};
        marchingCubes[113] = new int[] {9,0,3,7,10,0,7,10};
        marchingCubes[114] = new int[] {1,0,8,7,1};
        marchingCubes[116] = new int[] {9,8,7,-1,2,1,8,6,1};
        marchingCubes[120] = new int[] {2,3,11,-1,9,8,10,-1,7,8};
        marchingCubes[128] = new int[] {7,11};
        marchingCubes[129] = new int[] {7,11,-1,0,3,8,0};
        marchingCubes[130] = new int[] {7,11,-1,1,0,9};
        marchingCubes[131] = new int[] {1,3,8,9,-1,7,11};
        marchingCubes[132] = new int[] {7,11,-1,1,2};
        marchingCubes[133] = new int[] {7,11,-1,1,2,-1,0,3,8,0};
        marchingCubes[134] = new int[] {2,0,9,-1,11,7};
        marchingCubes[136] = new int[] {2,3,7};
        marchingCubes[137] = new int[] {2,0,6,-1,7,8,0};
        marchingCubes[138] = new int[] {2,3,7,-1,1,0,9};
        marchingCubes[140] = new int[] {1,3,7,1};
        marchingCubes[144] = new int[] {4,8,11};
        marchingCubes[145] = new int[] {4,0,6,-1,0,3,11};
        marchingCubes[146] = new int[] {4,8,11,-1,1,0,9};
        marchingCubes[148] = new int[] {4,8,11,-1,1,2};
        marchingCubes[152] = new int[] {2,3,8,4,2};
        marchingCubes[160] = new int[] {7,11,-1,4,9};
        marchingCubes[161] = new int[] {7,11,-1,4,9,-1,0,3,8,0};
        marchingCubes[162] = new int[] {1,0,4,-1,7,11};
        marchingCubes[164] = new int[] {1,2,-1,4,9,-1,7,11};
        marchingCubes[168] = new int[] {2,3,7,-1,4,9};
        marchingCubes[176] = new int[] {8,9,11,8};
        marchingCubes[192] = new int[] {7,11};
        marchingCubes[193] = new int[] {7,11,-1,0,3,8,0};
        marchingCubes[194] = new int[] {7,11,-1,1,0,9};
        marchingCubes[196] = new int[] {2,1,7,11};
        marchingCubes[200] = new int[] {5,3,7,-1,2,3};
        marchingCubes[208] = new int[] {10,8,11,-1,4,8};
        marchingCubes[224] = new int[] {4,9,11,7};
        
        for(int i = 0; i < 256; i++)
        {
            if(marchingCubes[i] == null)
                marchingCubes[i] = marchingCubes[i ^ 0xff];
            if(marchingCubes[i] == null)
                throw new Error("Marching cubes data missing for lookup index #"+i);
        }
        
        return marchingCubes;
    }
//}}}

//{{{ createTriangleStripTable
//##################################################################################################
    static private int[][] createTriangleStripTable()
    {
        int[][] marchingCubes = new int[256][];
        
        // -1 is the flag for a break in the triangle strip
        marchingCubes[0] = new int[] {};
        marchingCubes[1] = new int[] {0,8,3};
        marchingCubes[2] = new int[] {9,0,1};
        marchingCubes[3] = new int[] {9,8,1,3};
        marchingCubes[4] = new int[] {10,1,2};
        marchingCubes[5] = new int[] {0,8,3,-1,2,10,1};
        marchingCubes[6] = new int[] {0,2,9,10};
        marchingCubes[7] = new int[] {2,3,10,8,9};
        marchingCubes[8] = new int[] {3,11,2};
        marchingCubes[9] = new int[] {8,11,0,2};
        marchingCubes[10] = new int[] {1,9,0,-1,3,11,2};
        marchingCubes[11] = new int[] {1,2,9,11,8};
        marchingCubes[12] = new int[] {11,10,3,1};
        marchingCubes[13] = new int[] {0,1,8,10,11};
        marchingCubes[14] = new int[] {3,0,11,9,10};
        marchingCubes[15] = new int[] {9,10,8,11};
        marchingCubes[16] = new int[] {8,4,7};
        marchingCubes[17] = new int[] {7,3,4,0};
        marchingCubes[18] = new int[] {4,7,8,-1,0,1,9};
        marchingCubes[19] = new int[] {4,9,7,1,3};
        marchingCubes[20] = new int[] {7,8,4,-1,10,1,2};
        marchingCubes[21] = new int[] {0,4,3,7,-1,2,10,1};
        marchingCubes[22] = new int[] {0,2,9,10,-1,4,7,8};
        marchingCubes[23] = new int[] {3,2,7,9,4,-1,2,10,9};
        marchingCubes[24] = new int[] {8,4,7,-1,11,2,3};
        marchingCubes[25] = new int[] {11,7,2,4,0};
        marchingCubes[26] = new int[] {11,3,2,-1,0,1,9,-1,7,8,4};
        marchingCubes[27] = new int[] {4,7,9,11,1,2};
        marchingCubes[28] = new int[] {11,10,3,1,-1,8,4,7};
        marchingCubes[29] = new int[] {4,0,7,10,11,-1,0,10,1};
        marchingCubes[30] = new int[] {8,7,4,-1,3,0,11,9,10};
        marchingCubes[31] = new int[] {4,7,9,11,10};
        marchingCubes[32] = new int[] {5,4,9};
        marchingCubes[33] = new int[] {9,5,4,-1,8,3,0};
        marchingCubes[34] = new int[] {4,0,5,1};
        marchingCubes[35] = new int[] {8,4,3,5,1};
        marchingCubes[36] = new int[] {5,4,9,-1,1,2,10};
        marchingCubes[37] = new int[] {2,1,10,-1,9,5,4,-1,3,0,8};
        marchingCubes[38] = new int[] {5,10,4,2,0};
        marchingCubes[39] = new int[] {2,10,3,5,8,4};
        marchingCubes[40] = new int[] {2,3,11,-1,5,4,9};
        marchingCubes[41] = new int[] {8,11,0,2,-1,9,5,4};
        marchingCubes[42] = new int[] {1,5,0,4,-1,3,11,2};
        marchingCubes[43] = new int[] {11,8,2,5,1,-1,8,5,4};
        marchingCubes[44] = new int[] {1,3,10,11,-1,5,4,9};
        marchingCubes[45] = new int[] {9,4,5,-1,0,1,8,10,11};
        marchingCubes[46] = new int[] {0,3,4,10,5,-1,3,11,10};
        marchingCubes[47] = new int[] {5,4,10,8,11};
        marchingCubes[48] = new int[] {8,9,7,5};
        marchingCubes[49] = new int[] {9,0,5,3,7};
        marchingCubes[50] = new int[] {0,8,1,7,5};
        marchingCubes[51] = new int[] {1,3,5,7};
        marchingCubes[52] = new int[] {5,7,9,8,-1,1,2,10};
        marchingCubes[53] = new int[] {1,10,2,-1,9,0,5,3,7};
        marchingCubes[54] = new int[] {2,0,10,7,5,-1,0,7,8};
        marchingCubes[55] = new int[] {2,10,3,5,7};
        marchingCubes[56] = new int[] {8,9,7,5,-1,11,2,3};
        marchingCubes[57] = new int[] {0,9,2,7,11,-1,9,5,7};
        marchingCubes[58] = new int[] {3,2,11,-1,0,8,1,7,5};
        marchingCubes[59] = new int[] {11,2,7,1,5};
        marchingCubes[60] = new int[] {10,1,11,3,-1,5,9,7,8};
        marchingCubes[61] = new int[] {10,11,5,7,-1,9,0,1};
        marchingCubes[62] = new int[] {7,5,11,10,-1,3,0,8};
        marchingCubes[63] = new int[] {10,11,5,7};
        marchingCubes[64] = new int[] {6,5,10};
        marchingCubes[65] = new int[] {0,8,3,-1,10,6,5};
        marchingCubes[66] = new int[] {10,6,5,-1,9,0,1};
        marchingCubes[67] = new int[] {9,8,1,3,-1,10,6,5};
        marchingCubes[68] = new int[] {2,6,1,5};
        marchingCubes[69] = new int[] {2,6,1,5,-1,0,8,3};
        marchingCubes[70] = new int[] {9,5,0,6,2};
        marchingCubes[71] = new int[] {8,9,3,6,2,-1,9,6,5};
        marchingCubes[72] = new int[] {2,3,11,-1,6,5,10};
        marchingCubes[73] = new int[] {2,0,11,8,-1,6,5,10};
        marchingCubes[74] = new int[] {3,2,11,-1,10,6,5,-1,0,1,9};
        marchingCubes[75] = new int[] {10,5,6,-1,1,2,9,11,8};
        marchingCubes[76] = new int[] {6,11,5,3,1};
        marchingCubes[77] = new int[] {1,0,5,11,6,-1,0,8,11};
        marchingCubes[78] = new int[] {6,5,11,9,3,0};
        marchingCubes[79] = new int[] {6,5,11,9,8};
        marchingCubes[80] = new int[] {5,10,6,-1,7,8,4};
        marchingCubes[81] = new int[] {7,3,4,0,-1,5,10,6};
        marchingCubes[82] = new int[] {7,4,8,-1,9,0,1,-1,6,5,10};
        marchingCubes[83] = new int[] {5,6,10,-1,4,9,7,1,3};
        marchingCubes[84] = new int[] {5,1,6,2,-1,7,8,4};
        marchingCubes[85] = new int[] {4,7,0,3,-1,5,6,1,2};
        marchingCubes[86] = new int[] {4,8,7,-1,9,5,0,6,2};
        marchingCubes[87] = new int[] {6,2,7,3,-1,4,9,5};
        marchingCubes[88] = new int[] {5,6,10,-1,11,2,3,-1,4,7,8};
        marchingCubes[89] = new int[] {6,10,5,-1,11,7,2,4,0};
        marchingCubes[90] = new int[] {9,0,1,-1,11,3,2,-1,5,10,6,-1,8,7,4};
        marchingCubes[91] = new int[] {11,6,7,-1,5,4,9,-1,2,10,1};
        marchingCubes[92] = new int[] {7,4,8,-1,6,11,5,3,1};
        marchingCubes[93] = new int[] {4,0,5,1,-1,6,11,7};
        marchingCubes[94] = new int[] {3,8,0,-1,4,9,5,-1,11,7,6};
        marchingCubes[95] = new int[] {6,11,7,-1,4,9,5};
        marchingCubes[96] = new int[] {9,10,4,6};
        marchingCubes[97] = new int[] {9,10,4,6,-1,8,3,0};
        marchingCubes[98] = new int[] {10,1,6,0,4};
        marchingCubes[99] = new int[] {4,8,6,1,10,-1,8,3,1};
        marchingCubes[100] = new int[] {1,9,2,4,6};
        marchingCubes[101] = new int[] {0,3,8,-1,1,9,2,4,6};
        marchingCubes[102] = new int[] {2,0,6,4};
        marchingCubes[103] = new int[] {8,3,4,2,6};
        marchingCubes[104] = new int[] {6,4,10,9,-1,2,3,11};
        marchingCubes[105] = new int[] {0,8,2,11,-1,9,4,10,6};
        marchingCubes[106] = new int[] {2,11,3,-1,10,1,6,0,4};
        marchingCubes[107] = new int[] {11,8,6,4,-1,10,1,2};
        marchingCubes[108] = new int[] {4,6,9,3,1,-1,6,3,11};
        marchingCubes[109] = new int[] {4,6,8,11,-1,0,1,9};
        marchingCubes[110] = new int[] {3,11,0,6,4};
        marchingCubes[111] = new int[] {11,8,6,4};
        marchingCubes[112] = new int[] {7,6,8,10,9};
        marchingCubes[113] = new int[] {3,7,0,10,9,-1,7,10,6};
        marchingCubes[114] = new int[] {0,1,8,10,7,6};
        marchingCubes[115] = new int[] {10,6,1,7,3};
        marchingCubes[116] = new int[] {6,7,2,9,1,-1,7,8,9};
        marchingCubes[117] = new int[] {3,7,2,6,-1,1,9,0};
        marchingCubes[118] = new int[] {7,8,6,0,2};
        marchingCubes[119] = new int[] {6,2,7,3};
        marchingCubes[120] = new int[] {11,3,2,-1,7,6,8,10,9};
        marchingCubes[121] = new int[] {10,9,2,0,-1,11,7,6};
        marchingCubes[122] = new int[] {7,11,6,-1,2,10,1,-1,8,3,0};
        marchingCubes[123] = new int[] {11,7,6,-1,10,1,2};
        marchingCubes[124] = new int[] {3,1,8,9,-1,7,6,11};
        marchingCubes[125] = new int[] {11,7,6,-1,1,9,0};
        marchingCubes[126] = new int[] {7,6,11,-1,3,0,8};
        marchingCubes[127] = new int[] {7,6,11};
        
        for(int i = 0; i < 256; i++)
        {
            if(marchingCubes[i] == null)
                marchingCubes[i] = marchingCubes[i ^ 0xff];
            if(marchingCubes[i] == null)
                throw new Error("Marching cubes data missing for lookup index #"+i);
        }
        
        return marchingCubes;
    }
//}}}

//{{{ Constants
    static final int[][] SPARSE_VECTOR  = createSparseVectorTable();
    static final int[][] TRIANGLE_STRIP = createTriangleStripTable();
    
    /** The triangle strip isosurface mode */
    static final public Object MODE_TRIANGLE    = "triangle strip isosurface";
    /** The optimized wireframe mesh isosurface mode */
    static final public Object MODE_MESH        = "optimized wireframe mesh isosurface";
//}}}

//{{{ Variable definitions
//##################################################################################################
    VertexLocator       locator;        // provides <x,y,z> coordinates
    VertexEvaluator     evaluator;      // provides values for vertices
    EdgePlotter         plotter;        // gets fed the edges we create
    final int[][]       mcubes;         // the lookup table
    final Object        mode;           // one of the defined MODE constants
    
    // Variables used internally during plotting operations
    // values of vertices
    double vv0 = 0, vv1 = 0, vv2 = 0, vv3 = 0, vv4 = 0, vv5 = 0, vv6 = 0, vv7 = 0;
    // coordinates of vertices
    double[] cv0 = {0,0,0}, cv1 = {0,0,0}, cv2 = {0,0,0}, cv3 = {0,0,0},
             cv4 = {0,0,0}, cv5 = {0,0,0}, cv6 = {0,0,0}, cv7 = {0,0,0};
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public MarchingCubes(VertexLocator locator, VertexEvaluator evaluator, EdgePlotter plotter, Object mode)
    {
        this.locator    = locator;
        this.evaluator  = evaluator;
        this.plotter    = plotter;
        this.mode       = mode;
        
        if(mode == MODE_TRIANGLE)
            this.mcubes = TRIANGLE_STRIP;
        else if(mode == MODE_MESH)
            this.mcubes = SPARSE_VECTOR;
        else throw new IllegalArgumentException("Illegal mode: "+mode);
    }
//}}}

//{{{ march
//##################################################################################################
    /**
    * Performs marching cubes surface generation on a rectangular subset of the data.
    * @param startI the starting index in x, inclusive
    * @param startJ the starting index in y, inclusive
    * @param startK the starting index in z, inclusive
    * @param endI   the ending index in x, inclusive
    * @param endJ   the ending index in y, inclusive
    * @param endK   the ending index in z, inclusive
    * @param value  the value at which to generate the isosurface
    */
    public void march(int startI, int startJ, int startK, int endI, int endJ, int endK, double value)
    {
        int i, j, k; // loop indices
        // Use < instead of <= b/c at any point
        // we still use <i+1, j+1, k+1>
        
        plotter.startIsosurface(value);
        for(i = startI; i < endI; i++)
        {
            for(j = startJ; j < endJ; j++)
            {
                for(k = startK; k < endK; k++)
                {
                    evalCell(i, j, k, value);
                }
            }
        }
        plotter.endIsosurface(value);
    }
    
    /**
    * Performs marching cubes surface generation on a spherical subset of the data.
    * @param ctrI   the starting index in x, inclusive
    * @param ctrJ   the starting index in y, inclusive
    * @param ctrK   the starting index in z, inclusive
    * @param radius the radius in which to render
    * @param value  the value at which to generate the isosurface
    */
    /*public void march(int ctrI, int ctrJ, int ctrK, double radius, double value)
    {
        int i, j, k; // loop indices
        
        plotter.startIsosurface(value);
        for(i = startI; i < endI; i++)
        {
            for(j = startJ; j < endJ; j++)
            {
                for(k = startK; k < endK; k++)
                {
                    evalCell(i, j, k, value);
                }
            }
        }
        plotter.endIsosurface(value);
    }*/
//}}}

//{{{ evalCell
//##################################################################################################
    /** Performs marching cubes on one cell. */
    void evalCell(int i, int j, int k, double value)
    {
        plotter.startCell(i, j, k);
        
        // Find value at each corner of the cell
        vv0 = evaluator.evaluateVertex(i  , j  , k  );
        vv1 = evaluator.evaluateVertex(i+1, j  , k  );
        vv2 = evaluator.evaluateVertex(i+1, j+1, k  );
        vv3 = evaluator.evaluateVertex(i  , j+1, k  );
        vv4 = evaluator.evaluateVertex(i  , j  , k+1);
        vv5 = evaluator.evaluateVertex(i+1, j  , k+1);
        vv6 = evaluator.evaluateVertex(i+1, j+1, k+1);
        vv7 = evaluator.evaluateVertex(i  , j+1, k+1);
        
        // If we were unable to evaluate any one vertex, skip this cell entirely.
        if(Double.isNaN(vv0) || Double.isNaN(vv1) || Double.isNaN(vv2) || Double.isNaN(vv3)
        || Double.isNaN(vv4) || Double.isNaN(vv5) || Double.isNaN(vv6) || Double.isNaN(vv7)) return;
        
        // Calculate the bitmask for the lookup table
        int mask = 0;
        if(vv0 > value) mask |= 0x01;
        if(vv1 > value) mask |= 0x02;
        if(vv2 > value) mask |= 0x04;
        if(vv3 > value) mask |= 0x08;
        if(vv4 > value) mask |= 0x10;
        if(vv5 > value) mask |= 0x20;
        if(vv6 > value) mask |= 0x40;
        if(vv7 > value) mask |= 0x80;
        
        // If we're 100% inside or 100% outside, there's nothing to do
        if(mask == 0 || mask == 0xff)
        {
            plotter.endCell(i, j, k);
            return;
        }
        
        // Otherwise, we're going to need <x,y,z> coordinates for each corner
        locator.locateVertex(i  , j  , k  , cv0);
        locator.locateVertex(i+1, j  , k  , cv1);
        locator.locateVertex(i+1, j+1, k  , cv2);
        locator.locateVertex(i  , j+1, k  , cv3);
        locator.locateVertex(i  , j  , k+1, cv4);
        locator.locateVertex(i+1, j  , k+1, cv5);
        locator.locateVertex(i+1, j+1, k+1, cv6);
        locator.locateVertex(i  , j+1, k+1, cv7);
        
        // Now we walk the path of vertices from mcubes[][]
        int[]       path    = mcubes[mask];
        int         cnt     = 0;
        int         end     = path.length;
        double[]    xyz     = {0,0,0};
        boolean     lineto  = false;
        
        // debugging -- double dx = 0, dy = 0, dz = 0;
        
        for(cnt = 0; cnt < end; cnt++)
        {
            if(path[cnt] == -1) lineto = false; // -1 is the flag for a break in the polyline
            else
            {
                getEdge(path[cnt], value, xyz);
                plotter.plotEdge(xyz[0], xyz[1], xyz[2], lineto);
                
                /* debugging * /
                if(lineto)
                {
                    dx = dx - xyz[0];
                    dy = dy - xyz[1];
                    dz = dz - xyz[2];
                    if(dx*dx + dy*dy + dz*dz > 8.0) SoftLog.err.println("Long edge on type "+mask+" at index "+cnt);
                }
                dx = xyz[0];
                dy = xyz[1];
                dz = xyz[2];
                /* debugging */
                
                lineto = true;
            }
        }
        
        // Finished!
        plotter.endCell(i, j, k);
    }
//}}}

//{{{ getEdge
//##################################################################################################
    /** Calculates the coordinates of an edge intersection point, assuming vv# and cv# have been set. */
    void getEdge(int edge, double value, double[] xyz)
    {
        double alpha, one_alpha; // i.e. 1 - alpha
        // We interpolate in x, y, and z in case the edges of the cell are
        // not parallel to the coordinate axes, as may be the case for elec. density.
        switch(edge)
        {
            case 0: // v0 to v1
                alpha = (vv1 - value) / (vv1 - vv0);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv0[0] + one_alpha*cv1[0];
                xyz[1] = alpha*cv0[1] + one_alpha*cv1[1];
                xyz[2] = alpha*cv0[2] + one_alpha*cv1[2];
                break;
            case 1: // v2 to v1
                alpha = (vv1 - value) / (vv1 - vv2);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv2[0] + one_alpha*cv1[0];
                xyz[1] = alpha*cv2[1] + one_alpha*cv1[1];
                xyz[2] = alpha*cv2[2] + one_alpha*cv1[2];
                break;
            case 2: // v2 to v3
                alpha = (vv3 - value) / (vv3 - vv2);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv2[0] + one_alpha*cv3[0];
                xyz[1] = alpha*cv2[1] + one_alpha*cv3[1];
                xyz[2] = alpha*cv2[2] + one_alpha*cv3[2];
                break;
            case 3: // v0 to v3
                alpha = (vv3 - value) / (vv3 - vv0);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv0[0] + one_alpha*cv3[0];
                xyz[1] = alpha*cv0[1] + one_alpha*cv3[1];
                xyz[2] = alpha*cv0[2] + one_alpha*cv3[2];
                break;
            case 4: // v4 to v5
                alpha = (vv5 - value) / (vv5 - vv4);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv4[0] + one_alpha*cv5[0];
                xyz[1] = alpha*cv4[1] + one_alpha*cv5[1];
                xyz[2] = alpha*cv4[2] + one_alpha*cv5[2];
                break;
            case 5: // v5 to v6
                alpha = (vv6 - value) / (vv6 - vv5);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv5[0] + one_alpha*cv6[0];
                xyz[1] = alpha*cv5[1] + one_alpha*cv6[1];
                xyz[2] = alpha*cv5[2] + one_alpha*cv6[2];
                break;
            case 6: // v7 to v6
                alpha = (vv6 - value) / (vv6 - vv7);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv7[0] + one_alpha*cv6[0];
                xyz[1] = alpha*cv7[1] + one_alpha*cv6[1];
                xyz[2] = alpha*cv7[2] + one_alpha*cv6[2];
                break;
            case 7: // v7 to v4
                alpha = (vv4 - value) / (vv4 - vv7);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv7[0] + one_alpha*cv4[0];
                xyz[1] = alpha*cv7[1] + one_alpha*cv4[1];
                xyz[2] = alpha*cv7[2] + one_alpha*cv4[2];
                break;
            case 8: // v0 to v4
                alpha = (vv4 - value) / (vv4 - vv0);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv0[0] + one_alpha*cv4[0];
                xyz[1] = alpha*cv0[1] + one_alpha*cv4[1];
                xyz[2] = alpha*cv0[2] + one_alpha*cv4[2];
                break;
            case 9: // v5 to v1
                alpha = (vv1 - value) / (vv1 - vv5);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv5[0] + one_alpha*cv1[0];
                xyz[1] = alpha*cv5[1] + one_alpha*cv1[1];
                xyz[2] = alpha*cv5[2] + one_alpha*cv1[2];
                break;
            case 10: // v2 to v6
                alpha = (vv6 - value) / (vv6 - vv2);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv2[0] + one_alpha*cv6[0];
                xyz[1] = alpha*cv2[1] + one_alpha*cv6[1];
                xyz[2] = alpha*cv2[2] + one_alpha*cv6[2];
                break;
            case 11: // v3 to v7
                alpha = (vv7 - value) / (vv7 - vv3);
                one_alpha = 1 - alpha;
                xyz[0] = alpha*cv3[0] + one_alpha*cv7[0];
                xyz[1] = alpha*cv3[1] + one_alpha*cv7[1];
                xyz[2] = alpha*cv3[2] + one_alpha*cv7[2];
                break;
            default:
                throw new IllegalArgumentException(edge+" is not a legal edge index from 0 to 11");
        }
        
        /* debugging * /
        if(alpha < 0.0 || alpha > 1.0 || alpha+one_alpha != 1.0) SoftLog.err.println("Bad edge!");
        /* debugging */
    }
//}}}

//{{{ getMode
//##################################################################################################
    /**
    * Returns the MarchingCubes.MODE_xxx constant that this class was created with.
    */
    public Object getMode()
    { return this.mode; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

