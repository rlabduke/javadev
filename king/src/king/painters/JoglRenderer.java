// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.painters;
import king.core.*;

import java.awt.*;
import java.nio.*;
import java.util.*;
import driftwood.r3.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.common.nio.Buffers;
//}}}
/**
* <code>JoglRenderer</code> is a VBO-based OpenGL renderer for kinemage data.
* It bypasses the CPU-side transform/z-buffer of Engine2D and instead:
* <ul>
* <li>Packs model-space vertex data into NIO FloatBuffers</li>
* <li>Uploads to VBOs on the GPU</li>
* <li>Uses a vertex shader for model-to-screen transformation</li>
* <li>Uses a fragment shader for fog-based depth cueing</li>
* <li>Uses GL_DEPTH_TEST for hardware depth sorting</li>
* </ul>
*
* <p>Copyright (C) 2026 by Vincent B. Chen. All rights reserved.
* <br>Begun on Wed Feb 11 2026
*/
public class JoglRenderer
{
//{{{ Constants
    /** Floats per vertex for lines and dots: x,y,z, r,g,b,a */
    static final int FLOATS_PER_VERT = 7;
    /** Floats per vertex for triangles: x,y,z, nx,ny,nz, r,g,b,a */
    static final int FLOATS_PER_TRI_VERT = 10;
    /** Initial capacity in vertices for scratch buffers */
    static final int INITIAL_CAPACITY = 16384;
//}}}

//{{{ Variable definitions
//##############################################################################
    int     program     = 0;    // shader program handle
    int[]   vbo         = null; // VBO handles: [0]=lines, [1]=dots, [2]=tris
    GLUT    glut        = null;

    // Uniform locations
    int     uMvp, uMv, uLighting, uLightDir;
    int     uFogColor, uFogStart, uFogEnd;
    // Attribute locations
    int     aPosition, aColor, aNormal;

    // Scratch buffers (reused each frame, grown as needed)
    FloatBuffer lineVerts;
    FloatBuffer dotVerts;
    FloatBuffer triVerts;
    int         lineVertCount;
    int         dotVertCount;
    int         triVertCount;

    // Per-list draw commands: each int[] = {startVertex, vertexCount, width}
    ArrayList<int[]> lineDrawCmds = new ArrayList<int[]>();
    ArrayList<int[]> dotDrawCmds  = new ArrayList<int[]>();

    // Label data collected during packing (rendered in a separate pass)
    ArrayList<float[]>  labelPositions  = new ArrayList<float[]>();  // {x,y,z}
    ArrayList<String>   labelTexts      = new ArrayList<String>();
    ArrayList<float[]>  labelColors     = new ArrayList<float[]>(); // {r,g,b}

    // Reusable matrix arrays (column-major for GL)
    float[] mvpMatrix   = new float[16];
    float[] mvMatrix    = new float[16];

