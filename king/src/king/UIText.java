// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import driftwood.gui.*;
import driftwood.util.*;
//}}}
/**
* <code>UIText</code> is the kinemage text manager.
* It takes care of displaying and editing the text.
*
* <p>Copyright (C) 2002-2007 by Ian W. Davis. All rights reserved.
* <br>Begun on Sun Jun  9 19:06:25 EDT 2002
*/
public class UIText implements MouseListener, KMessage.Subscriber
{
//{{{ Static fields
//}}}

//{{{ INTERFACE: HypertextListener
//##############################################################################
    /**
    * <code>HypertextListener</code> is able to get events when the user
    * selects a Mage-style *{hyperlink}* from the text window.
    *
    * @see UIText#addHypertextListener(HypertextListener)
    * @see UIText#removeHypertextListener(HypertextListener)
    *
    * <p>Copyright (C) 2004-2007 by Ian W. Davis. All rights reserved.
    * <br>Begun on Fri Jul 16 11:47:37 EDT 2004
    */
    public interface HypertextListener //extends ... implements ...
    {
        /**
        * Called by UIText whenever the user selects any Mage-style
        * hyperlink, which is bracked by *{ and *}.
        * @param link   the text of the link, minus the flanking brackets
        */
        public void mageHypertextHit(String link);
    }//class
//}}}

//{{{ CLASS: TextChangeListener
//##############################################################################
    /**
    * <code>TextChangeListener</code> gets events when the user modifies the text
    * in the text window, then records that the kinemage has been modified 
    * (so the user can be asked about saving when it's closed).
    */
    public class TextChangeListener implements DocumentListener
    {
        UIText uiText;
        
        public TextChangeListener(UIText uitext)
        { this.uiText = uitext; }
        
        public void insertUpdate(DocumentEvent e)
        {
            // If user actually changed the text, record that here.
            // PROBLEM: It's hard to tell when a user meaningfully edited 
            // the text him/herself vs. when a 2nd (or 3rd or ...) kinemage
            // was appended, which technically *changes* the contents of 
            // the text window.
            /*if(ACTUAL_CHANGE)
                kMain.getKinemage().setModified(true);*/
        }
        public void removeUpdate(DocumentEvent e)
        {
            // If user actually changed the text, record that here.
            // PROBLEM: It's hard to tell when a user meaningfully edited 
            // the text him/herself vs. when a 2nd (or 3rd or ...) kinemage
            // was appended, which technically *changes* the contents of 
            // the text window.
            /*if(ACTUAL_CHANGE)
                kMain.getKinemage().setModified(true);*/
        }
        public void changedUpdate(DocumentEvent e)
        {
            // Plain text components do not fire these events
        }
    }
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    JFrame frame;
    JTextArea textarea;
    JCheckBox allowTextEdits;
    
    JButton popupButton;
    JFileChooser fileSaveChooser;
    
    Collection mageHypertextListeners = new ArrayList();
    
    boolean modified = false; // by analogy to Kinemage.modified, but for >= 1 Kinemage
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public UIText(KingMain kmain)
    {
        kMain = kmain;
        popupButton = new JButton(new ReflectiveAction("Show text", null, this, "onPopupButton"));

        frame = new JFrame("Text window");
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setIconImage(kMain.getPrefs().windowIcon);
        
        allowTextEdits = new JCheckBox(new ReflectiveAction("Allow text to be edited", null, this, "onAllowTextEdits"));
        allowTextEdits.setSelected( kMain.getPrefs().getBoolean("textDefaultAllowEdits") );
        
        textarea = new JTextArea();
        textarea.setEditable( allowTextEdits.isSelected() );
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
        textarea.addMouseListener(this);
        textarea.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        //textarea.setFont(new Font("Monospaced", Font.PLAIN, (int)Math.round(12 * kMain.getPrefs().getFloat("fontMagnification"))));
        JScrollPane textScroll = new JScrollPane(textarea);
        textScroll.setPreferredSize(new Dimension(500,400));
        new TextCutCopyPasteMenu(textarea);
        this.addHypertextListener(new MageHypertext(kMain));
        textarea.getDocument().addDocumentListener(new TextChangeListener(this));
        
        // Key bindings: just type the key to execute -- DAK 101115
        InputMap im = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        ActionMap am = frame.getRootPane().getActionMap();
        am.put("close", new ReflectiveAction(null, null, this, "onClose"   ));
        
        initSaveFileChooser();
        JButton exportText = new JButton(new ReflectiveAction("Export text to file", null, this, "onExportText"));
        //exportText.setPreferredSize(new Dimension(10, 20));
        //exportText.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        frame.getContentPane().add(allowTextEdits, BorderLayout.NORTH);
        frame.getContentPane().add(textScroll, BorderLayout.CENTER);
        frame.getContentPane().add(exportText, BorderLayout.SOUTH);
        kMain.subscribe(this);
    }
//}}}

//{{{ initSaveFileChooser
//##################################################################################################
  private void initSaveFileChooser() {
    try {
      SuffixFileFilter fileFilter = new SuffixFileFilter("text files (*.txt)");
      // New suffixes:
      fileFilter.addSuffix(".txt");
      fileSaveChooser = new JFileChooser();
      fileSaveChooser.addChoosableFileFilter(fileFilter);
      fileSaveChooser.setFileFilter(fileFilter);
      
      String currdir = System.getProperty("user.dir");
      if(currdir != null)
      {
        fileSaveChooser.setCurrentDirectory(new File(currdir));
      }
    }
    catch(SecurityException ex) {}
    // Temporary fix for Java 6 bug # 6570445:  JFileChooser in unsigned applet
    catch(java.lang.ExceptionInInitializerError ex)
    {
      if(!(ex.getCause() instanceof java.security.AccessControlException))
        throw ex;
    }
    // Subsequent attempts to create JFileChooser cause NoClassDefFound
    catch(java.lang.NoClassDefFoundError ex) {}
  }
//}}}

//{{{ get/set/appendText()
//##################################################################################################
    public String getText()
    { return textarea.getText(); }
    
