// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package molikin;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
import driftwood.data.*;
import driftwood.moldb2.*;
import driftwood.util.Strings;
//}}}
/**
* <code>ResRanger</code> is responsible for dealing with user-specified ranges of
* Residues, where ranges are based on insertion code and sequence "number"
* (which can be an arbitrary string in mmCIF files, but usually isn't).
* For this class, a residue "number" is a string consisting of the trimmed
* Residue number concatenated with its trimmed insertion code.
*
* <p>Copyright (C) 2005 by Ian W. Davis. All rights reserved.
* <br>Begun on Sat Oct  1 07:35:35 EDT 2005
*/
public class ResRanger //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ CLASS: ResNum
//##############################################################################
    /**
    * For sorting residue numbers into a meaningful order.
    * Residues are sorted first by number (if it's really a number)
    * and then by insertion code.
    * If the number is not a number, it's trimmed and contatenated with the
    * insertion code, and those sort as strings at the end of the group.
    */
    static class ResNum implements Comparable
    {
        int num = Integer.MAX_VALUE;
        String ins;
        String callme;
        
        public ResNum(Residue r)
        {
            try
            {
                num = Integer.parseInt(r.getSequenceNumber().trim());
                ins = r.getInsertionCode().trim();
                callme = num+ins;
            }
            catch(NumberFormatException ex)
            {
              if (r.getSequenceInteger() != Residue.NAN_SEQ) {
                num = r.getSequenceInteger();
                ins = r.getInsertionCode().trim();
                callme = r.getSequenceNumber().trim() + ins;
              } else {
                ins = r.getSequenceNumber().trim() + r.getInsertionCode().trim();
                callme = num+ins;
              }
            }
        }
        
        public boolean equals(Object o)
        { return this.callme.equals(((ResNum)o).callme); }
        
        public int hashCode()
        { return this.callme.hashCode(); }
        
        public int compareTo(Object o)
        {
            ResNum that = (ResNum) o;
            if(this.num == that.num) return this.ins.compareTo(that.ins);
            else return this.num - that.num;
        }
    }
//}}}

//{{{ Variable definitions
//##############################################################################
    // List of "callme" strings from the ResNums
    ArrayList   callmes = new ArrayList();
    // Bit flags for whether given numbers are selected or not.
    BitSet      resSel  = new BitSet();
//}}}

//{{{ Constructor(s)
//##############################################################################
    /**
    * Initializes this object with the universe of Residue numbers
    * in which selections will be made.
    * @param    residues    the Residues that selections will apply to
    */
    public ResRanger(Collection residues)
    {
        super();
        
        // Collect all unique ResNums
        Set resNums = new CheapSet();
        for(Iterator iter = residues.iterator(); iter.hasNext(); )
            resNums.add(new ResNum((Residue) iter.next()));
        
        // Sort them into their natural order
        ResNum[] rNums = (ResNum[]) resNums.toArray( new ResNum[resNums.size()] );
        Arrays.sort(rNums);
        
        // Create a list of callme strings for later lookups
        for(int i = 0; i < rNums.length; i++)
            callmes.add(rNums[i].callme);
    }
//}}}

//{{{ select(String)
//##############################################################################
    /**
    * Sets the selection to match the given string, which consists of
    * comma-separated ranges, which are either
    * single numbers or pairs separated by a dash (-).
    * You may want to convert the selection to upper case first,
    * as insertion codes are usually upper case if anything.
    * This is an order M*N algorithm, where M is the count of numbers in the
    * selection statement and N is the count of unique residue numbers.
    */
    public void select(String selection)
    {
        resSel.clear(); // no residues selected
        String[] ranges = Strings.explode(selection, ',', false, true);
        for(int j = 0; j < ranges.length; j++)
        {
            int k = ranges[j].indexOf('-', 1); // first dash, not counting leading minus
            if(k == -1) // single number
            {
                int i = callmes.indexOf( ranges[j] ); // already trimmed by explode()
                if(i != -1) resSel.set(i);
            }
            else // range of numbers
            {
                int i1 = callmes.indexOf( ranges[j].substring(0, k).trim() );
                int i2 = callmes.indexOf( ranges[j].substring(k+1 ).trim() );
                if(i1 == -1 || i2 == -1) {} // uninterpretable range -- can't find endpoint(s)
                else if(i1 > i2)    resSel.set(i2, i1+1); // our endpoint in inclusive,
                else                resSel.set(i1, i2+1); // BitSet is exclusive -- hence, +1
            }
        }
    }
//}}}

//{{{ select(int[])
//##############################################################################
    /**
    * Selects residue numbers by their index in the list.
    * This is mostly intended for use with a JList.
    */
    public void select(int[] indices)
    {
        resSel.clear();
        if(indices == null) return;
        for(int i = 0; i < indices.length; i++)
            resSel.set(indices[i]);
    }
//}}}

//{{{ getSelectionString
//##############################################################################
    /** Translates the current selection back into a (well-formed) String. */
    public String getSelectionString()
    {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for(int i = 0; i < callmes.size(); i++)
        {
            if(resSel.get(i))
            {
                if(first) first = false;
                else buf.append(", ");
                buf.append(callmes.get(i));
                if(resSel.get(i+1)) // next number also selected
                {
                    buf.append("-");
                    while(resSel.get(i) && i < callmes.size()) i++;
                    buf.append(callmes.get(i-1));
                }
            }
        }
        return buf.toString();
    }
//}}}

//{{{ getAllNumbers, getSelectionMask, getSelectedNumbers
//##############################################################################
    /**
    * Returns a set of all unique residue numbers + insertion codes as Strings,
    * in sorted order.
    */
    public Collection getAllNumbers()
    { return Collections.unmodifiableCollection(callmes); }
    
    /** Returns a mask of which numbers (of getAllNumbers()) are selected. */
    public BitSet getSelectionMask()
    { return (BitSet) resSel.clone(); }
    
    /** Returns a Set of the selected numbers as Strings, in no particular order. */
    public Set getSelectedNumbers()
    {
        Set sel = new CheapSet();
        for(int i = 0; i < callmes.size(); i++)
        {
            if(resSel.get(i))
                sel.add(callmes.get(i));
        }
        return sel;
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}
}//class