    // Pre-computed unit sphere mesh (subdivided icosahedron, depth 1 = 80 tris)
    // Each triangle is 9 floats: x1,y1,z1, x2,y2,z2, x3,y3,z3 (normals = positions for unit sphere)
    float[] sphereMesh  = null;
    int     sphereTris  = 0;
//}}}

//{{{ init
//##############################################################################
    /**
    * Initialize shaders and VBOs. Call once from GLEventListener.init().
    */
    public void init(GL2 gl)
    {
        program = JoglShaders.compileProgram(gl);
        if(program == 0)
        {
            System.err.println("JoglRenderer: shader compilation failed, falling back");
            return;
        }

        // Cache uniform locations
        uMvp        = gl.glGetUniformLocation(program, "u_mvp");
        uMv         = gl.glGetUniformLocation(program, "u_mv");
        uLighting   = gl.glGetUniformLocation(program, "u_lighting");
        uLightDir   = gl.glGetUniformLocation(program, "u_lightDir");
        uFogColor   = gl.glGetUniformLocation(program, "u_fogColor");
        uFogStart   = gl.glGetUniformLocation(program, "u_fogStart");
        uFogEnd     = gl.glGetUniformLocation(program, "u_fogEnd");

        // Cache attribute locations
        aPosition   = gl.glGetAttribLocation(program, "a_position");
        aColor      = gl.glGetAttribLocation(program, "a_color");
        aNormal     = gl.glGetAttribLocation(program, "a_normal");

        // Create VBOs
        vbo = new int[3];
        gl.glGenBuffers(3, vbo, 0);

        glut = new GLUT();

        // Allocate scratch buffers
        lineVerts   = Buffers.newDirectFloatBuffer(INITIAL_CAPACITY * FLOATS_PER_VERT);
        dotVerts    = Buffers.newDirectFloatBuffer(INITIAL_CAPACITY * FLOATS_PER_VERT);
        triVerts    = Buffers.newDirectFloatBuffer(INITIAL_CAPACITY * FLOATS_PER_TRI_VERT);

        // Build unit sphere mesh (subdivided icosahedron)
        buildSphereMesh(1); // depth 1 = 80 triangles
    }
//}}}

//{{{ dispose
//##############################################################################
    /**
    * Release GL resources. Call from GLEventListener.dispose().
    */
    public void dispose(GL2 gl)
    {
        if(vbo != null)
        {
            gl.glDeleteBuffers(3, vbo, 0);
            vbo = null;
        }
        if(program != 0)
        {
            gl.glDeleteProgram(program);
            program = 0;
        }
    }
//}}}

//{{{ isReady
//##############################################################################
    /** Returns true if shaders compiled and VBOs are ready. */
    public boolean isReady()
    {
        return program != 0 && vbo != null;
    }
//}}}

//{{{ render
//##############################################################################
    /**
    * Main per-frame rendering entry point.
    * @param kin        the current kinemage
    * @param view       the current view (rotation, zoom, clip)
    * @param bounds     the pixel rectangle to render into
    * @param gl         the GL2 context
    * @param engine     the Engine2D (used for color resolution flags)
    */
    public void render(Kinemage kin, KView view, Rectangle bounds, GL2 gl, Engine2D engine)
    {
        if(!isReady()) return;

        double width  = bounds.getWidth();
        double height = bounds.getHeight();
        double size   = Math.min(width, height);

        // Build matrices from KView
        double cx, cy, cz;
        double R11, R12, R13, R21, R22, R23, R31, R32, R33;
        double zoom, span, clip;
        synchronized(view)
        {
            view.compile();
            span    = view.getSpan();
            clip    = view.getClip();
            cx      = view.cx;
            cy      = view.cy;
            cz      = view.cz;
            R11     = view.R11;
            R12     = view.R12;
            R13     = view.R13;
            R21     = view.R21;
            R22     = view.R22;
            R23     = view.R23;
            R31     = view.R31;
            R32     = view.R32;
            R33     = view.R33;
        }
        zoom = size / span;

        double perspDist = 5.0 * size;
        double clipScaling = size / 2.0;
        double clipFront = clip;
        double clipBack  = -clip;

        // ModelView: translate(-cx,-cy,-cz) -> rotate -> scale(zoom) -> translate(0,0,-perspDist)
        // We build the 4x4 in column-major order for GL.
        //
        // MV = T(0,0,-perspDist) * R * S(zoom) * T(-cx,-cy,-cz)
        // Let R' = R * zoom (combine rotate+scale)
        double r11 = R11*zoom, r12 = R12*zoom, r13 = R13*zoom;
        double r21 = R21*zoom, r22 = R22*zoom, r23 = R23*zoom;
        double r31 = R31*zoom, r32 = R32*zoom, r33 = R33*zoom;

        // After R*S*T(-center), the translation part is R'*(-center):
        double tx = -(r11*cx + r12*cy + r13*cz);
        double ty = -(r21*cx + r22*cy + r23*cz);
        double tz = -(r31*cx + r32*cy + r33*cz) - perspDist;

        // Column-major MV matrix
        mvMatrix[0]  = (float)r11;   mvMatrix[1]  = (float)r21;   mvMatrix[2]  = (float)r31;   mvMatrix[3]  = 0;
        mvMatrix[4]  = (float)r12;   mvMatrix[5]  = (float)r22;   mvMatrix[6]  = (float)r32;   mvMatrix[7]  = 0;
        mvMatrix[8]  = (float)r13;   mvMatrix[9]  = (float)r23;   mvMatrix[10] = (float)r33;   mvMatrix[11] = 0;
        mvMatrix[12] = (float)tx;    mvMatrix[13] = (float)ty;    mvMatrix[14] = (float)tz;    mvMatrix[15] = 1;

        // Projection: perspective frustum or ortho
        double near = perspDist - clipScaling * clipFront;
        double far  = perspDist - clipScaling * clipBack;
        if(near < 1.0) near = 1.0;
        if(far <= near) far = near + 1.0;

        float[] projMatrix = new float[16];
        if(engine.usePerspective)
        {
            double right  = (width / 2.0)  * (near / perspDist);
            double top    = (height / 2.0) * (near / perspDist);
            // glFrustum-style: column-major
            projMatrix[0]  = (float)(near / right);
            projMatrix[5]  = (float)(near / top);
            projMatrix[10] = (float)(-(far + near) / (far - near));
            projMatrix[11] = -1.0f;
            projMatrix[14] = (float)(-2.0 * far * near / (far - near));
            // all other elements are 0
        }
        else
        {
            double right = width / 2.0;
            double top   = height / 2.0;
            // glOrtho-style: column-major
            projMatrix[0]  = (float)(1.0 / right);
            projMatrix[5]  = (float)(1.0 / top);
            projMatrix[10] = (float)(-2.0 / (far - near));
            projMatrix[14] = (float)(-(far + near) / (far - near));
            projMatrix[15] = 1.0f;
        }

        // MVP = Projection * ModelView
        multiplyMatrix(mvpMatrix, projMatrix, mvMatrix);

        // Fog parameters (in eye space: depth = -eyePos.z = distance from camera)
        float fogStart = (float) near;
        float fogEnd   = (float)((far - 0.36 * near) / (1.0 - 0.36));

        boolean whiteBg = engine.whiteBackground;
        float fogR = whiteBg ? 1.0f : 0.0f;
        float fogG = whiteBg ? 1.0f : 0.0f;
        float fogB = whiteBg ? 1.0f : 0.0f;

        // --- Pack geometry ---
        packGeometry(kin, engine);

        // --- GL state setup ---
        gl.glViewport(bounds.x, bounds.y, bounds.width, bounds.height);

        // Clear with background color
        gl.glClearColor(fogR, fogG, fogB, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // Use shader program
        gl.glUseProgram(program);

        // Set uniforms
        gl.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0);
        gl.glUniformMatrix4fv(uMv,  1, false, mvMatrix,  0);
        gl.glUniform3f(uFogColor, fogR, fogG, fogB);
        gl.glUniform1f(uFogStart, fogStart);
        gl.glUniform1f(uFogEnd,   fogEnd);

        // Light direction (normalized, in eye space)
        Triple lightDir = engine.lightingVector; // default: normalized(-1, 1, 3)
        gl.glUniform3f(uLightDir, (float)lightDir.getX(), (float)lightDir.getY(), (float)lightDir.getZ());

        // --- Draw dots (per-list widths) ---
        if(dotVertCount > 0)
        {
            gl.glUniform1i(uLighting, 0);
            uploadVBO(gl, vbo[1], dotVerts, dotVertCount, false);
            for(int[] cmd : dotDrawCmds)
            {
                gl.glPointSize(cmd[2] + 0.5f);
                gl.glDrawArrays(GL2.GL_POINTS, cmd[0], cmd[1]);
            }
        }

        // --- Draw lines (per-list widths) ---
        if(lineVertCount > 0)
        {
            gl.glUniform1i(uLighting, 0);
            uploadVBO(gl, vbo[0], lineVerts, lineVertCount, false);
            for(int[] cmd : lineDrawCmds)
            {
                gl.glLineWidth(cmd[2] + 0.5f);
                gl.glDrawArrays(GL2.GL_LINES, cmd[0], cmd[1]);
            }
        }

        // --- Draw triangles ---
        if(triVertCount > 0)
        {
            gl.glUniform1i(uLighting, 1);
            uploadVBO(gl, vbo[2], triVerts, triVertCount, true);
            gl.glDrawArrays(GL.GL_TRIANGLES, 0, triVertCount);
        }

        // --- Draw labels ---
        if(!labelPositions.isEmpty())
        {
            drawLabels(gl);
        }

        // Cleanup GL state
        gl.glDisableVertexAttribArray(aPosition);
        gl.glDisableVertexAttribArray(aColor);
        if(aNormal >= 0) gl.glDisableVertexAttribArray(aNormal);
        gl.glUseProgram(0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_BLEND);

        // Restore 2D ortho projection for toolbox overpaint (JoglPainter expects this)
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(0.0, width, -height, 0.0, -1.0, 1.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
//}}}

//{{{ packGeometry
//##############################################################################
    /**
    * Walks all visible lists and packs vertex data into scratch buffers.
    */
    void packGeometry(Kinemage kin, Engine2D engine)
    {
        // Reset counts
        lineVerts.clear();
        dotVerts.clear();
        triVerts.clear();
        lineVertCount = 0;
        dotVertCount  = 0;
        triVertCount  = 0;
        lineDrawCmds.clear();
        dotDrawCmds.clear();
        labelPositions.clear();
        labelTexts.clear();
        labelColors.clear();

        boolean whiteBg = engine.whiteBackground;

        for(KList list : KIterator.visibleLists(kin))
        {
            String type  = list.getType();
            int listAlpha = list.getAlpha();
            float alpha   = listAlpha / 255.0f;
            int listWidth = list.getWidth();

            if(type.equals(KList.VECTOR))
            {
                int startVert = lineVertCount;
                packVectorList(list, engine, whiteBg, alpha, listWidth);
                if(lineVertCount > startVert)
                    lineDrawCmds.add(new int[]{startVert, lineVertCount - startVert, listWidth});
            }
            else if(type.equals(KList.DOT) || type.equals("mark"))
            {
                int startVert = dotVertCount;
                packDotList(list, engine, whiteBg, alpha, listWidth);
                if(dotVertCount > startVert)
                    dotDrawCmds.add(new int[]{startVert, dotVertCount - startVert, listWidth});
            }
            else if(type.equals(KList.BALL) || type.equals("sphere"))
            {
                packBallList(list, engine, whiteBg, alpha);
            }
            else if(type.equals(KList.TRIANGLE) || type.equals(KList.RIBBON))
            {
                packTriangleList(list, engine, whiteBg, alpha);
            }
            else if(type.equals("label"))
            {
                packLabelList(list, engine, whiteBg);
            }
            else if(type.equals("arrow"))
            {
                int startVert = lineVertCount;
                packVectorList(list, engine, whiteBg, alpha, listWidth);
                if(lineVertCount > startVert)
                    lineDrawCmds.add(new int[]{startVert, lineVertCount - startVert, listWidth});
            }
            else if(type.equals("ring"))
            {
                int startVert = lineVertCount;
                packVectorList(list, engine, whiteBg, alpha, listWidth);
                if(lineVertCount > startVert)
                    lineDrawCmds.add(new int[]{startVert, lineVertCount - startVert, listWidth});
            }
        }
    }
//}}}

//{{{ packVectorList
//##############################################################################
    void packVectorList(KList list, Engine engine, boolean whiteBg, float alpha, int listWidth)
    {
        KPaint prevPaint = null;
        float pr = 0, pg = 0, pb = 0;

        for(KPoint pt : list.getChildren())
        {
            KPaint paint = pt.getDrawingColor(engine);
            if(paint.isInvisible()) continue;

            // Only draw line segments, not break points
            KPoint from = pt.getPrev();
            if(from == null) continue; // this is a break or first point

            KPaint fromPaint = from.getDrawingColor(engine);
            if(fromPaint.isInvisible()) continue;

            // Resolve color for "from" vertex
            Color fromColor = whiteBg ? fromPaint.getWhiteExemplar() : fromPaint.getBlackExemplar();
            float fr = fromColor.getRed()   / 255.0f;
            float fg = fromColor.getGreen() / 255.0f;
            float fb = fromColor.getBlue()  / 255.0f;

            // Resolve color for "to" vertex
            if(paint != prevPaint)
            {
                Color c = whiteBg ? paint.getWhiteExemplar() : paint.getBlackExemplar();
                pr = c.getRed()   / 255.0f;
                pg = c.getGreen() / 255.0f;
                pb = c.getBlue()  / 255.0f;
                prevPaint = paint;
            }

            // Ensure capacity
            ensureLineCapacity(2);

            // From vertex
            lineVerts.put((float)from.getX());
            lineVerts.put((float)from.getY());
            lineVerts.put((float)from.getZ());
            lineVerts.put(fr); lineVerts.put(fg); lineVerts.put(fb);
            lineVerts.put(alpha);

            // To vertex
            lineVerts.put((float)pt.getX());
            lineVerts.put((float)pt.getY());
            lineVerts.put((float)pt.getZ());
            lineVerts.put(pr); lineVerts.put(pg); lineVerts.put(pb);
            lineVerts.put(alpha);

            lineVertCount += 2;
        }
    }
//}}}

//{{{ packDotList
//##############################################################################
    void packDotList(KList list, Engine engine, boolean whiteBg, float alpha, int listWidth)
    {
        KPaint prevPaint = null;
        float r = 0, g = 0, b = 0;

        for(KPoint pt : list.getChildren())
        {
            KPaint paint = pt.getDrawingColor(engine);
            if(paint.isInvisible()) continue;

            if(paint != prevPaint)
            {
                Color c = whiteBg ? paint.getWhiteExemplar() : paint.getBlackExemplar();
                r = c.getRed()   / 255.0f;
                g = c.getGreen() / 255.0f;
                b = c.getBlue()  / 255.0f;
                prevPaint = paint;
            }

            ensureDotCapacity(1);

            dotVerts.put((float)pt.getX());
            dotVerts.put((float)pt.getY());
            dotVerts.put((float)pt.getZ());
            dotVerts.put(r); dotVerts.put(g); dotVerts.put(b);
            dotVerts.put(alpha);

            dotVertCount++;
        }
    }
//}}}

//{{{ packTriangleList
//##############################################################################
    void packTriangleList(KList list, Engine engine, boolean whiteBg, float alpha)
    {
        // Triangle/ribbon lists use strip format: each new point after the first two
        // forms a triangle with the previous two points.
        // Breaks restart the strip.
        KPoint from = null, fromfrom = null;
        boolean flipNormal = false;
        double prevNx = 0, prevNy = 0, prevNz = 1;

        for(KPoint pt : list.getChildren())
        {
            if(pt.isBreak())
            {
                from = fromfrom = null;
                flipNormal = false;
            }

            KPaint paint = pt.getDrawingColor(engine);
            if(paint.isInvisible()) continue;

            if(from != null && fromfrom != null)
            {
                // Compute face normal: cross(fromfrom->from, from->pt)
                double ax = from.getX() - fromfrom.getX();
                double ay = from.getY() - fromfrom.getY();
                double az = from.getZ() - fromfrom.getZ();
                double bx = pt.getX() - from.getX();
                double by = pt.getY() - from.getY();
                double bz = pt.getZ() - from.getZ();
                double nx = ay*bz - az*by;
                double ny = az*bx - ax*bz;
                double nz = ax*by - ay*bx;
                double len = Math.sqrt(nx*nx + ny*ny + nz*nz);
                if(len > 0) { nx /= len; ny /= len; nz /= len; }

                // Alternate triangles need flipped normals for consistent facing
                if(flipNormal) { nx = -nx; ny = -ny; nz = -nz; }
                prevNx = nx; prevNy = ny; prevNz = nz;

                ensureTriCapacity(3);

                // Emit three vertices for this triangle
                KPoint[] verts = {fromfrom, from, pt};
                for(KPoint v : verts)
                {
                    KPaint vp = v.getDrawingColor(engine);
                    Color c = whiteBg ? vp.getWhiteExemplar() : vp.getBlackExemplar();
                    triVerts.put((float)v.getX());
                    triVerts.put((float)v.getY());
                    triVerts.put((float)v.getZ());
                    triVerts.put((float)nx);
                    triVerts.put((float)ny);
                    triVerts.put((float)nz);
                    triVerts.put(c.getRed()   / 255.0f);
                    triVerts.put(c.getGreen() / 255.0f);
                    triVerts.put(c.getBlue()  / 255.0f);
                    triVerts.put(alpha);
                }
                triVertCount += 3;
            }

            fromfrom = from;
            from = pt;
            flipNormal = !flipNormal;
        }
    }
//}}}

//{{{ packLabelList
//##############################################################################
    void packLabelList(KList list, Engine engine, boolean whiteBg)
    {
        for(KPoint pt : list.getChildren())
        {
            KPaint paint = pt.getDrawingColor(engine);
            if(paint.isInvisible()) continue;

            Color c = whiteBg ? paint.getWhiteExemplar() : paint.getBlackExemplar();
            labelPositions.add(new float[]{(float)pt.getX(), (float)pt.getY(), (float)pt.getZ()});
            labelTexts.add(pt.getName());
            labelColors.add(new float[]{c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f});
        }
    }
//}}}

//{{{ packBallList
//##############################################################################
    void packBallList(KList list, Engine engine, boolean whiteBg, float alpha)
    {
        float listRadius = list.getRadius();

        for(KPoint pt : list.getChildren())
        {
            KPaint paint = pt.getDrawingColor(engine);
            if(paint.isInvisible()) continue;

            Color c = whiteBg ? paint.getWhiteExemplar() : paint.getBlackExemplar();
            float r = c.getRed()   / 255.0f;
            float g = c.getGreen() / 255.0f;
            float b = c.getBlue()  / 255.0f;

            float radius = pt.getRadius();
            if(radius == 0) radius = listRadius;
            float px = (float)pt.getX();
            float py = (float)pt.getY();
            float pz = (float)pt.getZ();

            // Pack sphere mesh triangles, scaled and translated to this ball
            ensureTriCapacity(sphereTris * 3);

            for(int t = 0; t < sphereTris; t++)
            {
                int base = t * 9;
                for(int v = 0; v < 3; v++)
                {
                    int vi = base + v * 3;
                    float nx = sphereMesh[vi];
                    float ny = sphereMesh[vi + 1];
                    float nz = sphereMesh[vi + 2];
                    // Position = center + radius * normal (unit sphere vertex = normal)
                    triVerts.put(px + radius * nx);
                    triVerts.put(py + radius * ny);
                    triVerts.put(pz + radius * nz);
                    triVerts.put(nx);
                    triVerts.put(ny);
                    triVerts.put(nz);
                    triVerts.put(r); triVerts.put(g); triVerts.put(b);
                    triVerts.put(alpha);
                }
            }
            triVertCount += sphereTris * 3;
        }
    }
//}}}

//{{{ buildSphereMesh
//##############################################################################
    /**
    * Builds a unit sphere mesh from a subdivided icosahedron.
    * Stores result in sphereMesh[] as flat triangle data: 9 floats per tri (3 vertices x 3 coords).
    * For a unit sphere, vertex positions ARE the normals.
    * @param depth 0=20 tris, 1=80 tris, 2=320 tris
    */
    void buildSphereMesh(int depth)
    {
        // Icosahedron vertices (from OpenGL Programming Guide, Ch. 2)
        final double x = 0.525731112119133606;
        final double z = 0.850650808352039932;
        double[][] verts = {
            {-x,0,z}, {x,0,z}, {-x,0,-z}, {x,0,-z},
            {0,z,x}, {0,z,-x}, {0,-z,x}, {0,-z,-x},
            {z,x,0}, {-z,x,0}, {z,-x,0}, {-z,-x,0}
        };
        int[][] faces = {
            {0,4,1}, {0,9,4}, {9,5,4}, {4,5,8}, {4,8,1},
            {8,10,1}, {8,3,10}, {5,3,8}, {5,2,3}, {2,7,3},
            {7,10,3}, {7,6,10}, {7,11,6}, {11,0,6}, {0,1,6},
            {6,1,10}, {9,0,11}, {9,11,2}, {9,2,5}, {7,2,11}
        };

        // Collect all triangles via recursive subdivision
        ArrayList<float[]> tris = new ArrayList<float[]>();
        for(int[] face : faces)
        {
            subdivideFace(
                verts[face[0]], verts[face[1]], verts[face[2]],
                depth, tris);
        }

        // Flatten into array
        sphereTris = tris.size();
        sphereMesh = new float[sphereTris * 9];
        for(int i = 0; i < sphereTris; i++)
        {
            float[] tri = tris.get(i);
            System.arraycopy(tri, 0, sphereMesh, i * 9, 9);
        }
    }

    /** Recursively subdivides a triangle on the unit sphere. */
    static void subdivideFace(double[] v1, double[] v2, double[] v3, int depth, ArrayList<float[]> out)
    {
        if(depth == 0)
        {
            out.add(new float[]{
                (float)v1[0], (float)v1[1], (float)v1[2],
                (float)v2[0], (float)v2[1], (float)v2[2],
                (float)v3[0], (float)v3[1], (float)v3[2]
            });
        }
        else
        {
            double[] v12 = normalize(mid(v1, v2));
            double[] v23 = normalize(mid(v2, v3));
            double[] v31 = normalize(mid(v3, v1));
            depth--;
            subdivideFace(v1, v12, v31, depth, out);
            subdivideFace(v2, v23, v12, depth, out);
            subdivideFace(v3, v31, v23, depth, out);
            subdivideFace(v12, v23, v31, depth, out);
        }
    }

    static double[] mid(double[] a, double[] b)
    { return new double[]{(a[0]+b[0])/2, (a[1]+b[1])/2, (a[2]+b[2])/2}; }

    static double[] normalize(double[] v)
    {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        return new double[]{v[0]/len, v[1]/len, v[2]/len};
    }
//}}}

//{{{ uploadVBO
//##############################################################################
    /**
    * Uploads vertex data to a VBO and sets up vertex attribute pointers.
    * Caller is responsible for issuing glDrawArrays() calls afterward.
    * @param gl         the GL context
    * @param vboHandle  the VBO handle to use
    * @param buffer     the vertex data buffer
    * @param vertCount  number of vertices
    * @param hasNormals true if vertex format includes normals (10 floats/vert)
    */
    void uploadVBO(GL2 gl, int vboHandle, FloatBuffer buffer, int vertCount, boolean hasNormals)
    {
        buffer.flip();
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboHandle);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (long)buffer.limit() * 4, buffer, GL2.GL_STREAM_DRAW);

        if(hasNormals)
        {
            // Stride = 10 floats = 40 bytes
            int stride = FLOATS_PER_TRI_VERT * 4;
            gl.glEnableVertexAttribArray(aPosition);
            gl.glVertexAttribPointer(aPosition, 3, GL.GL_FLOAT, false, stride, 0);
            if(aNormal >= 0)
            {
                gl.glEnableVertexAttribArray(aNormal);
                gl.glVertexAttribPointer(aNormal, 3, GL.GL_FLOAT, false, stride, 3 * 4);
            }
            gl.glEnableVertexAttribArray(aColor);
            gl.glVertexAttribPointer(aColor, 4, GL.GL_FLOAT, false, stride, 6 * 4);
        }
        else
        {
            // Stride = 7 floats = 28 bytes
            int stride = FLOATS_PER_VERT * 4;
            gl.glEnableVertexAttribArray(aPosition);
            gl.glVertexAttribPointer(aPosition, 3, GL.GL_FLOAT, false, stride, 0);
            gl.glEnableVertexAttribArray(aColor);
            gl.glVertexAttribPointer(aColor, 4, GL.GL_FLOAT, false, stride, 3 * 4);
            // Set default normal for non-triangle geometry
            if(aNormal >= 0)
            {
                gl.glDisableVertexAttribArray(aNormal);
                gl.glVertexAttrib3f(aNormal, 0, 0, 1);
            }
        }
    }
//}}}

//{{{ drawLabels
//##############################################################################
    /**
    * Draws text labels using fixed-function GL and GLUT bitmap strings.
    * Labels are drawn after all 3D geometry with depth test disabled.
    */
    void drawLabels(GL2 gl)
    {
        // Switch to fixed-function for label drawing
        gl.glUseProgram(0);

        // Load our matrices into the fixed-function pipeline
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        // We'll use the same MVP by loading projection as identity
        // and modelview as MVP... or better, load them separately.
        gl.glLoadMatrixf(mvpMatrix, 0);
        // Actually, MVP = P * MV, so to split: set projection to P and modelview to MV.
        // But we built MVP as one matrix. Let's just put MVP in projection and identity in MV.
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Disable depth test so labels are always visible
        gl.glDisable(GL.GL_DEPTH_TEST);

        for(int i = 0; i < labelPositions.size(); i++)
        {
            float[] pos = labelPositions.get(i);
            float[] col = labelColors.get(i);
            String text = labelTexts.get(i);

            gl.glColor3f(col[0], col[1], col[2]);
            gl.glRasterPos3f(pos[0], pos[1], pos[2]);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, text);
        }

