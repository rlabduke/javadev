// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;

import java.awt.*;
import java.awt.event.*;
//import java.io.*;
//import java.text.*;
import java.util.*;
import javax.swing.*;
import gnu.regexp.*;
//}}}
/**
 * <code>PointFinder</code> is responsible for the Edit | Find dialogs.
 *
 * <p>Begun on Fri Jun 14 21:19:49 EDT 2002
 * <br>Copyright (C) 2002 by Ian W. Davis. All rights reserved.
*/
public class PointFinder implements WindowListener
{
//{{{ Static fields
    static final String WHOLE_WORDS = "Whole words";
    static final String SUBSTRINGS  = "Substrings";
    static final String SIMPLE_REGEX  = "* and ? \"globs\"";
    static final String PERL_REGEX  = "Perl5 regex";
    static final String[] SEARCH_TYPES = { WHOLE_WORDS, SUBSTRINGS, SIMPLE_REGEX, PERL_REGEX };
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
    
    JDialog dialog;
    JTextField searchField;
    JComboBox searchType;
    JCheckBox caseSensitive;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public PointFinder(KingMain kmain)
    {
        kMain = kmain;
        
        dialog = new JDialog(kMain.getMainWindow(), "Find point", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
        Action searchAction = new ReflectiveAction("Search", null, this, "onSearch");
        Action closeAction = new ReflectiveAction("Close", null, this, "onClose");
        
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
        
        Box box = Box.createHorizontalBox();
        box.add(new JButton(searchAction));
        box.add(Box.createRigidArea(new Dimension(16,0)));
        box.add(new JButton(closeAction));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbl.setConstraints(box, gbc);
        searchPane.add(box);
    }
//}}}

//{{{ onXXX() functions, show()
//##################################################################################################
    public void show()
    {
        dialog.pack();
        dialog.setLocationRelativeTo(kMain.getMainWindow());
        searchField.selectAll();
        dialog.setVisible(true);
        // remember, execution of this thread stops here until dialog is closed
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onSearch(ActionEvent ev)
    {
        Object mode = searchType.getSelectedItem();
        ArrayList query = new ArrayList();
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
                    search = new RE(".*\\b"+token+"\\b.*", cflags);
                    query.add(search);
                }
                dialog.dispose();
                new SearchResults(kMain, query);
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getMainWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
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
                    query.add(search);
                }
                dialog.dispose();
                new SearchResults(kMain, query);
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getMainWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(mode == SIMPLE_REGEX)
        {
            try {
                // Rewrite most special chars; rewrite * and ? as .* and .?
                token = searchField.getText();
                token = simpleProtector.substituteAll(token, "\\$1");
                token = simpleRenamer.substituteAll(token, ".$1");
                token = token.replace('?', '.');
                search = new RE(".*"+token+".*", cflags);
                query.add(search);
                dialog.dispose();
                new SearchResults(kMain, query);
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getMainWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(mode == PERL_REGEX)
        {
            try {
                search = new RE(".*"+searchField.getText()+".*", cflags);
                query.add(search);
                dialog.dispose();
                new SearchResults(kMain, query);
            } catch(REException ex) {
                JOptionPane.showMessageDialog(kMain.getMainWindow(), "The string you entered was not a valid regular expression.\n"+ex.getMessage(), "Regex error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // This method is the target of reflection -- DO NOT CHANGE ITS NAME
    public void onClose(ActionEvent ev)
    {
        dialog.dispose();
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

//{{{ Utility/debugging functions
//##################################################################################################
    // Convenience functions for debugging
    void echo(String s) { System.err.println(s); } // like Unix 'echo'
    void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
//}}}

//##################################################################################################
//###  Inner class: SearchResults  #################################################################
//##################################################################################################

    /**
    * SearchResults implements a dialog that shows the results of a point search.
    */
    public static class SearchResults
    {
    //{{{ Variable definitions
    //##################################################################################################
        KingMain kMain;
        
        JDialog dialog;
        JList list;
        DefaultListModel model;
        Collection query;
        int nFound = 0;
        Kinemage kin = null;
    //}}}
    
    //{{{ Constructors
    //##################################################################################################
        /**
        * @param kmain the KingMain that started this program instance
        * @param q a Collection of gnu.regexp.RE's to be searched for
        */
        public SearchResults(KingMain kmain, Collection q)
        {
            kMain = kmain;
            query = q;
            
            dialog = new JDialog(kMain.getMainWindow(), "Search results", false);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            Action centerAction = new ReflectiveAction("Center on selected", null, this, "onCenter");
            Action closeAction  = new ReflectiveAction("Close", null, this, "onClose");
            
            // Found pane construction
            Container foundPane = dialog.getContentPane();
            
            model = new DefaultListModel();
            kin = kMain.getKinemage();
            if(kin != null) search(kin);
            else echo("Error: search launched with no kinemage!");
            
            list = new JList(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(8);
            JScrollPane listScroll = new JScrollPane(list);
            listScroll.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
            foundPane.add(listScroll, BorderLayout.CENTER);
            
            JButton centerButton = new JButton(centerAction);
            dialog.getRootPane().setDefaultButton(centerButton);
            JButton closeButton = new JButton(closeAction);
            String closekey = "ESC-CLOSE";
            closeButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), closekey);
            closeButton.getActionMap().put(closekey, closeAction);
            
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
            box.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
            box.add(Box.createHorizontalGlue());
            box.add(centerButton);
            box.add(Box.createRigidArea(new Dimension(16,0)));
            box.add(closeButton);
            box.add(Box.createHorizontalGlue());
            foundPane.add(box, BorderLayout.SOUTH);
    
            dialog.pack();
            dialog.setLocationRelativeTo(kMain.getMainWindow());
            dialog.setVisible(true);
        }
    //}}}
    
    //{{{ onXXX() functions
    //##################################################################################################
        // This method is the target of reflection -- DO NOT CHANGE ITS NAME
        public void onCenter(ActionEvent ev)
        {
            if(kin != kMain.getKinemage()) return;
            
            KPoint point = (KPoint)list.getSelectedValue();
            KingView view = kMain.getView();
            if(view != null && point != null)
            {
                view.setCenter(point.getOrigX(), point.getOrigY(), point.getOrigZ());
                kMain.notifyChange(KingMain.EM_NEWVIEW);
            }
        }
    
        // This method is the target of reflection -- DO NOT CHANGE ITS NAME
        public void onClose(ActionEvent ev)
        {
            dialog.dispose();
        }
    //}}}
    
    //{{{ search()
    //##################################################################################################
        /**
        * Performs a recursive search of the kinemage looking for points that match our query.
        */
        public void search(AGE age)
        {
            if(nFound > 1000) return; // limit the search in case of searches for ".*", etc.

            if(age instanceof KList)
            {
                KPoint p;
                RE re;
                boolean matchesAll;
                Iterator pIter, reIter;
                
                for(pIter = age.iterator(); pIter.hasNext(); )
                {
                    p           = (KPoint)pIter.next();
                    matchesAll  = true;
                    
                    for(reIter = query.iterator(); reIter.hasNext(); )
                    {
                        re = (RE)reIter.next();
                        if(!re.isMatch(p.getName())) { matchesAll = false; break; }
                    }//for each regex
                    if(matchesAll) { model.addElement(p); nFound++; }
                }//for each point
            }//list
            else
            {
                for(Iterator iter = age.iterator(); iter.hasNext(); )
                {
                    search((AGE)iter.next());
                }
            }//not a list
        }
    //}}}
    
    //{{{ Utility/debugging functions
    //##################################################################################################
        // Convenience functions for debugging
        void echo(String s) { System.err.println(s); } // like Unix 'echo'
        void echon(String s) { System.err.print(s); }  // like Unix 'echo -n'
    //}}}
    }//inner class
}//class
