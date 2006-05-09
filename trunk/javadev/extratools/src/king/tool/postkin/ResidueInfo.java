// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package king.tool.postkin;

import driftwood.moldb2.Residue;

// This class is intended to store information about a residue so it can be used
// as a hashmap key.  This allows me to deal with alternate conformations (e.g. in 
// the geometry-analyzing plugin).
public class ResidueInfo extends Residue { 
    

//{{{ Variable definitions
//###############################################################
    //int resNum;
    //String resName;

//}}}


    public ResidueInfo(String chain, String segment, String seqNum, String insCode, String resName) {
	//resNum = rNum;
	//resName = rName;
	super(chain, segment, seqNum, insCode, resName);
    }

    //{{{ compareTo, toString
//##################################################################################################
    /**
    * Copied from Residue, but changed so it doesn't use memory location, so residues with same name are equal.
    */
    public int compareTo(Object o)
    {
        if(o == null) return 1; // null sorts to front
        ResidueInfo r1 = this;
        ResidueInfo r2 = (ResidueInfo)o;
        
        int comp = r1.getChain().compareTo(r2.getChain());
        if(comp != 0) return comp;
        comp = r1.getSegment().compareTo(r2.getSegment());
        if(comp != 0) return comp;
        
        comp = r1.getSequenceInteger() - r2.getSequenceInteger();
        if(comp != 0) return comp;
        // seqNums could still differ by whitespace...
        comp = r1.getSequenceNumber().compareTo(r2.getSequenceNumber());
        if(comp != 0) return comp;
        comp = r1.getInsertionCode().compareTo(r2.getInsertionCode());
        if(comp != 0) return comp;
        comp = r1.getName().compareTo(r2.getName());
        if(comp != 0) return comp;
        
        return 0;
    }
    /*
    public int getNumber() {
	return resNum;
    }

    public String getName() {
	return resName;
    }

    public int compareTo(ResidueInfo rinfo) {
	int resnumComp = this.getNumber() - rinfo.getNumber();
	int resnameComp = this.getName().compareTo(rinfo.getName());
	if (resnumComp < 0) return -1;
	if (resnumComp > 0) return 1;
	return resnameComp;
    }

    public int compareTo(Object obj) {
	if (obj instanceof ResidueInfo) {
	    return this.compareTo((ResidueInfo) obj);
	} else {
	    throw new ClassCastException("Not a ResidueInfo object!");
	}
    }

    public int hashCode() {
	String combine = (new Integer(resNum)).toString() + resName;
	return combine.hashCode();
    }

    public boolean equals(Object obj) {
	if (obj instanceof ResidueInfo) {
	    ResidueInfo resinfo2 = (ResidueInfo) obj;
	    return ((this.getNumber()==resinfo2.getNumber())&&(this.getName().equals(resinfo2.getName())));
	}
	return false;
    }
    */
}
