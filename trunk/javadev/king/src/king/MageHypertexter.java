// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king;
import king.core.*;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.util.SoftLog;
//}}}

public class MageHypertexter implements MageHypertextListener
{
//{{{ Static fields
//}}}

//{{{ Variable definitions
//##################################################################################################
    KingMain kMain;
//}}}

//{{{ Constructors
//##################################################################################################
    /**
    * Constructor
    */
    public MageHypertexter(KingMain kmain) {
        this.kMain = kmain;
    }
//}}}

//{{{ mageHypertextHit
//##################################################################################################
    public void mageHypertextHit(String link)
    {
        try
        {
            KinfileTokenizer token = new KinfileTokenizer(new LineNumberReader(new StringReader(link)));
            while(!token.isEOF())
            {
                //if(!(token.isLiteral() || token.isProperty())) continue;
                String cmd = token.getString().toLowerCase();
                token.advance();
                if(cmd.equals("kinemage") || cmd.equals("kin"))         doKinToken(token);
                else if(cmd.equals("view") || cmd.equals("v="))         doViewToken(token);
                else if(cmd.startsWith("master") || cmd.equals("m="))   doMasterToken(token);
                else if(cmd.equals("alloff"))                           doAllOffToken();
                //else SoftLog.err.println("Unexpected hypertext token: "+cmd);
            }
        }
        catch(IOException ex) {}
    }
//}}}

//{{{ doKinToken, doViewToken
//##################################################################################################
    public void doKinToken(KinfileTokenizer token) throws IOException
    {
        if(token.isInteger())
        {
            kMain.getStable().changeCurrentKinemage(token.getInt());
            token.advance();
        }
    }
    
    public void doViewToken(KinfileTokenizer token) throws IOException
    {
        if(token.isInteger())
        {
            try
            {
                Kinemage kin = kMain.getStable().getKinemage();
                KingView view = (KingView)kin.getViewList().get(token.getInt() - 1);
                kin.notifyViewSelected(view);
            }
            catch(IndexOutOfBoundsException ex) {}
            token.advance();
        }
    }
//}}}

//{{{ doMasterToken, doAllOffToken
//##################################################################################################
    public void doMasterToken(KinfileTokenizer token) throws IOException
    {
        if(token.isIdentifier())
        {
            String masterName = token.getString();
            token.advance();
            if(token.isLiteral())
            {
                String masterAlive = token.getString().toLowerCase();
                token.advance();
                Kinemage kin = kMain.getKinemage();
                if(masterAlive.equals("on"))        kin.getMasterByName(masterName).setOn(true);
                else if(masterAlive.equals("off"))  kin.getMasterByName(masterName).setOn(false);
                kMain.notifyChange(KingMain.EM_ON_OFF);
            }
        }
    }

    public void doAllOffToken() {
        Kinemage kin = kMain.getKinemage();
        Collection masters = kin.masterList();
        Iterator iter = masters.iterator();
        while (iter.hasNext()) {
            MasterGroup master = (MasterGroup) iter.next();
            master.setOn(false);
        }
    }
//}}}
}//class