        // Re-enable depth test and switch back to shader
        gl.glEnable(GL.GL_DEPTH_TEST);
    }
//}}}

//{{{ ensureLineCapacity, ensureDotCapacity, ensureTriCapacity
//##############################################################################
    void ensureLineCapacity(int additionalVerts)
    {
        int needed = (lineVertCount + additionalVerts) * FLOATS_PER_VERT;
        if(needed > lineVerts.capacity())
        {
            int newCap = Math.max(lineVerts.capacity() * 2, needed);
            FloatBuffer newBuf = Buffers.newDirectFloatBuffer(newCap);
            lineVerts.flip();
            newBuf.put(lineVerts);
            lineVerts = newBuf;
        }
    }

    void ensureDotCapacity(int additionalVerts)
    {
        int needed = (dotVertCount + additionalVerts) * FLOATS_PER_VERT;
        if(needed > dotVerts.capacity())
        {
            int newCap = Math.max(dotVerts.capacity() * 2, needed);
            FloatBuffer newBuf = Buffers.newDirectFloatBuffer(newCap);
            dotVerts.flip();
            newBuf.put(dotVerts);
            dotVerts = newBuf;
        }
    }

    void ensureTriCapacity(int additionalVerts)
    {
        int needed = (triVertCount + additionalVerts) * FLOATS_PER_TRI_VERT;
        if(needed > triVerts.capacity())
        {
            int newCap = Math.max(triVerts.capacity() * 2, needed);
            FloatBuffer newBuf = Buffers.newDirectFloatBuffer(newCap);
            triVerts.flip();
            newBuf.put(triVerts);
            triVerts = newBuf;
        }
    }
//}}}

//{{{ multiplyMatrix
//##############################################################################
    /**
    * Multiplies two 4x4 column-major matrices: result = a * b.
    */
    static void multiplyMatrix(float[] result, float[] a, float[] b)
    {
        for(int col = 0; col < 4; col++)
        {
            for(int row = 0; row < 4; row++)
            {
                float sum = 0;
                for(int k = 0; k < 4; k++)
                {
                    sum += a[k*4 + row] * b[col*4 + k];
                }
                result[col*4 + row] = sum;
            }
        }
    }
//}}}

}//class
