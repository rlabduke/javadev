/*
 * $Id: PdfNumber.java,v 1.39 2004/02/07 10:28:44 blowagie Exp $
 * $Name:  $
 *
 * Copyright 1999, 2000, 2001, 2002 Bruno Lowagie
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * LGPL license (the "GNU LIBRARY GENERAL PUBLIC LICENSE"), in which case the
 * provisions of LGPL are applicable instead of those above.  If you wish to
 * allow use of your version of this file only under the terms of the LGPL
 * License and not to allow others to use your version of this file under
 * the MPL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the LGPL.
 * If you do not delete the provisions above, a recipient may use your version
 * of this file under either the MPL or the GNU LIBRARY GENERAL PUBLIC LICENSE.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the MPL as stated above or under the terms of the GNU
 * Library General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library general Public License for more
 * details.
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */

package com.lowagie.text.pdf;

/**
 * <CODE>PdfNumber</CODE> provides two types of numbers, integer and real.
 * <P>
 * Integers may be specified by signed or unsigned constants. Reals may only be
 * in decimal format.<BR>
 * This object is described in the 'Portable Document Format Reference Manual version 1.3'
 * section 4.3 (page 37).
 *
 * @see		PdfObject
 * @see		BadPdfFormatException
 */

public class PdfNumber extends PdfObject {
    
/** actual value of this <CODE>PdfNumber</CODE>, represented as a <CODE>double</CODE> */
    private double value;
    
    // constructors
    
/**
 * Constructs a <CODE>PdfNumber</CODE>-object.
 *
 * @param		content			value of the new <CODE>PdfNumber</CODE>-object
 */
    
    public PdfNumber(String content) {
        super(NUMBER);
        try {
            value = Double.valueOf(content.trim()).doubleValue();
            setContent(content);
        }
        catch (NumberFormatException nfe){
            throw new RuntimeException(content + " is not a valid number - " + nfe.toString());
        }
    }
    
/**
 * Constructs a new INTEGER <CODE>PdfNumber</CODE>-object.
 *
 * @param		value				value of the new <CODE>PdfNumber</CODE>-object
 */
    
    public PdfNumber(int value) {
        super(NUMBER);
        this.value = value;
        setContent(String.valueOf(value));
    }
    
/**
 * Constructs a new REAL <CODE>PdfNumber</CODE>-object.
 *
 * @param		value				value of the new <CODE>PdfNumber</CODE>-object
 */
    
    public PdfNumber(double value) {
        super(NUMBER);
        this.value = value;
        setContent(ByteBuffer.formatDouble(value));
    }
    
/**
 * Constructs a new REAL <CODE>PdfNumber</CODE>-object.
 *
 * @param		value				value of the new <CODE>PdfNumber</CODE>-object
 */
    
    public PdfNumber(float value) {
        this((double)value);
    }
    
    // methods returning the value of this object
    
/**
 * Returns the primitive <CODE>int</CODE> value of this object.
 *
 * @return		a value
 */
    
    public int intValue() {
        return (int) value;
    }
    
/**
 * Returns the primitive <CODE>double</CODE> value of this object.
 *
 * @return		a value
 */
    
    public double doubleValue() {
        return value;
    }
    
    public float floatValue() {
        return (float)value;
    }
    
    // other methods
    
/**
 * Increments the value of the <CODE>PdfNumber</CODE>-object with 1.
 */
    
    public void increment() {
        value += 1.0;
        setContent(ByteBuffer.formatDouble(value));
    }
}