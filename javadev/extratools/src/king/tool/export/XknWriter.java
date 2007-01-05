// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.export;
import king.*;
import king.core.*;
import king.points.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.text.*;
import java.util.*;
//import java.util.regex.*;
import javax.swing.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;

// XML stuff -- not 1.3 compatible unless JAXP is provided as a JAR.
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;
//}}}
/**
 * <code>XknWriter</code> writes out XML-format kinemage files.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Thu Oct  3 09:51:11 EDT 2002
*/
public class XknWriter extends Plugin implements XMLReader
{
//{{{ Constants
    static final DecimalFormat df = new DecimalFormat("0.####");
    static final char[] newline = "\n".toCharArray();
//}}}

//{{{ Variable definitions
//##################################################################################################
    ContentHandler cnHandler = null;
    LexicalHandler lxHandler = null;
    ErrorHandler errHandler = null;
    String nsu = "";
    
    JFileChooser fileChooser;
//}}}

//{{{ Constructor(s)
//##################################################################################################
    /**
    * Constructor
    */
    public XknWriter(ToolBox tb)
    {
        super(tb);
        fileChooser = new JFileChooser();
        String currdir = System.getProperty("user.dir");
        if(currdir != null) fileChooser.setCurrentDirectory(new File(currdir));
    }
//}}}

//{{{ save()
//##################################################################################################
    /** Writes out all the currently open kinemages */
    public void save(OutputStream os)
    {
        try
        {
            BufferedOutputStream bos = new BufferedOutputStream(os);
            
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
        
            // Empty InputSource is OK, but it can't be null for some reason.
            SAXSource source = new SAXSource(this, new InputSource());
            StreamResult result = new StreamResult(bos);
            transformer.transform(source, result); // calls parse()
        }
        catch(Throwable t)
        {
            System.err.println("*** Couldn't transform!");
            t.printStackTrace(SoftLog.err);
        }
    }
//}}}

//{{{ parse()
//##################################################################################################
    public void parse(InputSource input) throws IOException, SAXException
    {
        if(cnHandler == null) throw new SAXException("No content handler");
        
        // Begin the document
        String elName = "kinemages";
        AttributesImpl atts = new AttributesImpl();
        //atts.addAttribute(nsu, "x", "x", "CDATA", "this_is_x");
        cnHandler.startDocument();
        cnHandler.startElement(nsu, elName, elName, atts);
        
        // Write out text
        atts = new AttributesImpl();
        atts.addAttribute(nsu, "mimetype", "mimetype", "CDATA", "text/plain");
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, "text", "text", atts);
        if(lxHandler != null) lxHandler.startCDATA();
        String kinText = kMain.getTextWindow().getText().trim();
        cnHandler.characters(kinText.toCharArray(), 0, kinText.length());
        if(lxHandler != null) lxHandler.endCDATA();
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, "text", "text");
        
        // Write each kinemage
        KinStable   stable  = kMain.getStable();
        Kinemage    kin;
        int         index   = 1;
        
        for(Iterator iter = stable.iterator(); iter.hasNext(); index++)
        {
            kin = (Kinemage)iter.next();
            writeKinemage(kin, index);
        }
        
        // End the document
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endDocument();
    }
//}}}

//{{{ writeKinemage()
//##################################################################################################
    void writeKinemage(Kinemage kin, int index) throws SAXException
    {
        // Begin the element
        String elName = "kinemage";
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, "index", "index", "CDATA", Integer.toString(index));
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        
        Iterator iter;
        
        int idx = 1;
        for(iter = kin.getViewIterator(); iter.hasNext(); idx++)
        {
            writeView((KView)iter.next(), idx);
        }
        
        for(iter = kin.masterList().iterator(); iter.hasNext(); )
        {
            writeMaster((MasterGroup)iter.next(), kin);
        }
        
        // etc.
        
        KGroup group;
        for(iter = kin.iterator(); iter.hasNext(); )
        {
            group = (KGroup)iter.next();
            writeGroup(group, kin);
        }
        
        // End the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }
//}}}

