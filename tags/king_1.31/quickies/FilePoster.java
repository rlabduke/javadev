// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
//package ;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>FilePoster</code> sends a file to a URL as a POST with
* fileName=name&fileContents=contents
*
* <p>PHP code to interpret this, on the other end:<pre>
    echo "File Name: ".$_REQUEST['fileName'];
    $h = fopen($_REQUEST['fileName'], 'wb');
    fwrite($h, $_REQUEST['fileContents']);
    fclose($h);
</pre>
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed Sep  1 10:59:20 EDT 2004
*/
public class FilePoster //extends ... implements ...
{
    public static void main(String[] args) throws Exception
    {
        String s;
        int b;
        String hex = "0123456789ABCDEF";

        File file = new File(args[0]);
        String fileHeader = "fileName="+URLEncoder.encode(file.getName())+"&"+"fileContents=";
        
        URL url = new URL(args[1]);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        // This is nice for maximum web-server / java-client compatibility,
        // but is technically optional.
        //conn.setRequestProperty("Content-Length", encData.length());
        conn.setRequestProperty("User-Agent", "Mozilla/4.0");
        conn.setRequestMethod("POST");
        
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());
        out.writeBytes(fileHeader);
        
        // Java's urlencode methods are stupid, b/c they expect only Strings.
        // Everything gets translated into UTF-8, which breaks binary files.
        // So I do it myself, based on the java.net.URLEncoder description.
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        while((b = in.read()) != -1)
        {
            if(('a' <= b && b <= 'z')
            || ('A' <= b && b <= 'Z')
            || ('0' <= b && b <= '9')
            || b == '.' || b == '-' || b == '*' || b == '_')
                out.write(b);
            else if(b == ' ')
                out.write('+');
            else
            {
                out.write('%');
                out.write(hex.charAt( (b>>4) & 0x0F ));
                out.write(hex.charAt( (b   ) & 0x0F ));
            }
        }
        out.flush();
        out.close();
        in.close();
        
        BufferedReader response = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while((s = response.readLine()) != null)
            System.out.println(s);
        response.close();
    }
}//class

