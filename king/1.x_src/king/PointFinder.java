// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import gnu.regexp.*;
import driftwood.gui.*;
import driftwood.util.SoftLog;
//}}}
/**
 * <code>PointFinder</code> is responsible for the Edit | Find dialog.
 *
 * <p>Begun on Fri Jun 14 21:19:49 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class PointFinder implements WindowListener
{
//{{{ Static fields
    static final String WHOLE_WORDS = "Whole words";
    static final String SUBSTRINGS  = "Substrings";
    static final String SIMPLE_REGEX  = "Globs: ?=1, *=any";
    static final String PERL_REGEX  = "Perl5 regex";
    static final String[] SEARCH_TYPES = { WHOLE_WORDS, SUBSTRINGS, SIMPLE_REGEX, PERL_REGEX };
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain    kMain;
    
    JDialog     dialog;
    JTextField  searchField;
    JComboBox   searchType;
    JCheckBox   caseSensitive;
    JCheckBox   centerOnFound;
    
    RecursivePointIterator  ptIter;
    Collection              query;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public PointFinder(KingMain kmain)
    {
        kMain = kmain;
        
        dialog = new JDialog(kMain.getTopWindow(), "Find point", true);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.addWindowListener(this);

        // Layout setup
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.fill = gbc.NONE;
        gbc.ipadx = gbc.ipady = 0;
        gbc.insets = new Insets(4,4,4,4);
        gbc.anchor = gbc.CENTER;
        gbc.weightx = gbc.weighty = 0.0;
        
        // Search pane construction
        Container searchPane = dialog.getContentPane();
        searchPane.setLayout(gbl);
        ReflectiveAction searchAction = new ReflectiveAction("Search", null, this, "onSearch");
        ReflectiveAction closeAction = new ReflectiveAction("Close", null, this, "onClose");
        JButton btnClose = new JButton(closeAction);
        closeAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        closeAction.bindWindow(btnClose);
        
        searchField = new JTextField(20);
        searchField.addActionListener(searchAction);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbl.setConstraints(searchField, gbc);
        searchPane.add(searchField);
        
        searchType = new JComboBox(SEARCH_TYPES);
        searchType.setEditable(false);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1;
        gbl.setConstraints(searchType, gbc);
        searchPane.add(searchType);
        
        caseSensitive = new JCheckBox("Case sensitive", false);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        gbl.setConstraints(caseSensitive, gbc);
        searchPane.add(caseSensitive);
        
        centerOnFound = new JCheckBox("Go to found point (re-center)", true);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.anchor = gbc.WEST;
        gbl.setConstraints(centerOnFound, gbc);
        searchPane.add(centerOnFound);
        gbc.anchor = gbc.CENTER;
        
        Box box = Box.createHorizontalBox();
        box.add(new JButton(searchAction));
        box.add(Box.createRigidArea(new Dimension(16,0)));
        box.add(btnClose);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbl.setConstraints(box, gbc);
        searchPane.add(box);

        clearSearch();
    }
//}}}

//{{{ onXXX() functions, show()
//##################################################################################################
    public void show()
    {
        dialog.pack();
        dialog.setLocationRelativeTo(kMain.getTopWindow());
        searchField.selectAll();
        searchField.requestFocus(); // may not take effect since dlg isn't visible?
        dialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSearch(ActionEvent ev)
    {
        Kinemage kin = kMain.getKinemage();     if(kin == null) return;
        
        Object mode = searchType.getSelectedItem();
        ArrayList newQuery = new ArrayList();
        String token;
        
        RE search;
        // For whole words & substrings, quote metacharacters with backslashes
        // Things to quote:                  \  ^  $  .  [  ]  (  )  |  *  +  ?  {  }
        RE protector = new UncheckedRE("([\\\\\\^\\$\\.\\[\\]\\(\\)\\|\\*\\+\\?\\{\\}])");
        // For "simple regex" searches             \  ^  $  .  [  ]  (  )  |  +  {  }
        RE simpleProtector = new UncheckedRE("([\\\\\\^\\$\\.\\[\\]\\(\\)\\|\\+\\{\\}])");
        RE simpleRenamer   = new UncheckedRE("([\\*])");
        int cflags = ( caseSensitive.isSelected() ? 0 : RE.REG_ICASE );

        if(mode == WHOLE_WORDS)
        {
            try {
                StringTokenizer st = new StringTokenizer(searchField.getText());
                while(st.hasMoreTokens())
                {
                    token = protector.substituteAll(st.nextToken(), "\\$1");
                    //search = new RE(".*\\b"+token+"\\b.*", cflags);
                    search = new RE("(?:.*[ ,;:])?"+token+"(?:[ ,;:].*)?", cflags);
                    //System.err.println("Search term: "+search);
                    newQuery.add(search);
                }
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getTopWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(mode == SUBSTRINGS)
        {
            try {
                StringTokenizer st = new StringTokenizer(searchField.getText());
                while(st.hasMoreTokens())
                {
                    token = protector.substituteAll(st.nextToken(), "\\$1");
                    search = new RE(".*"+token+".*", cflags);
                    newQuery.add(search);
                }
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getTopWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(mode == SIMPLE_REGEX)
        {
            try {
                // Rewrite most special chars; rewrite * and ? as .* and .
                token = searchField.getText();
                token = simpleProtector.substituteAll(token, "\\$1");
                token = simpleRenamer.substituteAll(token, ".$1");
                token = token.replace('?', '.');
                search = new RE(".*"+token+".*", cflags);
                newQuery.add(search);
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getTopWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(mode == PERL_REGEX)
        {
            try {
                search = new RE(".*"+searchField.getText()+".*", cflags);
                newQuery.add(search);
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getTopWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {return;}

        // Using dispose() instead of setVisible(false) prevents redraw and focus problems
        dialog.dispose();
        
        query   = newQuery;
        ptIter  = new RecursivePointIterator(kin);
        findNext();
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClose(ActionEvent ev)
    {
        // Using dispose() instead of setVisible(false) prevents redraw and focus problems
        dialog.dispose();
    }
//}}}

//{{{ clearSearch, findNext
//##################################################################################################
    /** Clears out the parameters used by the previous search */
    public void clearSearch()
    {
        ptIter = null;
        query = null;
    }
    
    /** Centers on the next found point, or else returns false. */
    public boolean findNext()
    {
        if(ptIter == null || query == null) return false;
        
        KPoint      p;
        RE          re;
        RE[]        allREs = (RE[])query.toArray(new RE[ query.size() ]);
        boolean     matchesAll;
        Iterator    reIter;
        
        try
        {
            while(ptIter.hasNext())
            {
                p           = ptIter.next();
                matchesAll  = p.isTotallyOn();
                
                //for(reIter = query.iterator(); matchesAll && reIter.hasNext(); )
                for(int i = 0; matchesAll && i < allREs.length; i++)
                {
                    //re = (RE)reIter.next();
                    re = allREs[i];
                    if(!re.isMatch(p.getName())) { matchesAll = false; }
                }//for each regex
                
                if(matchesAll)//center on the found point
                {
                    if(kMain.getView() != null)
                    {
                        ToolServices ts = kMain.getCanvas().getToolBox().services;
                        ts.pick(p);
                        if(centerOnFound.isSelected()) ts.centerOnPoint(p);
                    }
                    return true;
                }//if match
            }//for each point
        }
        catch(ConcurrentModificationException ex)
        { SoftLog.err.println("Find Next failed due to concurrent modification of the kinemage."); }
        
        return false;
    }
//}}}

//{{{ Window events
//##################################################################################################
    public void windowActivated(WindowEvent ev)   { searchField.requestFocus(); }
    public void windowClosed(WindowEvent ev)      {}
    public void windowClosing(WindowEvent ev)     {}
    public void windowDeactivated(WindowEvent ev) {}
    public void windowDeiconified(WindowEvent ev) {}
    public void windowIconified(WindowEvent ev)   {}
    public void windowOpened(WindowEvent ev)      {}
//}}}
}//class
