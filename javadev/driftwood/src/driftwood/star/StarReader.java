// (jEdit options) :folding=explicit:collapseFolds=1:
//{{{ Package, imports
package driftwood.star;

//import java.awt.*;
//import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.*;
//import java.util.regex.*;
//import javax.swing.*;
//import driftwood.*;
//}}}
/**
* <code>StarReader</code> can read a STAR file from a stream and construct
* a Document Object Model of it in memory as a StarFile object.
*
* <p>Copyright (C) 2004 by Ian W. Davis. All rights reserved.
* <br>Begun on Wed May 19 16:18:19 EDT 2004
*/
public class StarReader //extends ... implements ...
{
//{{{ Constants
//}}}

//{{{ Variable definitions
//##############################################################################
    LineNumberReader    input   = null;
    StarTokenizer       token   = null;
    StarFile            dom     = null;
//}}}

//{{{ Constructor(s)
//##############################################################################
    public StarReader()
    {
        super();
    }
//}}}

//{{{ parse
//##############################################################################
    /**
    * Parses the STAR file and creates a StarFile object from it.
    * Any syntax errors are fatal and will result in an ParseException.
    */
    public StarFile parse(LineNumberReader in) throws IOException, ParseException
    {
        this.input = in;
        this.token = new StarTokenizer(in);
        this.dom = new StarFile();
        
        while(!token.isEOF())
        {
            if(token.isGlobal())
            {
                DataCell global = dom.getGlobalBlock();
                token.advance();
                doGlobal(global);
            }
            else if(token.isData())
            {
                DataBlock block = new DataBlock(token.getString());
                if(dom.addDataBlock(block) != null)
                    throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
                    +"Repeated data block name: data_"+block, input.getLineNumber()+1);
                token.advance();
                doDataBlock(block);
            }
            else
                throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
                +"Illegal token type '"+token.getType()+"' at top level", input.getLineNumber()+1);
        }
        
        return this.dom;
    }
//}}}

//{{{ doGlobal
//##############################################################################
    protected void doGlobal(DataCell global) throws IOException, ParseException
    {
        while(!(token.isEOF() || token.isGlobal() || token.isData()))
        {
            if(token.isLoopStart())
            {
                token.advance();
                doLoop(global);
            }
            else if(token.isName())
                doItem(global);
            else
                throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
                +"Illegal token type '"+token.getType()+"' in global block", input.getLineNumber()+1);
        }
    }
//}}}

//{{{ doDataBlock
//##############################################################################
    protected void doDataBlock(DataBlock block) throws IOException, ParseException
    {
        while(!(token.isEOF() || token.isGlobal() || token.isData()))
        {
            if(token.isLoopStart())
            {
                token.advance();
                doLoop(block);
            }
            else if(token.isName())
                doItem(block);
            else if(token.isSaveStart())
            {
                DataCell frame = new DataCell(token.getString());
                if(block.addSaveFrame(frame) != null)
                    throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
                    +"Repeated save frame name: save_"+frame, input.getLineNumber()+1);
                token.advance();
                doSaveFrame(frame);
            }
            else
                throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
                +"Illegal token type '"+token.getType()+"' in data block", input.getLineNumber()+1);
        }
    }
//}}}

//{{{ doSaveFrame
//##############################################################################
    protected void doSaveFrame(DataCell frame) throws IOException, ParseException
    {
        while(!(token.isEOF() || token.isSaveEnd()))
        {
            if(token.isLoopStart())
            {
                token.advance();
                doLoop(frame);
            }
            else if(token.isName())
                doItem(frame);
            else
                throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
                +"Illegal token type '"+token.getType()+"' in save frame", input.getLineNumber()+1);
        }
    }
//}}}

//{{{ doLoop
//##############################################################################
    protected void doLoop(DataCell cell) throws IOException, ParseException
    {
        List names = new ArrayList();
        while(!token.isEOF() && token.isName())
        {
            names.add(token.getString());
            token.advance();
        }
        if(names.size() == 0)
            throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
            +"No data names declared for loop_ (0 columns)", input.getLineNumber()+1);
        
        List[] values = new List[names.size()];
        for(int i = 0; i < values.length; i++) values[i] = new ArrayList();
        
        int row = 0, col = 0;
        while(!token.isEOF() && token.isValue())
        {
            values[col].add(token.getString());
            token.advance();
            col++;
            if(col % values.length == 0)
            {
                col = 0;
                row++;
            }
        }
        
        if(col != 0)
        {
            /* debugging * /
            for(int j = 0; j < row; j++)
            {
                for(int i = 0; i < values.length; i++)
                    System.err.print(values[i].get(j)+" ");
                System.err.println();
            }
            System.err.println("-----");
            for(int i = 0; i < col; i++)
                System.err.println(values[i].get(row));
            /* debugging */
            throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
            +"Not enough values to complete row "+(row+1)+" in loop_", input.getLineNumber()+1);
        }
        if(row == 0)
            throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
            +"No data values declared for loop_ (0 rows)", input.getLineNumber()+1);
        
        for(int i = 0; i < names.size(); i++)
            cell.putItem((String)names.get(i), values[i]);
    }
//}}}

//{{{ doItem
//##############################################################################
    protected void doItem(DataCell cell) throws IOException, ParseException
    {
        if(!token.isName())
            throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
            +"Illegal token type '"+token.getType()+"' when data name was expected", input.getLineNumber()+1);
        String name = token.getString();
        
        token.advance();
        if(!token.isValue())
            throw new ParseException("[line "+(input.getLineNumber()+1)+"] "
            +"Illegal token type '"+token.getType()+"' when data value was expected", input.getLineNumber()+1);
        String value = token.getString();
        
        cell.putItem(name, value);
        token.advance();
    }
//}}}

//{{{ empty_code_segment
//##############################################################################
//}}}

//{{{ main (for testing)
//##############################################################################
    /* for testing */
    public static void main(String[] args) throws IOException, ParseException
    {
        LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
        StarReader reader = new StarReader();
        StarFile dom = reader.parse(in);
        
        System.out.println("Global items:");
        System.out.println(dom.getGlobalBlock().getItemNames());
        System.out.println();
        
        System.out.println("Data blocks:");
        System.out.println(dom.getDataBlockNames());
        System.out.println();
        
        for(Iterator iter = dom.getDataBlockNames().iterator(); iter.hasNext(); )
        {
            String name = (String) iter.next();
            DataBlock block = (DataBlock) dom.getDataBlock(name);
            System.out.println("DATA_"+block+" save frames:");
            System.out.println(block.getSaveFrames());
            System.out.println();
            System.out.println("DATA_"+block+" items:");
            System.out.println(block.getItemNames());
            System.out.println();
        }
    }
    /* for testing */
//}}}
}//class