//{{{ writeGroup()
//##################################################################################################
    void writeGroup(KGroup group, Kinemage kin) throws SAXException
    {
        Iterator iter;

        String elName = "group";
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, "name", "name", "CDATA", group.getName());
        if(! group.isOn())          atts.addAttribute(nsu, "off", "off", "CDATA", "true");
        if(! group.hasButton())     atts.addAttribute(nsu, "nobutton", "nobutton", "CDATA", "true");
        if(  group.isDominant())    atts.addAttribute(nsu, "dominant", "dominant", "CDATA", "true");
        if(  group.isAnimate())     atts.addAttribute(nsu, "animate", "animate", "CDATA", "true");
        if(  group.is2Animate())    atts.addAttribute(nsu, "2animate", "2animate", "CDATA", "true");
        
        // Begin the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        
        /*MasterGroup master;
        for(iter = kin.masterIter(); iter.hasNext(); )
        {
            master = (MasterGroup)iter.next();
            if(master.isTarget(group)) writeMasterReference(master);
        }*/
        for(iter = group.masterIterator(); iter != null && iter.hasNext(); )
        {
            writeMasterReference(iter.next().toString());
        }
        
        KSubgroup subgroup;
        for(iter = group.iterator(); iter.hasNext(); )
        {
            subgroup = (KSubgroup)iter.next();
            writeSubgroup(subgroup, kin);
        }

        // End the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }
//}}}

//{{{ writeSubgroup()
//##################################################################################################
    void writeSubgroup(KGroup subgroup, Kinemage kin) throws SAXException
    {
        Iterator iter;

        String elName = "subgroup";
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, "name", "name", "CDATA", subgroup.getName());
        if(! subgroup.isOn())       atts.addAttribute(nsu, "off", "off", "CDATA", "true");
        if(! subgroup.hasButton())  atts.addAttribute(nsu, "nobutton", "nobutton", "CDATA", "true");
        if(  subgroup.isDominant()) atts.addAttribute(nsu, "dominant", "dominant", "CDATA", "true");
        
        // Begin the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        
        /*MasterGroup master;
        for(iter = kin.masterIter(); iter.hasNext(); )
        {
            master = (MasterGroup)iter.next();
            if(master.isTarget(subgroup)) writeMasterReference(master);
        }*/
        for(iter = subgroup.masterIterator(); iter != null && iter.hasNext(); )
        {
            writeMasterReference(iter.next().toString());
        }
        
        KList list;
        for(iter = subgroup.iterator(); iter.hasNext(); )
        {
            list = (KList)iter.next();
            writeList(list, kin);
        }

        // End the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }
//}}}

//{{{ writeList()
//##################################################################################################
    void writeList(KList list, Kinemage kin) throws SAXException
    {
        Iterator iter;

        String elName = "list";
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, "type", "type", "CDATA", list.getType());
        atts.addAttribute(nsu, "name", "name", "CDATA", list.getName());
        if(! list.isOn())       atts.addAttribute(nsu, "off", "off", "CDATA", "true");
        if(! list.hasButton())  atts.addAttribute(nsu, "nobutton", "nobutton", "CDATA", "true");
        atts.addAttribute(nsu, "color", "color", "CDATA", list.getColor().toString());
        if(list.getType() == KList.VECTOR)
        { atts.addAttribute(nsu, "width", "width", "CDATA", Integer.toString(list.width)); }
        else if(list.getType() == KList.BALL || list.getType() == KList.SPHERE)
        {
            atts.addAttribute(nsu, "radius", "radius", "CDATA", df.format(list.radius));
            if((list.flags & KList.NOHILITE) != 0)
            { atts.addAttribute(nsu, "highlight", "highlight", "CDATA", "false"); }
        }

        // Begin the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        
        /*MasterGroup master;
        for(iter = kin.masterIter(); iter.hasNext(); )
        {
            master = (MasterGroup)iter.next();
            if(master.isTarget(list)) writeMasterReference(master);
        }*/
        for(iter = list.masterIterator(); iter != null && iter.hasNext(); )
        {
            writeMasterReference(iter.next().toString());
        }
        
        KPoint point;
        for(iter = list.iterator(); iter.hasNext(); )
        {
            point = (KPoint)iter.next();
            writePoint(point, list, kin);
        }

        // End the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }
