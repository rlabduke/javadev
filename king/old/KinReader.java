// (jEdit options) :folding=explicit:collapseFolds=1:
package king;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;

/**
 * <code>KinReader</code> reads in kinemage files and builds up a data structure from them.
 *
 * <p>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
 * <br>Begun on Thu May  2 10:34:36 EDT 2002
 */
public class KinReader implements ActionListener
{
//{{{ Variables
//##################################################################################################
    KingMain kMain = null;
    Thread openThread = null;
    
    Collection openURLs  = null;
    Kinemage   openKin   = null;
    String     openError = null;
    
    java.util.List kinList  = null;
    Kinemage    currKin     = null;
    KGroup      currGroup   = null;
    KSubgroup   currSub     = null;
    KList       currList    = null;

    PositionReader source = null;
    KinParser token = null;
    
    // Init'd in constructor
    JDialog progDialog = null;
    JProgressBar progBar = null;
    javax.swing.Timer progTimer = null;
    
    int kinCount = 1; // number of kinemages created so far
    URL lastRead = null;
    boolean ignoreAtkinemage = false;
    long time = 0;
//}}}

//{{{ Constructor    
//##################################################################################################
    /**
    * Constructor
    */
    public KinReader(KingMain kmain)
    {
        kMain = kmain;
        
        // Build the progress display dialog...
        progBar = new JProgressBar();
        progBar.setStringPainted(true); // shows % complete
        progBar.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        // If this isn't included, progDialog doesn't paint (bug in Java 1.4.0) (reported)
        JLabel labelNote = new JLabel("Loading kinemage(s)...");
        labelNote.setHorizontalAlignment(JLabel.CENTER);
        labelNote.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        progDialog = new JDialog(kMain.getMainWindow(), "", true); // true => modal
        progDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progDialog.getContentPane().add(progBar, BorderLayout.SOUTH);
        progDialog.getContentPane().add(labelNote, BorderLayout.NORTH);
        
        progTimer = new javax.swing.Timer(1000, this); // update every 1000ms
    }
//}}}

//{{{ Constructor (test)
//##################################################################################################
    /**
    * Constructor
    */
    public KinReader()
    {
    }
//}}}

//{{{ notifyChange, get/set functions
//##################################################################################################
    // Called by KingMain when something happens.
    // Shouldn't be called directly under normal circumstances.
    void notifyChange(int event_mask)
    {
        // Take care of yourself
        if((event_mask & kMain.EM_CLOSEALL) != 0) kinCount = 1;
    }
    
    /** Returns the URL of the last kin file sucessfully opened */
    public URL getLastRead() { return lastRead; }
//}}}

//{{{ open() functions
//##################################################################################################
    /**
    * Convenience function for opening one file.
    * Reads one or more kinemages from the specified resource.
    * THIS METHOD IS NOT THREAD SAFE and <b>must</b> be called from the event-dispatching thread.
    * @param url the URL to read from
    * @param kin the kinemage to append to, or null to create new kinemages
    */
    public void open(URL url, Kinemage kin)
    {
        ArrayList urls = new ArrayList();
        urls.add(url);
        open(urls, kin);
    }