    public void setText(String txt)
    {
        textarea.setText(txt);
        textarea.setCaretPosition(0); // at the top
    }

    public void appendText(String txt)
    {
      if (!txt.equals(textarea.getText())) { // to help with issue of multiplying text
        txt = getText().concat(txt);
        
        // Keep the text window from moving around too much
        int caret = textarea.getCaretPosition();
        caret = Math.min(caret, txt.length());
        textarea.setText(txt);
        textarea.setCaretPosition(caret);
      }
    }
//}}}

//{{{ deliverMessage
//##################################################################################################
    public void deliverMessage(KMessage msg)
    {
        if(msg.testProg(KMessage.ALL_CLOSED))
            this.setText("");
        if(msg.testProg(KMessage.KING_SHUTDOWN))
            frame.dispose();
    }
//}}}

//{{{ cascadeBehind, onPopupButton, onAllowTextEdits, getButton
//##################################################################################################
    /**
    * Positions this window above, left, and behind the specified window.
    */
    public void cascadeBehind(Window w)
    {
        if(w == null) return;
        
        frame.pack();
        Point p = w.getLocation();
        frame.setLocation(p);
        frame.setVisible(true);
        p.x += 24;
        p.y += 24;
        w.setLocation(p);
        w.toFront();
        w.requestFocus();
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onPopupButton(ActionEvent ev)
    {
        if(!frame.isVisible())
        {
            frame.pack();
            //frame.setLocationRelativeTo(kMain.getTopWindow()); // centers frame
            frame.setVisible(true);
        }
        else
        {
            frame.toFront();
            //frame.requestFocus();
        }
    }
    
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onAllowTextEdits(ActionEvent ev)
    {
        textarea.setEditable( allowTextEdits.isSelected() );
    }
    
    public JButton getButton() { return popupButton; }
//}}}

//{{{ onExportText, saveFile, onClose
//##################################################################################################
  public void onExportText(ActionEvent ev) {
    String currdir = System.getProperty("user.dir");
    if(currdir != null) fileSaveChooser.setCurrentDirectory(new File(currdir));
    if(fileSaveChooser.APPROVE_OPTION == fileSaveChooser.showSaveDialog(kMain.getTopWindow()))
    {
      File f = fileSaveChooser.getSelectedFile();
      if( !f.exists() ||
        JOptionPane.showConfirmDialog(kMain.getTopWindow(),
          "This file exists -- do you want to overwrite it?",
          "Overwrite file?", JOptionPane.YES_NO_OPTION)
        == JOptionPane.YES_OPTION )
      {
        saveFile(f, getText());
        System.setProperty("user.dir", f.getAbsolutePath());
      }
    }
  }
  
  public void saveFile(File f, String text) { 
    try {
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
      out.println(text);
      out.flush();
      out.close();
    } catch (IOException ie) {
      ie.printStackTrace(SoftLog.err);
      JOptionPane.showMessageDialog(kMain.getTopWindow(),
        "An error occurred while saving the file.",
        "Sorry!", JOptionPane.ERROR_MESSAGE);
    }
  }
  
    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClose(ActionEvent ev)
    {
        frame.dispose();
    }
//}}}

//{{{ Mouse listeners (for hypertext)
//##################################################################################################
    public void mouseClicked(MouseEvent ev)
    {
        int where = textarea.viewToModel(ev.getPoint());
        //System.err.println("Click occurred at position "+where);
        
        String text = this.getText();
        int prevOpen, prevClose, nextOpen, nextClose;
        // "where-#" terms below ensure that link is active out through the space between } and *
        // Original code used "where" in all four places, cutting the link short.
        // Passing a negative start index to (last)IndexOf is the same as passing zero.
        prevOpen = text.lastIndexOf("*{", where);
        prevClose = text.lastIndexOf("}*", where-2);
        nextOpen = text.indexOf("*{", where);
        nextClose = text.indexOf("}*", where-1);
        //System.err.println("prevs:" + prevOpen + "," + prevClose + "; nexts:" + nextOpen + "," + nextClose);
        
        //                   Still works if prevClose == -1             Might not be a next hyperlink...
        if(prevOpen != -1 && prevOpen > prevClose && nextClose != -1 && (nextClose < nextOpen || nextOpen == -1))
        {
            String link = text.substring(prevOpen+2, nextClose);
            textarea.select(prevOpen, nextClose+2);
            //System.err.println("Hit hypertext: '"+link+"'");
            for(Iterator iter = mageHypertextListeners.iterator(); iter.hasNext(); )
            {
                HypertextListener listener = (HypertextListener) iter.next();
                listener.mageHypertextHit(link);
            }
        }
    }

    public void mouseEntered(MouseEvent ev)     {}
    public void mouseExited(MouseEvent ev)      {}
    public void mousePressed(MouseEvent ev)     {}
    public void mouseReleased(MouseEvent ev)    {}
//}}}

//{{{ add/removeHypertextListener
//##################################################################################################
    /** Registers a listener for hypertext events. */
    public void addHypertextListener(HypertextListener listener)
    {
        mageHypertextListeners.add(listener);
    }

    /** Registers a listener for hypertext events. */
    public void removeHypertextListener(HypertextListener listener)
    {
        mageHypertextListeners.remove(listener);
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class