//}}}

//{{{ writePoint()
//##################################################################################################
    void writePoint(KPoint point, KList list, Kinemage kin) throws SAXException
    {
        Iterator iter;

        String elName = "t";
        AttributesImpl atts = new AttributesImpl();
        if(point.isBreak())             atts.addAttribute(nsu, "lineto", "lineto", "CDATA", "false");
        atts.addAttribute(nsu, "name", "name", "CDATA", point.getName());
        if(point.getPmMask() != 0)      atts.addAttribute(nsu, "masters", "masters", "CDATA", kin.fromPmBitmask(point.getPmMask()));
        if(point.getAspects() != null)  atts.addAttribute(nsu, "aspects", "aspects", "CDATA", point.getAspects());
        if(point.isUnpickable())        atts.addAttribute(nsu, "pickable", "pickable", "CDATA", "false");
        if(point instanceof VectorPoint)
        {
            VectorPoint v = (VectorPoint)point;
            if(v.getWidth() > 0 && v.getWidth() != list.width)
            { atts.addAttribute(nsu, "width", "width", "CDATA", Integer.toString(v.getWidth())); }
        }
        else if(point instanceof BallPoint)
        {
            BallPoint b = (BallPoint)point;
            { atts.addAttribute(nsu, "radius", "radius", "CDATA", df.format(b.r0)); }
        }
        else if(point instanceof MarkerPoint)
        {
            MarkerPoint m = (MarkerPoint)point;
            atts.addAttribute(nsu, "style", "style", "CDATA", Integer.toString(m.getStyle()));
        }
        if(point.getColor() != null)
        { atts.addAttribute(nsu, "color", "color", "CDATA", point.getColor().toString()); }
        atts.addAttribute(nsu, "x", "x", "CDATA", df.format(point.getX()));
        atts.addAttribute(nsu, "y", "y", "CDATA", df.format(point.getY()));
        atts.addAttribute(nsu, "z", "z", "CDATA", df.format(point.getZ()));

        // Begin the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        // End the element
        //cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }
//}}}

//{{{ writeView()
//##################################################################################################
    void writeView(KView view, int index) throws SAXException
    {
        String elName = "view";
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, "name", "name", "CDATA", view.getName());
        atts.addAttribute(nsu, "index", "index", "CDATA", Integer.toString(index));
        float[] center = view.getCenter();
        atts.addAttribute(nsu, "x", "x", "CDATA", df.format(center[0]));
        atts.addAttribute(nsu, "y", "y", "CDATA", df.format(center[1]));
        atts.addAttribute(nsu, "z", "z", "CDATA", df.format(center[2]));
        atts.addAttribute(nsu, "span", "span", "CDATA", df.format(view.getSpan()));
        atts.addAttribute(nsu, "zslab", "zslab", "CDATA", df.format(view.getClip()*200f));
        // Writen out Mage-style, for a post-multiplied matrix
        for(int i = 1; i < 4; i++)
        {
            for(int j = 1; j < 4; j++)
            { atts.addAttribute(nsu, "m"+i+j, "m"+i+j, "CDATA", df.format(view.xform[j-1][i-1])); }
        }

        // Begin the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        // End the element
        //cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }
//}}}

//{{{ writeMaster(Reference)()
//##################################################################################################
    void writeMaster(MasterGroup master, Kinemage kin) throws SAXException
    {
        String elName = "master";
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, "name", "name", "CDATA", master.getName());
        if(master.pm_mask != 0)
        { atts.addAttribute(nsu, "pointmaster", "pointmaster", "CDATA", kin.fromPmBitmask(master.pm_mask)); }
        if(! master.isOn())         atts.addAttribute(nsu, "off", "off", "CDATA", "true");
        if(! master.hasButton())    atts.addAttribute(nsu, "nobutton", "nobutton", "CDATA", "true");
        
        // Begin the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        // End the element
        //cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }

    void writeMasterReference(String master) throws SAXException
    {
        String elName = "master";
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(nsu, "name", "name", "CDATA", master);
        
        // Begin the element
        cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.startElement(nsu, elName, elName, atts);
        // End the element
        //cnHandler.ignorableWhitespace(newline, 0, 1);
        cnHandler.endElement(nsu, elName, elName);
    }
///}}}

//{{{ get/set ContentHandler
//##################################################################################################
    public ContentHandler getContentHandler()
    {
        return cnHandler;
    }

    public void setContentHandler(ContentHandler h)
    {
        cnHandler = h;
    }
//}}}

//{{{ get/set ErrorHandler
//##################################################################################################
    public ErrorHandler getErrorHandler()
    {
        return errHandler;
    }

    public void setErrorHandler(ErrorHandler h)
    {
        errHandler = h;
    }
//}}}

//{{{ get/set Property
//##################################################################################################
    public Object getProperty(String name)
    {
        if(( name.equals("http://xml.org/sax/properties/lexical-handler")
          || name.equals("http://xml.org/sax/handlers/LexicalHandler")))
        {
            return lxHandler;
        }
        else return null;
    }
    
    public void setProperty(String name, Object value)
    {
        if(( name.equals("http://xml.org/sax/properties/lexical-handler")
          || name.equals("http://xml.org/sax/handlers/LexicalHandler"))
          && value instanceof LexicalHandler)
        {
            lxHandler = (LexicalHandler)value;
        }
    }
//}}}

//{{{ other null methods for XMLReader
//##################################################################################################
    public void parse(String systemID) throws IOException, SAXException
    {}
    public DTDHandler getDTDHandler()
    { return null; }
    public void setDTDHandler(DTDHandler h)
    {}
    public EntityResolver getEntityResolver()
    { return null; }
    public void setEntityResolver(EntityResolver e)
    {}
    public boolean getFeature(String name)
    { return false; }
    public void setFeature(String name, boolean value)
    {}
//}}}

//{{{ getToolsMenuItem, getHelpMenuItem, toString, onExport, isAppletSafe
//##################################################################################################
    public JMenuItem getToolsMenuItem()
    {
        return new JMenuItem(new ReflectiveAction(this.toString()+"...", null, this, "onExport"));
    }

    /** Returns the URL of a web page explaining use of this tool */
    public URL getHelpURL()
    {
        URL     url     = getClass().getResource("/extratools/tools-manual.html");
        String  anchor  = getHelpAnchor();
        if(url != null && anchor != null)
        {
            try { url = new URL(url, anchor); }
            catch(MalformedURLException ex) { ex.printStackTrace(SoftLog.err); }
            return url;
        }
        else return null;
    }
    
    public String getHelpAnchor()
    { return "#export-xml"; }

    public String toString()
    { return "XML"; }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onExport(ActionEvent ev)
    {
        if(fileChooser.showSaveDialog(kMain.getTopWindow()) == fileChooser.APPROVE_OPTION)
        {
            File f = fileChooser.getSelectedFile();
            if( !f.exists() ||
                JOptionPane.showConfirmDialog(kMain.getTopWindow(),
                    "This file exists -- do you want to overwrite it?",
                    "Overwrite file?", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION )
            {
                try
                {
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                    this.save(os);
                    os.close();
                }
                catch(IOException ex)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "An error occurred while saving the file.",
                        "Sorry!",
                        JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace(SoftLog.err);
                }
                catch(Throwable t)
                {
                    JOptionPane.showMessageDialog(kMain.getTopWindow(),
                        "Your version of Java appears to be missing the needed XML libraries. File was not written.",
                        "Sorry!",
                        JOptionPane.ERROR_MESSAGE);
                    t.printStackTrace(SoftLog.err);
                }
            }
        }
    }

    static public boolean isAppletSafe()
    { return false; }
//}}}

//{{{ empty_code_segment
//##################################################################################################
//}}}
}//class