    /**
    * Reads one or more kinemages from the specified resources.
    * THIS METHOD IS NOT THREAD SAFE and <b>must</b> be called from the event-dispatching thread.
    * @param urls a collection of one or more URLs to read from
    * @param kin the kinemage to merge with, or null to create new kinemages
    */
    public void open(Collection urls, Kinemage kin)
    {
        // fail if open operation is already in progress
        if(openThread != null && openThread.isAlive()) return;
        
        openURLs  = urls;
        openKin   = kin;
        openError = null;
        
        // Protect data structure while we're working
        if(kin == null) ignoreAtkinemage = false;
        else
        {
            ignoreAtkinemage = true;
            kMain.getStable().setLocked(true);
        }

        progBar.setMaximum(1000);
        progBar.setValue(0);
        progTimer.start();
        progDialog.pack();
        progDialog.setLocationRelativeTo(kMain.getMainWindow()); // centers dialog 
        
        openThread = new Thread(new ReflectiveRunnable(this, "loadURLs"));
        try { openThread.setDaemon(true); } catch(SecurityException ex) {}
        openThread.start();
        
        progDialog.setVisible(true);
        // Blocks execution -- we won't get here until installer calls dispose() on the dialog!
    }
//}}}

//{{{ loadURLs() -- the reading function, runs in a background thread
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /**
    * Runs in a background thread, calling readFile for each URL to be read.
    * Not to be called by the general public.
    */
    public void loadURLs()
    {
        kinList = new ArrayList();
        
        URL url;
        URLConnection conn;
        InputStream is;
        
        // Iterate over all URLs
        for(Iterator iter = openURLs.iterator(); iter.hasNext(); )
        {
            url = (URL)iter.next();
            
            // set up data structures
            currKin   = openKin;
            currGroup = null;
            currSub   = null;
            currList  = null;
        
            // set up connection and data source
            try
            {
                conn = url.openConnection();
                conn.setAllowUserInteraction(false);
                conn.connect();
                is = conn.getInputStream();
                
                // Test for GZIPped files
                is = new BufferedInputStream(is);
                is.mark(10);
                if(is.read() == 31 && is.read() == 139)
                {
                    // We've found the gzip magic numbers...
                    is.reset();
                    is = new GZIPInputStream(is);
                }
                else is.reset();
                
                source = new PositionReader(new InputStreamReader(is));
                token = new KinParser(source);
                lastRead = url; // made it to here, must have been a good URL
                
                synchronized(progBar) { progBar.setMaximum(conn.getContentLength()); }
                
                readFile();

                try { source.close(); } catch(IOException ex) {}
            }
            catch(IOException ex)
            {
                openError = "The file '"+url.getFile()+"'\ncould not be opened due to an I/O error.\n"+ex.getMessage();
            }
            catch(SecurityException ex)
            {
                openError = "The file '"+url.getFile()+"'\ncould not be opened due to security restrictions\nestablished by the browser.";
            }
            
        }//loop over URLs
        
        // Do this to prevent some memory leaks
        openKin = null;
        
        SwingUtilities.invokeLater(new ReflectiveRunnable(this, "install"));
    }
//}}}

//{{{ testLoadURLs() -- for testing and profiling
//##################################################################################################
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    /**
    * Runs in a background thread, calling readFile for each URL to be read.
    * Not to be called by the general public.
    */
    public void testLoadURLs(URL url)
    {
        kinList = new ArrayList();
        
        URLConnection conn;
        InputStream is;
        
        // set up data structures
        currKin   = openKin;
        currGroup = null;
        currSub   = null;
        currList  = null;
        
        // set up connection and data source
        try
        {
            conn = url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.connect();
            is = conn.getInputStream();
            
            // Test for GZIPped files
            is = new BufferedInputStream(is);
            is.mark(10);
            if(is.read() == 31 && is.read() == 139)
            {
                // We've found the gzip magic numbers...
                is.reset();
                is = new GZIPInputStream(is);
            }
            else is.reset();
            
            source = new PositionReader(new InputStreamReader(is));
            token = new KinParser(source);
            lastRead = url; // made it to here, must have been a good URL
            
            readFile();
            
            try { source.close(); } catch(IOException ex) {}
        }
        catch(IOException ex)
        {
            openError = "The file '"+url.getFile()+"'\ncould not be opened due to an I/O error.\n"+ex.getMessage();
        }
        catch(SecurityException ex)
        {
            openError = "The file '"+url.getFile()+"'\ncould not be opened due to security restrictions\nestablished by the browser.";
        }
    }
//}}}

//{{{ actionPerformed() -- called during a read to update the progress bar
//##################################################################################################
    /** Updates the progress bar during file loading; called by a swing.Timer */
    public void actionPerformed(ActionEvent ev)
    {
        synchronized(progBar)
        {
            if(source != null) progBar.setValue((int)source.getPosition());
        }
    }
//}}}

//{{{ install() -- runs in event thread, after kins are loaded
//##################################################################################################
    // A function that takes the read-in kinemage(s) and folds it/them into the data structure.
    // Install the loaded kinemages, but do so in the event-dispatching thread.
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void install()
    {
        // Un-protect data structure now that we're done.
        if(ignoreAtkinemage) kMain.getStable().setLocked(false);
        else kMain.getStable().append(kinList);
        
        // close the progress dialog
        progTimer.stop();
        progDialog.dispose();
        
        // report last error, if any
        if(openError != null) JOptionPane.showMessageDialog(kMain.getMainWindow(), openError, "Error", JOptionPane.ERROR_MESSAGE);
        
        // Memory leaks otherwise!!
        kinList = null;
    }
//}}}

//{{{ readFile()
//##################################################################################################
    /**
    * Reads a kinemage from the specified stream and builds up an internal representation of the data.
    * Not to be called by the general public.
    */
    void readFile()
    {
        time = System.currentTimeMillis();
        
        String text = "";
        String tokenlc, key, colorsetname;
        int i;
        
        MasterGroup master;
        ColorManager colorman;
        AnimationGroup anim;
        
        for(token.next(); !token.isEOF(); token.next())
        {
            // Discard non-keywords
            if( !token.isKeyword()) token.consume();
            else
            {
                token.consume();
                tokenlc = token.sval.toLowerCase();
                
                //{{{ Text, other informational keywords
                if(token.sval.equalsIgnoreCase("@text") || token.sval.equalsIgnoreCase("@caption") || token.sval.equalsIgnoreCase("@htmltext"))
                {
                    token.longText();
                    text = text.concat(token.sval);
                    token.consume();
                }
                else if(token.sval.equalsIgnoreCase("@comment"))
                {
                    // @comment starts a text block like @text, except that it's ignored
                    token.longText();
                    token.consume();
                }
                else if(token.sval.equalsIgnoreCase("@mage") || token.sval.equalsIgnoreCase("@prekin"))
                {
                    // ignore these
                    token.consume();
                }
                else if(token.sval.equalsIgnoreCase("@pdbfile"))
                {
                    if(currKin != null)
                    {
                        token.next();
                        if(token.isName()) 
                        {
                            currKin.atPdbfile = token.sval;
                            token.consume(); token.next();
                        }
                    }
                    else echo("Can't say @pdbfile without a kinemage, line "+source.getLineNumber());
                }
                //}}}
                
                //{{{ Display options, colorsets
                else if(token.sval.equalsIgnoreCase("@whitebkg") || token.sval.equalsIgnoreCase("@whiteback") || token.sval.equalsIgnoreCase("@whitebackground"))
                {
                    if(currKin != null) currKin.atWhitebackground = true;
                    else echo("Can't say @whitebackground without a kinemage, line "+source.getLineNumber());
                }
                else if(token.sval.equalsIgnoreCase("@onewidth"))
                {
                    if(currKin != null) currKin.atOnewidth = true;
                    else echo("Can't say @onewidth without a kinemage, line "+source.getLineNumber());
                }
                else if(token.sval.equalsIgnoreCase("@thinline"))
                {
                    if(currKin != null) currKin.atThinline = true;
                    else echo("Can't say @thinline without a kinemage, line "+source.getLineNumber());
                }
                else if(token.sval.equalsIgnoreCase("@perspective"))
                {
                    if(currKin != null) currKin.atPerspective = true;
                    else echo("Can't say @perspective without a kinemage, line "+source.getLineNumber());
                }
                /*else if(token.sval.equalsIgnoreCase("@kingtool"))
                {
                    ToolBox toolbox = kMain.getCanvas().getToolBox();
                    token.next();
                    if(token.isName() && toolbox != null) toolbox.requestToolByName(token.sval, true);
                }*/
                else if(token.sval.equalsIgnoreCase("@flat") || token.sval.equalsIgnoreCase("@flatland") || token.sval.equalsIgnoreCase("@xytranslation"))
                {
                    if(currKin != null) currKin.atFlat = true;
                    else echo("Can't say @flat without a kinemage, line "+source.getLineNumber());
                }
                else if(token.sval.equalsIgnoreCase("@colorset"))
                {
                    if(currKin != null)
                    {
                        colorman = currKin.getColorManager();
                        token.next();
                        if(token.isName())
                        {
                            colorsetname = token.sval;
                            token.consume(); token.next();
                            if(token.isConstant() && colorman.hasColor(token.sval))
                            {
                                colorman.aliasColor("{"+colorsetname+"}", token.sval);
                                token.consume();
                            }
                        }
                    }
                    else echo("Can't have a colorset without a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@hsvcolor"))
                {
                    // @hsvcolor {color_name} hue blacksat whitesat blackval whiteval
                    // hue on [0,360]; sat, val on [0,100]
                    String colorname;
                    float[] hsvParams = new float[5];
                    if(currKin != null)
                    {
                        token.next();
                        if(token.isName())
                        {
                            colorname = token.sval;
                            token.consume(); token.next();
                            for(i = 0; token.isNumber() && i < hsvParams.length; i++)
                            {
                                hsvParams[i] = token.fval;
                                token.consume(); token.next();
                            }
                            if(i == hsvParams.length)
                            {
                                currKin.addHSVcolor(colorname, hsvParams[0], hsvParams[1], hsvParams[2], hsvParams[3], hsvParams[4]);
                            }
                            else echo("Bad color specification (<5 params), line "+source.getLineNumber());
                        }
                        else echo("Bad color specification (no name), line "+source.getLineNumber());
                    }
                    else echo("Can't have a newcolor without a @kinemage!");
                }
                //}}}
                
                //{{{ Hierarchy -- kinemages, groups, subgroups, lists
                else if(token.sval.equalsIgnoreCase("@kinemage"))
                {
                    if(!ignoreAtkinemage)
                    {
                        // sync masters in prev. kinemage before moving on
                        if(currKin != null) currKin.initAll();
                        
                        // ignore the numbering of kinemages
                        token.next();
                        if(token.isInteger()) { token.consume(); token.next(); }
                        // check for a name after/in place of the number
                        if(token.isName())
                        {
                            token.consume();
                            currKin = new Kinemage(kMain, token.sval);
                        }
                        else
                        {
                            currKin = new Kinemage(kMain, "Kinemage "+kinCount);
                            kinCount++;
                        }
                        kinList.add(currKin);
                        currGroup = null;
                        currSub = null;
                        currList = null;
                    }
                }
                else if(token.sval.equalsIgnoreCase("@group"))
                {
                    if(currKin != null)
                    {
                        currGroup = readGroup(token);
                        currKin.add(currGroup);
                        currSub  = null;
                        currList = null;
                    }
                    else echo("Can't have a @group without a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@subgroup"))
                {
                    if(currKin != null)
                    {
                        if(currGroup == null)
                        {
                            currGroup = new KGroup(currKin, "(implied)");
                            currGroup.setHasButton(false);
                            currKin.add(currGroup);
                        }
                        currSub = readSubgroup(token);
                        currGroup.add(currSub);
                        currList = null;
                    }
                    else echo("Can't have a @subgroup without a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@vectorlist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.VECTOR);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@balllist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.BALL);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@spherelist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.BALL);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                    // Now that we read it in as a ball list, set the sphere flag for rendering
                    if(currList != null && currList.type == KList.BALL) currList.type = KList.SPHERE;
                }
                else if(token.sval.equalsIgnoreCase("@dotlist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.DOT);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@marklist")
                || token.sval.equalsIgnoreCase("@ringlist")
                || token.sval.equalsIgnoreCase("@fanlist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.MARK);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@labellist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.LABEL);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@trianglelist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.TRIANGLE);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@ribbonlist"))
                {
                    if(currKin != null)
                    {
                        // currGroup, currSub are created (if necessary) in readList()
                        currList = readList(token, KList.TRIANGLE);
                        currSub.add(currList);
                    }
                    else echo("Can't have a list without at least a @kinemage!");
                    // Now that we read it in as a triangle list, set the ribbon flag for rendering
                    if(currList != null && currList.type == KList.TRIANGLE) currList.type = KList.RIBBON;
                }
                //}}}
                
                //{{{ Aspects, masters, animations
                else if(tokenlc.endsWith("aspect"))
                {
                    if(currKin != null)
                    {
                        Integer index;
                        try { index = new Integer(tokenlc.substring(1, tokenlc.indexOf("aspect"))); }
                        catch(NumberFormatException ex) { index = new Integer(1); }
                        
                        String name = "(unnamed)";
                        token.next();
                        if(token.isName())
                        {
                            token.consume();
                            name = token.sval;
                        }
                        
                        currKin.createAspect(name, index);
                    }
                    else echo("Can't have an aspect without a @kinemage, line "+source.getLineNumber()); 
                }
                else if(token.sval.equalsIgnoreCase("@master"))
                {
                    // This just creates the master button ahead of time,
                    // so they'll appear in the specified order later on.
                    if(currKin != null)
                    {
                        token.next();
                        if(token.isName())
                        {
                            master = currKin.getMasterByName(token.sval);
                            token.consume();
                            for(token.next(); !token.isEndOfKeyword(); token.next())
                            {
                                if(token.isConstant())
                                {
                                    if(token.sval.equalsIgnoreCase("nobutton")) master.setHasButton(false);
                                    // Careful! Master declared 'off' after some objects will turn them off!
                                    // This doesn't actually work, yet
                                    if(token.sval.equalsIgnoreCase("off"))      master.setOn(false);
                                }
                                token.consume();
                            }
                        }
                    }
                    else echo("Can't have a master without a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@pointmaster"))
                {
                    // This both creates the master and sets its pointmaster bitflags
                    // To use flags like 'nobutton' and 'off', there has to be
                    // a duplicate @master statement (and it should come first!)
                    //
                    // Syntax: @pointmaster 'x' {Button name}
                    // Possible to do the following (but why?):
                    //         @pointmaster 'abc' {Multi-master}
                    if(currKin != null)
                    {
                        String pm = "";
                        token.next();
                        if(token.isSingleQuote()) { pm = token.sval; token.consume(); }
                        token.next();
                        if(token.isName())
                        {
                            currKin.getMasterByName(token.sval).setPmMask(pm);
                            token.consume();
                        }
                    }
                    else echo("Can't have a pointmaster without a @kinemage!");
                }
                else if(token.sval.equalsIgnoreCase("@animation"))
                {
                    if(currKin != null)
                    {
                        token.next();
                        if(token.isName()) { token.consume(); anim = currKin.getAnimationByName(token.sval); }
                        else               { anim = currKin.getAnimationByName("ANIMATE"); }
                        
                        for(token.next(); !token.isEndOfKeyword(); token.next())
                        {
                            if(token.isName()) { anim.add(currKin.getMasterByName(token.sval)); }
                            token.consume();
                        }
                    }
                    else echo("Can't have an animation without a @kinemage!");
                }
                //}}}
                
                //{{{ Views
                else if(tokenlc.endsWith("viewid"))
                {
                    if(currKin != null)
                    {
                        key = tokenlc.substring(1, tokenlc.indexOf("viewid"));
                        if(key.equals("")) key = "1";
                        
                        token.next();
                        if(token.isName())
                        {
                            token.consume();
                            currKin.getViewByKey(key).setID(token.sval);
                        }
                    }
                    else echo("Can't have a viewid without a @kinemage!"); 
                }
                else if(tokenlc.endsWith("zoom"))
                {
                    if(currKin != null)
                    {
                        key = tokenlc.substring(1, tokenlc.indexOf("zoom"));
                        if(key.equals("")) key = "1";
                        
                        token.next();
                        if(token.isNumber())
                        {
                            token.consume();
                            currKin.getViewByKey(key).setZoom(token.fval);
                        }
                        else echo("Must specify a number after "+tokenlc);
                    }
                    else echo("Can't have a zoom without a @kinemage!"); 
                }
                else if(tokenlc.endsWith("span"))
                {
                    if(currKin != null)
                    {
                        key = tokenlc.substring(1, tokenlc.indexOf("span"));
                        if(key.equals("")) key = "1";
                        
                        token.next();
                        if(token.isNumber())
                        {
                            token.consume();
                            currKin.getViewByKey(key).setSpan(token.fval);
                        }
                        else echo("Must specify a number after "+tokenlc);
                    }
                    else echo("Can't have a span without a @kinemage!"); 
                }
                else if(tokenlc.endsWith("zslab") || tokenlc.endsWith("zclip"))
                {
                    if(currKin != null)
                    {
                        key = tokenlc.substring(1, tokenlc.indexOf("z"));
                        if(key.equals("")) key = "1";
                        
                        token.next();
                        if(token.isNumber())
                        {
                            token.consume();
                            currKin.getViewByKey(key).setClip(token.fval/200f);
                        }
                        else echo("Must specify a number after "+tokenlc);
                    }
                    else echo("Can't have a zslab/zclip without a @kinemage!"); 
                }
                else if(tokenlc.endsWith("ztran"))
                {
                    // ignore z-tran
                }
                else if(tokenlc.endsWith("center"))
                {
                    if(currKin != null)
                    {
                        key = tokenlc.substring(1, tokenlc.indexOf("center"));
                        if(key.equals("")) key = "1";
                        
                        float cx = 0f, cy = 0f, cz = 0f;
                        token.next();
                        if(token.isNumber()) { token.consume(); cx = token.fval; }
                        token.next();
                        if(token.isNumber()) { token.consume(); cy = token.fval; }
                        token.next();
                        if(token.isNumber()) { token.consume(); cz = token.fval; }
                        else echo("Must specify 3 numbers after "+tokenlc);
                        
                        currKin.getViewByKey(key).setCenter(cx, cy, cz);
                    }
                    else echo("Can't have a center without a @kinemage!"); 
                }
                else if(tokenlc.endsWith("matrix"))
                {
                    if(currKin != null)
                    {
                        key = tokenlc.substring(1, tokenlc.indexOf("matrix"));
                        if(key.equals("")) key = "1";
                        
                        // Mage-style (post-multiplied) matrix
                        float[] mm = { 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f };
                        for(int imat = 0; imat < 9; imat++)
                        {
                            token.next();
                            if(token.isNumber()) { token.consume(); mm[imat] = token.fval; }
                            else echo(tokenlc+" is not followed by 9 numbers!");
                        }
                        // King-style (pre-multiplied) matrix
                        // km is transpose of mm
                        float[][] km = new float[3][3];
                        km[0][0] = mm[0]; km[1][0] = mm[1]; km[2][0] = mm[2];
                        km[0][1] = mm[3]; km[1][1] = mm[4]; km[2][1] = mm[5];
                        km[0][2] = mm[6]; km[1][2] = mm[7]; km[2][2] = mm[8];
                        
                        currKin.getViewByKey(key).setMatrix(km);
                    }
                    else echo("Can't have a matrix without a @kinemage!"); 
                }
                //}}}
                else echo("Unsupported keyword, line "+source.getLineNumber()+": "+token.sval);
            }//is a keyword
        }//for ! EOF
        
        time = System.currentTimeMillis() - time;
        System.err.println("Load time: "+time+" msec");
        
        kMain.getTextWindow().appendText(text);
        if(currKin != null) currKin.initAll();
        
        // Do this to prevent some memory leaks
        currKin     = null;
        currGroup   = null;
        currSub     = null;
        currList    = null;
    }
//}}}

//{{{ readGroup()    
//##################################################################################################
    KGroup readGroup(KinParser token)
    {
        KGroup theGroup = new KGroup(currKin, "(no name)");
        token.next();
        // Read parameters for the group
        for(token.next(); !token.isEndOfKeyword(); token.next())
        {
            // Group name (done as a param b/c Probe doesn't always write it first)
            if(token.isName())
            {
                theGroup.setName(token.sval);
            }
            // Single word parameters: dominant, nobutton, off, etc.
            else if(token.isConstant())
            {
                     if(token.sval.equalsIgnoreCase("dominant")) theGroup.setDominant(true);
                else if(token.sval.equalsIgnoreCase("nobutton")) theGroup.setHasButton(false);
                else if(token.sval.equalsIgnoreCase("off"))      theGroup.setOn(false);
                else if(token.sval.equalsIgnoreCase("animate"))  theGroup.setAnimate(true);
                else if(token.sval.equalsIgnoreCase("2animate")) theGroup.set2Animate(true);
            }
            // Two-part parameters: master, etc.
            else if(token.isParam())
            {
                if(token.sval.equalsIgnoreCase("master"))
                {
                    token.consume(); token.next();
                    if(token.isName())
                    {
                        currKin.ensureMasterExists(token.sval);
                        theGroup.addMaster(token.sval);
                    }
                }
                else { token.consume(); token.next(); }
            }
            token.consume();
        }//group params

        return theGroup;
    }
//}}}

//{{{ readSubgroup()    
//##################################################################################################
    KSubgroup readSubgroup(KinParser token)
    {
        KSubgroup theGroup = new KSubgroup(currGroup, "(no name)");
        token.next();
        // Read parameters for the group
        for(token.next(); !token.isEndOfKeyword(); token.next())
        {
            // Group name (done as a param b/c Probe doesn't always write it first)
            if(token.isName())
            {
                theGroup.setName(token.sval);
            }
            // Single word parameters: dominant, nobutton, off, etc.
            else if(token.isConstant())
            {
                     if(token.sval.equalsIgnoreCase("dominant")) theGroup.setDominant(true);
                else if(token.sval.equalsIgnoreCase("nobutton")) theGroup.setHasButton(false);
                else if(token.sval.equalsIgnoreCase("off"))      theGroup.setOn(false);
            }
            // Two-part parameters: master, etc.
            else if(token.isParam())
            {
                if(token.sval.equalsIgnoreCase("master"))
                {
                    token.consume(); token.next();
                    if(token.isName())
                    {
                        currKin.ensureMasterExists(token.sval);
                        theGroup.addMaster(token.sval);
                    }
                }
                else { token.consume(); token.next(); }
            }
            token.consume();
        }//group params

        return theGroup;
    }
//}}}

//{{{ readList()
//##################################################################################################
    KList readList(KinParser token, String type)
    {
        if(currGroup == null)
        {
            currGroup = new KGroup(currKin, "(implied)");
            currGroup.setHasButton(false);
            currKin.add(currGroup);
        }
        if(currSub == null)
        {
            currSub = new KSubgroup(currGroup, "(implied)");
            currSub.setHasButton(false);
            currGroup.add(currSub);
        }

        //{{{ List
        // List variables:
        KList theList;
        boolean listNamed = false;
        
        // Create the list object
        theList = new KList(currSub, "(no name)");
        theList.setType(type);

        // Read parameters for the list
        for(token.next(); !token.isEndOfKeyword() && !(token.isName() && listNamed); token.next())
        {
            // List name (done as a param b/c it doesn't always come first)
            if(token.isName())
            {
                listNamed = true;
                theList.setName(token.sval);
            }
            // Single word parameters: dominant, nobutton, off
            if(token.isConstant())
            {
                     if(token.sval.equalsIgnoreCase("dominant")) theList.setDominant(true);
                else if(token.sval.equalsIgnoreCase("nobutton")) theList.setHasButton(false);
                else if(token.sval.equalsIgnoreCase("off"))      theList.setOn(false);
                else if(token.sval.equalsIgnoreCase("nohi") || token.sval.equalsIgnoreCase("nohilite") || token.sval.equalsIgnoreCase("nohighlight"))
                { theList.flags |= KList.NOHILITE; }
            }
            // Two-part parameters: color=, radius=, width=
            else if(token.isParam())
            {
                if(token.sval.equalsIgnoreCase("master"))
                {
                    token.consume(); token.next();
                    if(token.isName())
                    {
                        currKin.ensureMasterExists(token.sval);
                        theList.addMaster(token.sval);
                    }
                }
                else if(token.sval.equalsIgnoreCase("color") || token.sval.equalsIgnoreCase("colour"))
                {
                    token.consume(); token.next();
                    if(token.isConstant() && currKin.colorman.hasColor(token.sval)) theList.color = currKin.colorman.getIndex(token.sval);
                    else if(token.isName() && currKin.colorman.hasColor("{"+token.sval+"}")) theList.color = currKin.colorman.getIndex("{"+token.sval+"}");
                }
                else if(token.sval.equalsIgnoreCase("radius"))
                {
                    token.consume(); token.next();
                    if(token.isNumber()) theList.radius = token.fval;
                }
                else if(token.sval.equalsIgnoreCase("width"))
                {
                    token.consume(); token.next();
                    if(token.isInteger()) theList.width = token.ival;
                }
                else { token.consume(); token.next(); }
            }
            token.consume();
        }//list params
        //}}}
        
        //{{{ Points
        //##########################################################################################
        // Read the lists of points
        
        // point variables:
        KPoint        thePoint = null;
        VectorPoint   vFrom = null;
        TrianglePoint tFrom = null;
        BallPoint     bp = null;
        String        pID = "";
        int           whichcoord;
        
        // Loop over all points
        for(token.next(); !token.isEndOfKeyword(); token.next())
        {
            // Determine the name of this point
            // Not every point will have a name (e.g. WorldMap.kin)
            if(token.isName())
            {
                if(!token.sval.equals("\"")) pID = token.sval;
                token.consume();
            }
            
            whichcoord = 0; // x
            
            // Create the point
            if(type == KList.DOT)         thePoint = new DotPoint(theList, pID);
            else if(type == KList.VECTOR)
            {
                vFrom = (VectorPoint)thePoint;
                thePoint = new VectorPoint(theList, pID, vFrom);
            }
            else if(type == KList.BALL)   thePoint = new BallPoint(theList, pID);
            else if(type == KList.MARK)   thePoint = new MarkerPoint(theList, pID);
            else if(type == KList.LABEL)  thePoint = new LabelPoint(theList, pID);
            else if(type == KList.TRIANGLE)
            {
                tFrom = (TrianglePoint)thePoint;
                thePoint = new TrianglePoint(theList, pID, tFrom);
            }
            else                             thePoint = new DotPoint(theList, pID);
            
            // Read point parameters
            // adding 'whichcoord' clause allows us to read e.g. WorldMap.kin
            for(token.next(); !token.isEndOfPoint() && whichcoord < 3; token.next())
            {
                if(token.isNumber())
                {
                         if(whichcoord == 0) thePoint.x0 = token.fval;
                    else if(whichcoord == 1) thePoint.y0 = token.fval;
                    else if(whichcoord == 2) thePoint.z0 = token.fval;
                    whichcoord += 1;
                }
                // Aspects: (ABC...)
                else if(token.isAspect())
                {
                    thePoint.aspects = token.sval;
                }
                // Point masters: 'x'
                else if(token.isSingleQuote())
                {
                    thePoint.pm_mask = MasterGroup.toPmBitmask(token.sval);
                }
                // constants
                else if(token.isConstant()) 
                {
                    // point / move
                    if(token.sval.equalsIgnoreCase("P") || token.sval.equalsIgnoreCase("M"))
                    {
                        if(type != KList.TRIANGLE) thePoint.setPrev(null);
                        //if(type == KList.VECTOR) ((VectorPoint)thePoint).from = null;
                        //else if(type == KList.TRIANGLE) thePoint.multi &= ~TrianglePoint.LINETO_BIT;
                    }
                    if(token.sval.equalsIgnoreCase("L") || token.sval.equalsIgnoreCase("D"))
                    {
                        //if(type == KList.TRIANGLE) thePoint.multi |= TrianglePoint.LINETO_BIT;
                    }
                    // unpickable
                    else if(token.sval.equalsIgnoreCase("U")) thePoint.setUnpickable(true);
                    // width4, etc.
                    else if(token.sval.startsWith("width") && type == KList.VECTOR)
                    {
                        try {
                            ((VectorPoint)thePoint).width = Integer.parseInt(token.sval.substring(5));
                        } catch(NumberFormatException ex) {}
                    }
                    // a color?
                    else if(currKin.colorman.hasColor(token.sval)) thePoint.setColor(currKin.colorman.getIndex(token.sval));
                    // label alignments
                    else if(type == KList.LABEL)
                    {
                             if(token.sval.equalsIgnoreCase("left"))   ((LabelPoint)thePoint).setHorizontalAlignment(LabelPoint.LEFT);
                        else if(token.sval.equalsIgnoreCase("center")) ((LabelPoint)thePoint).setHorizontalAlignment(LabelPoint.CENTER);
                        else if(token.sval.equalsIgnoreCase("right"))  ((LabelPoint)thePoint).setHorizontalAlignment(LabelPoint.RIGHT);
                    }
                    // Don't put anything here -- everything else swallowed by above block for labels
                }
                // two-part params
                else if(token.isParam())
                {
                    // r= radius
                    if(token.sval.equalsIgnoreCase("r") && type == KList.BALL)
                    {
                        token.consume(); token.next();
                        if(token.isNumber()) ((BallPoint)thePoint).r0 = token.fval;
                    }
                    // w= width (KiNG extension)
                    else if(token.sval.equalsIgnoreCase("w") && type == KList.VECTOR)
                    {
                        token.consume(); token.next();
                        if(token.isInteger()) ((VectorPoint)thePoint).width = token.ival;
                    }
                    // s= style (KiNG extension)
                    else if(token.sval.equalsIgnoreCase("s") && type == KList.MARK)
                    {
                        token.consume(); token.next();
                        if(token.isInteger()) ((MarkerPoint)thePoint).style = token.ival;
                    }
                    // something unknown
                    else { token.consume(); token.next(); }
                }
                token.consume();
            }//this point's parameters
            
            theList.add(thePoint);
        }//all points
        //}}}
        
        return theList;
    }
//}}}

//{{{ echo()
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}
}//class
