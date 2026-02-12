// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.painters;

import com.jogamp.opengl.*;
//}}}
/**
* <code>JoglShaders</code> provides GLSL shader compilation utilities
* and embedded shader source for the VBO-based JoglRenderer.
*
* <p>All shaders target GLSL 1.20 for GL2 profile compatibility.
*
* <p>Copyright (C) 2026 by Vincent B. Chen. All rights reserved.
* <br>Begun on Wed Feb 11 2026
*/
public class JoglShaders
{
//{{{ Shader source strings

    /** Vertex shader: transforms by MVP, passes color and eye-space depth to fragment shader. */
    static final String VERT_SRC =
        "#version 120\n" +
        "uniform mat4 u_mvp;\n" +
        "uniform mat4 u_mv;\n" +
        "uniform int u_lighting;\n" +
        "uniform vec3 u_lightDir;\n" +
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec3 a_normal;\n" +
        "varying vec4 v_color;\n" +
        "varying float v_eyeDepth;\n" +
        "void main() {\n" +
        "    gl_Position = u_mvp * a_position;\n" +
        "    vec4 eyePos = u_mv * a_position;\n" +
        "    v_eyeDepth = -eyePos.z;\n" +
        "    v_color = a_color;\n" +
        "    if (u_lighting != 0) {\n" +
        "        mat3 normalMat = mat3(u_mv);\n" +
        "        vec3 n = normalize(normalMat * a_normal);\n" +
        "        float ndotl = abs(dot(n, u_lightDir));\n" +
        "        float light = 0.4 + 0.6 * ndotl;\n" +
        "        v_color = vec4(a_color.rgb * light, a_color.a);\n" +
        "    }\n" +
        "}\n";

    /** Fragment shader: applies linear fog for depth cueing. */
    static final String FRAG_SRC =
        "#version 120\n" +
        "uniform vec3 u_fogColor;\n" +
        "uniform float u_fogStart;\n" +
        "uniform float u_fogEnd;\n" +
        "varying vec4 v_color;\n" +
        "varying float v_eyeDepth;\n" +
        "void main() {\n" +
        "    float fogFactor = clamp((u_fogEnd - v_eyeDepth) / (u_fogEnd - u_fogStart), 0.0, 1.0);\n" +
        "    vec3 color = mix(u_fogColor, v_color.rgb, fogFactor);\n" +
        "    gl_FragColor = vec4(color, v_color.a);\n" +
        "}\n";

//}}}

//{{{ compileProgram
//##############################################################################
    /**
    * Compiles and links the standard shader program.
    * @return the GL program handle, or 0 on failure
    */
    static int compileProgram(GL2 gl)
    {
        int vert = compileShader(gl, GL2.GL_VERTEX_SHADER, VERT_SRC);
        if(vert == 0) return 0;

        int frag = compileShader(gl, GL2.GL_FRAGMENT_SHADER, FRAG_SRC);
        if(frag == 0)
        {
            gl.glDeleteShader(vert);
            return 0;
        }

        int prog = gl.glCreateProgram();
        gl.glAttachShader(prog, vert);
        gl.glAttachShader(prog, frag);
        gl.glLinkProgram(prog);

        // Check link status
        int[] status = new int[1];
        gl.glGetProgramiv(prog, GL2.GL_LINK_STATUS, status, 0);
        if(status[0] == GL.GL_FALSE)
        {
            int[] len = new int[1];
            gl.glGetProgramiv(prog, GL2.GL_INFO_LOG_LENGTH, len, 0);
            byte[] log = new byte[len[0]];
            gl.glGetProgramInfoLog(prog, len[0], len, 0, log, 0);
            System.err.println("JoglShaders: program link error: " + new String(log, 0, len[0]));
            gl.glDeleteProgram(prog);
            gl.glDeleteShader(vert);
            gl.glDeleteShader(frag);
            return 0;
        }

        // Shaders can be detached after linking
        gl.glDetachShader(prog, vert);
        gl.glDetachShader(prog, frag);
        gl.glDeleteShader(vert);
        gl.glDeleteShader(frag);

        return prog;
    }
//}}}

//{{{ compileShader
//##############################################################################
    private static int compileShader(GL2 gl, int type, String source)
    {
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[]{source}, null);
        gl.glCompileShader(shader);

        int[] status = new int[1];
        gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, status, 0);
        if(status[0] == GL.GL_FALSE)
        {
            int[] len = new int[1];
            gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, len, 0);
            byte[] log = new byte[len[0]];
            gl.glGetShaderInfoLog(shader, len[0], len, 0, log, 0);
            String typeStr = (type == GL2.GL_VERTEX_SHADER) ? "vertex" : "fragment";
            System.err.println("JoglShaders: " + typeStr + " shader compile error: " + new String(log, 0, len[0]));
            gl.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }
//}}}
}//class
