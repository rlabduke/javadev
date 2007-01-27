/*
 * $Id: PdfReader.java,v 1.41 2004/02/10 16:36:03 blowagie Exp $
 * $Name:  $
 *
 * Copyright 2001, 2002 Paulo Soares
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.zip.InflaterInputStream;
import java.util.Arrays;

import com.lowagie.text.Rectangle;
import com.lowagie.text.PageSize;

/** Reads a PDF document and prepares it to import pages to our
 * document. This class is thread safe; this means that
 * a single instance can serve as many output documents as needed and can even be static.
 * @author Paulo Soares (psoares@consiste.pt)
 * @author Kazuya Ujihara
 */
public class PdfReader {
    
    static final PdfName pageInhCandidates[] = {
        PdfName.MEDIABOX, PdfName.ROTATE, PdfName.RESOURCES, PdfName.CROPBOX
    };
    
    static final byte endstream[] = {'e','n','d','s','t','r','e','a','m'};
    static final byte endobj[] = {'e','n','d','o','b','j'};
    protected PRTokeniser tokens;
    protected int xref[];
    protected PdfObject xrefObj[];
    protected PdfDictionary trailer;
    protected PdfDictionary pages[];
    protected PdfDictionary catalog;
    protected PRIndirectReference pageRefs[];
    protected PRAcroForm acroForm = null;
    protected boolean acroFormParsed = false;
    protected ArrayList pageInh;
    protected int pagesCount;
    protected boolean encrypted = false;
    protected boolean rebuilt = false;
    protected int freeXref;
    protected boolean tampered = false;
    protected int lastXref;
    protected int eofPos;
    protected char pdfVersion;
    protected PdfEncryption decrypt;
    protected byte password[] = null; //added by ujihara for decryption
    protected ArrayList strings = new ArrayList();
    protected boolean sharedStreams = true;
    protected boolean consolidateNamedDestinations = false;
    protected int rValue;
    protected int pValue;
    private int objNum;
    private int objGen;
    private boolean visited[];
    private IntHashtable newHits;
    
    protected PdfReader() {
    }
    
    /** Reads and parses a PDF document.
     * @param filename the file name of the document
     * @throws IOException on error
     */
    public PdfReader(String filename) throws IOException {
        this(filename, null);
    }
    
    /** Reads and parses a PDF document.
     * @param filename the file name of the document
     * @param ownerPassword the password to read the document
     * @throws IOException on error
     */    
    public PdfReader(String filename, byte ownerPassword[]) throws IOException {
        password = ownerPassword;
        tokens = new PRTokeniser(filename);
        readPdf();
    }
    
    /** Reads and parses a PDF document.
     * @param pdfIn the byte array with the document
     * @throws IOException on error
     */
    public PdfReader(byte pdfIn[]) throws IOException {
        this(pdfIn, null);
    }
    
    /** Reads and parses a PDF document.
     * @param pdfIn the byte array with the document
     * @param ownerPassword the password to read the document
     * @throws IOException on error
     */
    public PdfReader(byte pdfIn[], byte ownerPassword[]) throws IOException {
        password = ownerPassword;
        tokens = new PRTokeniser(pdfIn);
        readPdf();
    }
    
    /** Reads and parses a PDF document.
     * @param url the URL of the document
     * @throws IOException on error
     */
    public PdfReader(URL url) throws IOException {
        this(url, null);
    }
    
    /** Reads and parses a PDF document.
     * @param url the URL of the document
     * @param ownerPassword the password to read the document
     * @throws IOException on error
     */
    public PdfReader(URL url, byte ownerPassword[]) throws IOException {
        password = ownerPassword;
        tokens = new PRTokeniser(new RandomAccessFileOrArray(url));
        readPdf();
    }
    
    /** Creates an independent duplicate.
     * @param reader the <CODE>PdfReader</CODE> to duplicate
     */    
    public PdfReader(PdfReader reader) {
        this.consolidateNamedDestinations = reader.consolidateNamedDestinations;
        this.encrypted = reader.encrypted;
        this.rebuilt = reader.rebuilt;
        this.sharedStreams = reader.sharedStreams;
        this.tampered = reader.tampered;
        this.password = reader.password;
        this.pdfVersion = reader.pdfVersion;
        this.eofPos = reader.eofPos;
        this.freeXref = reader.freeXref;
        this.lastXref = reader.lastXref;
        this.pagesCount = reader.pagesCount;
        this.tokens = reader.tokens;
        this.decrypt = reader.decrypt;
        this.pValue = reader.pValue;
        this.rValue = reader.rValue;
        this.xrefObj = new PdfObject[reader.xrefObj.length];
        for (int k = 0; k < reader.xrefObj.length; ++k) {
            this.xrefObj[k] = duplicatePdfObject(reader.xrefObj[k], this);
        }
        this.pageRefs = new PRIndirectReference[reader.pageRefs.length];
        this.pages = new PdfDictionary[reader.pages.length];
        for (int k = 0; k < reader.pageRefs.length; ++k) {
            this.pageRefs[k] = (PRIndirectReference)duplicatePdfObject(reader.pageRefs[k], this);
            this.pages[k] = (PdfDictionary)getPdfObject(this.pageRefs[k]);
        }
        this.trailer = (PdfDictionary)duplicatePdfObject(reader.trailer, this);
        this.catalog = (PdfDictionary)getPdfObject(trailer.get(PdfName.ROOT));
    }
    
    /** Gets a new file instance of the original PDF
     * document.
     * @return a new file instance of the original PDF document
     */
    public RandomAccessFileOrArray getSafeFile() {
        return tokens.getSafeFile();
    }
    
    protected PdfReaderInstance getPdfReaderInstance(PdfWriter writer) {
        return new PdfReaderInstance(this, writer, xrefObj, pages);
    }
    
    /** Gets the number of pages in the document.
     * @return the number of pages in the document
     */
    public int getNumberOfPages() {
        return pages.length;
    }
    
    /** Returns the document's catalog. This dictionary is not a copy,
     * any changes will be reflected in the catalog.
     * @return the document's catalog
     */
    public PdfDictionary getCatalog() {
        return catalog;
    }
    
    /** Returns the document's acroform, if it has one.
     * @return he document's acroform
     */
    public PRAcroForm getAcroForm() {
        if (!acroFormParsed) {
            acroFormParsed = true;
            PdfObject form = catalog.get(PdfName.ACROFORM);
            if (form != null) {
                try {
                    acroForm = new PRAcroForm(this);
                    acroForm.readAcroForm((PdfDictionary)getPdfObject(form));
                }
                catch (Exception e) {
                    acroForm = null;
                }
            }
        }
        return acroForm;
    }
    /**
     * Gets the page rotation. This value can be 0, 90, 180 or 270.
     * @param index the page number. The first page is 1
     * @return the page rotation
     */
    public int getPageRotation(int index) {
        PdfDictionary page = pages[index - 1];
        PdfNumber rotate = (PdfNumber)getPdfObject(page.get(PdfName.ROTATE));
        if (rotate == null)
            return 0;
        else {
            int n = rotate.intValue();
            n %= 360;
            return n < 0 ? n + 360 : n;
        }
    }
    
    /** Gets the page size, taking rotation into account. This
     * is a <CODE>Rectangle</CODE> with the value of the /MediaBox and the /Rotate key.
     * @param index the page number. The first page is 1
     * @return a <CODE>Rectangle</CODE>
     */
    public Rectangle getPageSizeWithRotation(int index) {
        Rectangle rect = getPageSize(index);
        int rotation = getPageRotation(index);
        while (rotation > 0) {
            rect = rect.rotate();
            rotation -= 90;
        }
        return rect;
    }
    
    /** Gets the page size without taking rotation into account. This
     * is the value of the /MediaBox key.
     * @param index the page number. The first page is 1
     * @return the page size
     */
    public Rectangle getPageSize(int index) {
        PdfDictionary page = pages[index - 1];
        PdfArray mediaBox = (PdfArray)getPdfObject(page.get(PdfName.MEDIABOX));
        return getNormalizedRectangle(mediaBox);
    }
    
    /** Gets the crop box without taking rotation into account. This
     * is the value of the /CropBox key. The crop box is the part
     * of the document to be displayed or printed. It usually is the same
     * as the media box but may be smaller.
     * @param index the page number. The first page is 1
     * @return the crop box
     */
    public Rectangle getCropBox(int index) {
        PdfDictionary page = pages[index - 1];
        PdfArray cropBox = (PdfArray)getPdfObject(page.get(PdfName.CROPBOX));
        if (cropBox == null)
            return getPageSize(index);
        return getNormalizedRectangle(cropBox);
    }
    
    /** Returns the content of the document information dictionary as a <CODE>HashMap</CODE>
     * of <CODE>String</CODE>.
     * @return content of the document information dictionary
     */
    public HashMap getInfo() {
        HashMap map = new HashMap();
        PdfDictionary info = (PdfDictionary)getPdfObject(trailer.get(PdfName.INFO));
        if (info == null)
            return map;
        for (Iterator it = info.getKeys().iterator(); it.hasNext();) {
            PdfName key = (PdfName)it.next();
            PdfObject obj = (PdfObject)getPdfObject(info.get(key));
            if (obj == null)
                continue;
            String value = obj.toString();
            switch (obj.type()) {
                case PdfObject.STRING: {
                    value = ((PdfString)obj).toUnicodeString();
                    break;
                }
                case PdfObject.NAME: {
                    value = PdfName.decodeName(value);
                    break;
                }
            }
            map.put(PdfName.decodeName(key.toString()), value);
        }
        return map;
    }
    
    /** Normalizes a <CODE>Rectangle</CODE> so that llx and lly are smaller than urx and ury.
     * @param box the original rectangle
     * @return a normalized <CODE>Rectangle</CODE>
     */    
    public static Rectangle getNormalizedRectangle(PdfArray box) {
        ArrayList rect = box.getArrayList();
        float llx = ((PdfNumber)rect.get(0)).floatValue();
        float lly = ((PdfNumber)rect.get(1)).floatValue();
        float urx = ((PdfNumber)rect.get(2)).floatValue();
        float ury = ((PdfNumber)rect.get(3)).floatValue();
        return new Rectangle(Math.min(llx, urx), Math.min(lly, ury),
        Math.max(llx, urx), Math.max(lly, ury));
    }
    
    protected void readPdf() throws IOException {
        try {
            pdfVersion = tokens.checkPdfHeader();
            try {
                readXref();
            }
            catch (Exception e) {
                try {
                    rebuilt = true;
                    rebuildXref();
                    lastXref = -1;
                }
                catch (Exception ne) {
                    throw new IOException("Rebuild failed: " + ne.getMessage() + "; Original message: " + e.getMessage());
                }
            }
            readDocObj();
            
            readDecryptedDocObj();	//added by ujihara for decryption
            strings.clear();
            readPages();
            eliminateSharedStreams();
        }
        finally {
            try {
                tokens.close();
            }
            catch (Exception e) {
                // empty on purpose
            }
        }
    }
    
    private boolean equalsArray(byte ar1[], byte ar2[], int size) {
        for (int k = 0; k < size; ++k) {
            if (ar1[k] != ar2[k])
                return false;
        }
        return true;
    }
    
    /**
     */
    private void readDecryptedDocObj() throws IOException {
        PdfObject encDic = trailer.get(PdfName.ENCRYPT);
        if (encDic == null || encDic.toString().equals("null"))
            return;
        encrypted = true;
        PdfDictionary enc = (PdfDictionary)getPdfObject(encDic);
        
        String s;
        PdfObject o;
        
        PdfArray documentIDs = (PdfArray)getPdfObject(trailer.get(PdfName.ID));
        byte documentID[] = null;
        if (documentIDs != null) {
            o = (PdfObject)documentIDs.getArrayList().get(0);
            s = o.toString();
            documentID = com.lowagie.text.DocWriter.getISOBytes(s);
        }
        
        s = enc.get(PdfName.U).toString();
        byte uValue[] = com.lowagie.text.DocWriter.getISOBytes(s);
        s = enc.get(PdfName.O).toString();
        byte oValue[] = com.lowagie.text.DocWriter.getISOBytes(s);
        
        o = enc.get(PdfName.R);
        if (!o.isNumber()) throw new IOException("Illegal R value.");
        rValue = ((PdfNumber)o).intValue();
        if (rValue != 2 && rValue != 3) throw new IOException("Unknown encryption type (" + rValue + ")");
        
        o = enc.get(PdfName.P);
        if (!o.isNumber()) throw new IOException("Illegal P value.");
        pValue = ((PdfNumber)o).intValue();
        
        decrypt = new PdfEncryption();
        
        //check by user password
        decrypt.setupByUserPassword(documentID, password, oValue, pValue, rValue == 3);
        if (!equalsArray(uValue, decrypt.userKey, rValue == 3 ? 16 : 32)) {
            //check by owner password
            decrypt.setupByOwnerPassword(documentID, password, uValue, oValue, pValue, rValue == 3);
            if (!Arrays.equals(uValue, decrypt.userKey)) {
                throw new IOException("Bad user password");
            }
        }
        for (int k = 0; k < strings.size(); ++k) {
            PdfString str = (PdfString)strings.get(k);
            str.decrypt(this);
        }
        if (encDic.isIndirect())
            xrefObj[((PRIndirectReference)encDic).getNumber()] = null;
    }
    
    /** Reads a <CODE>PdfObject</CODE> resolving an indirect reference
     * if needed.
     * @param obj the <CODE>PdfObject</CODE> to read
     * @return the resolved <CODE>PdfObject</CODE>
     */    
    public static PdfObject getPdfObject(PdfObject obj) {
        if (obj == null)
            return null;
        if (!obj.isIndirect())
            return obj;
        PRIndirectReference ref = (PRIndirectReference)obj;
        int idx = ref.getNumber();
        obj = ref.getReader().xrefObj[idx];
        if (obj == null)
            return PdfNull.PDFNULL;
        else
            return obj;
    }
    
    protected void pushPageAttributes(PdfDictionary nodePages) {
        PdfDictionary dic = new PdfDictionary();
        if (pageInh.size() != 0) {
            dic.putAll((PdfDictionary)pageInh.get(pageInh.size() - 1));
        }
        for (int k = 0; k < pageInhCandidates.length; ++k) {
            PdfObject obj = nodePages.get(pageInhCandidates[k]);
            if (obj != null)
                dic.put(pageInhCandidates[k], obj);
        }
        pageInh.add(dic);
    }
    
    protected void popPageAttributes() {
        pageInh.remove(pageInh.size() - 1);
    }
    
    protected void iteratePages(PdfDictionary page) throws IOException {
        PdfArray kidsPR = (PdfArray)getPdfObject(page.get(PdfName.KIDS));
        if (kidsPR == null) {
            page.put(PdfName.TYPE, PdfName.PAGE);
            PdfDictionary dic = (PdfDictionary)pageInh.get(pageInh.size() - 1);
            PdfName key;
            for (Iterator i = dic.getKeys().iterator(); i.hasNext();) {
                key = (PdfName)i.next();
                if (page.get(key) == null)
                    page.put(key, dic.get(key));
            }
            if (page.get(PdfName.MEDIABOX) == null) {
                PdfArray arr = new PdfArray(new float[]{0,0,PageSize.LETTER.right(),PageSize.LETTER.top()});
                page.put(PdfName.MEDIABOX, arr);
            }
            pages[pagesCount++] = page;
        }
        else {
            page.put(PdfName.TYPE, PdfName.PAGES);
            pushPageAttributes(page);
            ArrayList kids = kidsPR.getArrayList();
            for (int k = 0; k < kids.size(); ++k){
                pageRefs[pagesCount] = (PRIndirectReference)kids.get(k);
                PdfDictionary kid = (PdfDictionary)getPdfObject(pageRefs[pagesCount]);
                iteratePages(kid);
            }
            popPageAttributes();
        }
    }
    
    protected void readPages() throws IOException {
        pageInh = new ArrayList();
        catalog = (PdfDictionary)getPdfObject(trailer.get(PdfName.ROOT));
        PdfDictionary rootPages = (PdfDictionary)getPdfObject(catalog.get(PdfName.PAGES));
        PdfNumber count = (PdfNumber)getPdfObject(rootPages.get(PdfName.COUNT));
        pages = new PdfDictionary[count.intValue()];
        pageRefs = new PRIndirectReference[pages.length];
        pagesCount = 0;
        iteratePages(rootPages);
        pageInh = null;
    }
    
    protected void PRSimpleRecursive(PdfObject obj) throws IOException {
        switch (obj.type()) {
            case PdfObject.DICTIONARY:
            case PdfObject.STREAM:
                PdfDictionary dic = (PdfDictionary)obj;
                for (Iterator it = dic.getKeys().iterator(); it.hasNext();) {
                    PdfName key = (PdfName)it.next();
                    PRSimpleRecursive(dic.get(key));
                }
                break;
            case PdfObject.ARRAY:
                ArrayList list = ((PdfArray)obj).getArrayList();
                for (int k = 0; k < list.size(); ++k) {
                    PRSimpleRecursive((PdfObject)list.get(k));
                }
                break;
            case PdfObject.INDIRECT:
                PRIndirectReference ref = (PRIndirectReference)obj;
                int num = ref.getNumber();
                if (!visited[num]) {
                    visited[num] = true;
                    newHits.put(num, 1);
                }
                break;
        }
    }
        
    protected void readDocObj() throws IOException {
        ArrayList streams = new ArrayList();
        xrefObj = new PdfObject[xref.length];
        visited = new boolean[xref.length];
        newHits = new IntHashtable();
        PRSimpleRecursive(trailer);
        while (!newHits.isEmpty()) {
            int hits[] = newHits.getKeys();
            newHits.clear();
            for (int k = 0; k < hits.length; ++k) {
                int pos = xref[hits[k]];
                if (pos <= 0)
                    continue;
                tokens.seek(pos);
                tokens.nextValidToken();
                if (tokens.getTokenType() != PRTokeniser.TK_NUMBER)
                    tokens.throwError("Invalid object number.");
                objNum = tokens.intValue();
                tokens.nextValidToken();
                if (tokens.getTokenType() != PRTokeniser.TK_NUMBER)
                    tokens.throwError("Invalid generation number.");
                objGen = tokens.intValue();
                tokens.nextValidToken();
                if (!tokens.getStringValue().equals("obj"))
                    tokens.throwError("Token 'obj' expected.");
                PdfObject obj = readPRObject();
                xrefObj[hits[k]] = obj;
                if (obj.isStream()) {
                    streams.add(obj);
                }
            }
        }
        visited = null;
        newHits = null;
        int fileLength = tokens.length();
        byte tline[] = new byte[16];
        for (int k = 0; k < streams.size(); ++k) {
            PRStream stream = (PRStream)streams.get(k);
            PdfNumber length = (PdfNumber)getPdfObject(stream.get(PdfName.LENGTH));
            int start = stream.getOffset();
            int streamLength = length.intValue();
            boolean calc = false;
            if (streamLength + start > fileLength - 20)
                calc = true;
            else {
                tokens.seek(start + streamLength);
                String line = tokens.readString(20);
                if (!line.startsWith("\nendstream") &&
                !line.startsWith("\r\nendstream") &&
                !line.startsWith("\rendstream") &&
                !line.startsWith("endstream"))
                    calc = true;
            }
            if (calc) {
                tokens.seek(start);
                while (true) {
                    int pos = tokens.getFilePointer();
                    if (!tokens.readLineSegment(tline))
                        break;
                    if (equalsn(tline, endstream)) {
                        streamLength = pos - start;
                        break;
                    }
                    if (equalsn(tline, endobj)) {
                        tokens.seek(pos - 16);
                        String s = tokens.readString(16);
                        int index = s.indexOf("endstream");
                        if (index >= 0)
                            pos = pos - 16 + index;
                        streamLength = pos - start;
                        break;
                    }
                }
            }
            stream.setLength(streamLength);
        }
        xref = null;
    }

    static PdfObject killIndirect(PdfObject obj) {
        if (obj == null || obj.isNull())
            return null;
        PdfObject ret = getPdfObject(obj);
        if (obj.isIndirect()) {
            PRIndirectReference ref = (PRIndirectReference)obj;
            ref.getReader().xrefObj[ref.getNumber()] = null;
        }
        return ret;
    }
    
    protected void readXref() throws IOException {
        tokens.seek(tokens.getStartxref());
        tokens.nextToken();
        if (!tokens.getStringValue().equals("startxref"))
            throw new IOException("startxref not found.");
        tokens.nextToken();
        if (tokens.getTokenType() != PRTokeniser.TK_NUMBER)
            throw new IOException("startxref is not followed by a number.");
        int startxref = tokens.intValue();
        lastXref = startxref;
        eofPos = tokens.getFilePointer();
        tokens.seek(startxref);
        int ch;
        do {
            ch = tokens.read();
        } while (ch != -1 && ch != 't');
        if (ch == -1)
            throw new IOException("Unexpected end of file.");
        tokens.backOnePosition(ch);
        tokens.nextValidToken();
        if (!tokens.getStringValue().equals("trailer"))
            throw new IOException("trailer not found.");
        trailer = (PdfDictionary)readPRObject();
        PdfNumber xrefSize = (PdfNumber)trailer.get(PdfName.SIZE);
        xref = new int[xrefSize.intValue()];
        tokens.seek(startxref);
        readXrefSection();
        PdfDictionary trailer2 = trailer;
        while (true) {
            PdfNumber prev = (PdfNumber)trailer2.get(PdfName.PREV);
            if (prev == null)
                break;
            tokens.seek(prev.intValue());
            readXrefSection();
            trailer2 = (PdfDictionary)readPRObject();
        }
    }
    
    protected void readXrefSection() throws IOException {
        tokens.nextValidToken();
        if (!tokens.getStringValue().equals("xref"))
            tokens.throwError("xref subsection not found");
        int start = 0;
        int end = 0;
        int pos = 0;
        int gen = 0;
        while (true) {
            tokens.nextValidToken();
            if (tokens.getStringValue().equals("trailer"))
                break;
            if (tokens.getTokenType() != PRTokeniser.TK_NUMBER)
                tokens.throwError("Object number of the first object in this xref subsection not found");
            start = tokens.intValue();
            tokens.nextValidToken();
            if (tokens.getTokenType() != PRTokeniser.TK_NUMBER)
                tokens.throwError("Number of entries in this xref subsection not found");
            end = tokens.intValue() + start;
            if (start == 1) { // fix incorrect start number
                int back = tokens.getFilePointer();
                tokens.nextValidToken();
                pos = tokens.intValue();
                tokens.nextValidToken();
                gen = tokens.intValue();
                if (pos == 0 && gen == 65535) {
                    --start;
                    --end;
                }
                tokens.seek(back);
            }
            if (xref.length < end) { // fix incorrect size
                int xref2[] = new int[end];
                System.arraycopy(xref, 0, xref2, 0, xref.length);
                xref = xref2;
            }
            for (int k = start; k < end; ++k) {
                tokens.nextValidToken();
                pos = tokens.intValue();
                tokens.nextValidToken();
                gen = tokens.intValue();
                tokens.nextValidToken();
                if (tokens.getStringValue().equals("n")) {
                    if (xref[k] == 0) {
                        if (pos == 0)
                            tokens.throwError("File position 0 cross-reference entry in this xref subsection");
                        xref[k] = pos;
                    }
                }
                else if (tokens.getStringValue().equals("f")) {
                    if (xref[k] == 0)
                        xref[k] = -1;
                }
                else
                    tokens.throwError("Invalid cross-reference entry in this xref subsection");
            }
        }
    }
    
    protected void rebuildXref() throws IOException {
        tokens.seek(0);
        int xr[][] = new int[1024][];
        int top = 0;
        trailer = null;
        byte line[] = new byte[64];
        for (;;) {
            int pos = tokens.getFilePointer();
            if (!tokens.readLineSegment(line))
                break;
            if (line[0] == 't') {
                if (!PdfEncodings.convertToString(line, null).startsWith("trailer"))
                    continue;
                pos = tokens.getFilePointer();
                try {
                    PdfDictionary dic = (PdfDictionary)readPRObject();
                    if (dic.get(PdfName.ROOT) != null)
                        trailer = dic;
                    else
                        tokens.seek(pos);
                }
                catch (Exception e) {
                    tokens.seek(pos);
                }
            }
            else if (line[0] >= '0' && line[0] <= '9') {
                int obj[] = PRTokeniser.checkObjectStart(line);
                if (obj == null)
                    continue;
                int num = obj[0];
                int gen = obj[1];
                if (num >= xr.length) {
                    int newLength = num * 2;
                    int xr2[][] = new int[newLength][];
                    System.arraycopy(xr, 0, xr2, 0, top);
                    xr = xr2;
                }
                if (num >= top)
                    top = num + 1;
                if (xr[num] == null || gen >= xr[num][1]) {
                    obj[0] = pos;
                    xr[num] = obj;
                }
            }
        }
        if (trailer == null)
            throw new IOException("trailer not found.");
        xref = new int[top];
        for (int k = 0; k < top; ++k) {
            int obj[] = xr[k];
            if (obj != null)
                xref[k] = obj[0];
        }
    }
    
    protected PdfDictionary readDictionary() throws IOException {
        PdfDictionary dic = new PdfDictionary();
        while (true) {
            tokens.nextValidToken();
            if (tokens.getTokenType() == PRTokeniser.TK_END_DIC)
                break;
            if (tokens.getTokenType() != PRTokeniser.TK_NAME)
                tokens.throwError("Dictionary key is not a name.");
            PdfName name = new PdfName(tokens.getStringValue());
            PdfObject obj = readPRObject();
            int type = obj.type();
            if (-type == PRTokeniser.TK_END_DIC)
                tokens.throwError("Unexpected '>>'");
            if (-type == PRTokeniser.TK_END_ARRAY)
                tokens.throwError("Unexpected ']'");
            dic.put(name, obj);
        }
        return dic;
    }
    
    protected PdfArray readArray() throws IOException {
        PdfArray array = new PdfArray();
        while (true) {
            PdfObject obj = readPRObject();
            int type = obj.type();
            if (-type == PRTokeniser.TK_END_ARRAY)
                break;
            if (-type == PRTokeniser.TK_END_DIC)
                tokens.throwError("Unexpected '>>'");
            array.add(obj);
        }
        return array;
    }
    
    protected PdfObject readPRObject() throws IOException {
        tokens.nextValidToken();
        int type = tokens.getTokenType();
        switch (type) {
            case PRTokeniser.TK_START_DIC: {
                PdfDictionary dic = readDictionary();
                int pos = tokens.getFilePointer();
                // be careful in the trailer. May not be a "next" token.
                if (tokens.nextToken() && tokens.getStringValue().equals("stream")) {
                    int ch = tokens.read();
                    if (ch == '\r')
                        ch = tokens.read();
                    if (ch != '\n')
                        tokens.backOnePosition(ch);
                    PRStream stream = new PRStream(this, tokens.getFilePointer());
                    stream.putAll(dic);
                    stream.setObjNum(objNum, objGen);
                    return stream;
                }
                else {
                    tokens.seek(pos);
                    return dic;
                }
            }
            case PRTokeniser.TK_START_ARRAY:
                return readArray();
            case PRTokeniser.TK_NUMBER:
                return new PdfNumber(tokens.getStringValue());
            case PRTokeniser.TK_STRING:
                PdfString str = new PdfString(tokens.getStringValue(), null).setWritingMode(tokens.isHexString());
                str.setObjNum(objNum, objGen);
                strings.add(str);
                return str;
            case PRTokeniser.TK_NAME:
                return new PdfName(tokens.getStringValue());
            case PRTokeniser.TK_REF:
                int num = tokens.getReference();
                PRIndirectReference ref = new PRIndirectReference(this, num, tokens.getGeneration());
                if (visited != null && !visited[num]) {
                    visited[num] = true;
                    newHits.put(num, 1);
                }
                return ref;
            default:
                return new PdfLiteral(-type, tokens.getStringValue());
        }
    }
    
    /** Decodes a stream that has the FlateDecode filter.
     * @param in the input data
     * @return the decoded data
     */    
    public static byte[] FlateDecode(byte in[]) {
        byte b[] = FlateDecode(in, true);
        if (b == null)
            return FlateDecode(in, false);
        return b;
    }
    
    /** A helper to FlateDecode.
     * @param in the input data
     * @param strict <CODE>true</CODE> to read a correct stream. <CODE>false</CODE>
     * to try to read a corrupted stream
     * @return the decoded data
     */    
    public static byte[] FlateDecode(byte in[], boolean strict) {
        ByteArrayInputStream stream = new ByteArrayInputStream(in);
        InflaterInputStream zip = new InflaterInputStream(stream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte b[] = new byte[strict ? 4092 : 1];
        try {
            int n;
            while ((n = zip.read(b)) >= 0) {
                out.write(b, 0, n);
            }
            zip.close();
            out.close();
            return out.toByteArray();
        }
        catch (Exception e) {
            if (strict)
                return null;
            return out.toByteArray();
        }
    }
    
    /** Decodes a stream that has the ASCIIHexDecode filter.
     * @param in the input data
     * @return the decoded data
     */    
    public static byte[] ASCIIHexDecode(byte in[]) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean first = true;
        int n1 = 0;
        for (int k = 0; k < in.length; ++k) {
            int ch = in[k] & 0xff;
            if (ch == '>')
                break;
            if (PRTokeniser.isWhitespace(ch))
                continue;
            int n = PRTokeniser.getHex(ch);
            if (n == -1)
                throw new RuntimeException("Illegal character in ASCIIHexDecode.");
            if (first)
                n1 = n;
            else
                out.write((byte)((n1 << 4) + n));
            first = !first;
        }
        if (!first)
            out.write((byte)(n1 << 4));
        return out.toByteArray();
    }
    
    /** Decodes a stream that has the ASCII85Decode filter.
     * @param in the input data
     * @return the decoded data
     */    
    public static byte[] ASCII85Decode(byte in[]) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int state = 0;
        int chn[] = new int[5];
        for (int k = 0; k < in.length; ++k) {
            int ch = in[k] & 0xff;
            if (ch == '~')
                break;
            if (PRTokeniser.isWhitespace(ch))
                continue;
            if (ch == 'z' && state == 0) {
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                continue;
            }
            if (ch < '!' || ch > 'u')
                throw new RuntimeException("Illegal character in ASCII85Decode.");
            chn[state] = ch - '!';
            ++state;
            if (state == 5) {
                state = 0;
                int r = 0;
                for (int j = 0; j < 5; ++j)
                    r = r * 85 + chn[j];
                out.write((byte)(r >> 24));
                out.write((byte)(r >> 16));
                out.write((byte)(r >> 8));
                out.write((byte)r);
            }
        }
        int r = 0;
        if (state == 1)
            throw new RuntimeException("Illegal length in ASCII85Decode.");
        if (state == 2) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85;
            out.write((byte)(r >> 24));
        }
        else if (state == 3) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85  + chn[2] * 85 * 85;
            out.write((byte)(r >> 24));
            out.write((byte)(r >> 16));
        }
        else if (state == 4) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85  + chn[2] * 85 * 85  + chn[3] * 85 ;
            out.write((byte)(r >> 24));
            out.write((byte)(r >> 16));
            out.write((byte)(r >> 8));
        }
        return out.toByteArray();
    }
    
    /** Decodes a stream that has the LZWDecode filter.
     * @param in the input data
     * @return the decoded data
     */    
    public static byte[] LZWDecode(byte in[]) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LZWDecoder lzw = new LZWDecoder();
        lzw.decode(in, out);
        return out.toByteArray();
    }
    
    /** Checks if the document had errors and was rebuilt.
     * @return true if rebuilt.
     *
     */
    public boolean isRebuilt() {
        return this.rebuilt;
    }
    
    /** Gets the dictionary that represents a page.
     * @param pageNum the page number. 1 is the first
     * @return the page dictionary
     */    
    public PdfDictionary getPageN(int pageNum) {
        if (pageNum > pages.length) return null;
        return pages[pageNum - 1];
        
        
    }
    
    /** Gets the page reference to this page.
     * @param pageNum the page number. 1 is the first
     * @return the page reference
     */    
    public PRIndirectReference getPageOrigRef(int pageNum) {
        if (pageNum > pageRefs.length) return null;
        return pageRefs[pageNum - 1];
    }
    
    /** Gets the contents of the page.
     * @param pageNum the page number. 1 is the first
     * @param file the location of the PDF document
     * @throws IOException on error
     * @return the content
     */    
    public byte[] getPageContent(int pageNum, RandomAccessFileOrArray file) throws IOException{
        PdfDictionary page = getPageN(pageNum);
        if (page == null)
            return null;
        PdfObject contents = getPdfObject(page.get(PdfName.CONTENTS));
        if (contents == null)
            return new byte[0];
        ByteArrayOutputStream bout = null;
        if (contents.isStream()) {
            return getStreamBytes((PRStream)contents, file);
        }
        else if (contents.isArray()) {
            PdfArray array = (PdfArray)contents;
            ArrayList list = array.getArrayList();
            bout = new ByteArrayOutputStream();
            for (int k = 0; k < list.size(); ++k) {
                PdfObject item = getPdfObject((PdfObject)list.get(k));
                if (item == null || !item.isStream()) //here's the ERROR
                    continue;
                byte[] b = getStreamBytes((PRStream)item, file);
                bout.write(b);
                if (k != list.size() - 1)
                    bout.write('\n');
            }
            return bout.toByteArray();
        }
        else
            return new byte[0];
    }
    
    protected void killXref(PdfObject obj) {
        if (obj == null)
            return;
        if ((obj instanceof PdfIndirectReference) && !obj.isIndirect())
            return;
        switch (obj.type()) {
            case PdfObject.INDIRECT: {
                int xr = ((PRIndirectReference)obj).getNumber();
                obj = xrefObj[xr];
                xrefObj[xr] = null;
                freeXref = xr;
                killXref(obj);
                break;
            }
            case PdfObject.ARRAY: {
                ArrayList t = ((PdfArray)obj).getArrayList();
                for (int i = 0; i < t.size(); ++i)
                    killXref((PdfObject)t.get(i));
                break;
            }
            case PdfObject.STREAM:
            case PdfObject.DICTIONARY: {
                PdfDictionary dic = (PdfDictionary)obj;
                for (Iterator i = dic.getKeys().iterator(); i.hasNext();){
                    killXref(dic.get((PdfName)i.next()));
                }
                break;
            }
        }
    }
    
    /** Sets the contents of the page.
     * @param content the new page content
     * @param pageNum the page number. 1 is the first
     * @throws IOException on error
     */    
    public void setPageContent(int pageNum, byte content[]) throws IOException{
        PdfDictionary page = getPageN(pageNum);
        if (page == null)
            return;
        PdfObject contents = page.get(PdfName.CONTENTS);
        freeXref = -1;
        killXref(contents);
        if (freeXref == -1) {
            for (int k = xrefObj.length - 1; k > 0; --k) {
                if (xrefObj[k] == null) {
                    freeXref = k;
                    break;
                }
            }
            if (freeXref == -1) {
                PdfObject temp[] = new PdfObject[xrefObj.length + 10];
                System.arraycopy(xrefObj, 0, temp, 0, xrefObj.length);
                freeXref = xrefObj.length;
                xrefObj = temp;
            }
        }
        page.put(PdfName.CONTENTS, new PRIndirectReference(this, freeXref));
        xrefObj[freeXref] = new PRStream(this, content);
    }
    
    /** Get the content from a stream.
     * @param stream the stream
     * @param file the location where the stream is
     * @throws IOException on error
     * @return the stream content
     */    
    public static byte[] getStreamBytes(PRStream stream, RandomAccessFileOrArray file) throws IOException {
        PdfReader reader = stream.getReader();
        PdfObject filter = PdfReader.getPdfObject(stream.get(PdfName.FILTER));
        byte b[];
        if (stream.getOffset() < 0)
            b = stream.getBytes();
        else {
            b = new byte[stream.getLength()];
            file.seek(stream.getOffset());
            file.readFully(b);
            PdfEncryption decrypt = reader.getDecrypt();
            if (decrypt != null) {
                decrypt.setHashKey(stream.getObjNum(), stream.getObjGen());
                decrypt.prepareKey();
                decrypt.encryptRC4(b);
            }
        }
        ArrayList filters = new ArrayList();
        if (filter != null) {
            if (filter.isName()) {
                filters.add(filter);
            }
            else if (filter.isArray()) {
                filters = ((PdfArray)filter).getArrayList();
            }
        }
        String name;
        for (int j = 0; j < filters.size(); ++j) {
            name = ((PdfName)PdfReader.getPdfObject((PdfObject)filters.get(j))).toString();
            if (name.equals("/FlateDecode") || name.equals("/Fl"))
                b = PdfReader.FlateDecode(b);
            else if (name.equals("/ASCIIHexDecode") || name.equals("/AHx"))
                b = PdfReader.ASCIIHexDecode(b);
            else if (name.equals("/ASCII85Decode") || name.equals("/A85"))
                b = PdfReader.ASCII85Decode(b);
            else if (name.equals("/LZWDecode"))
                b = PdfReader.LZWDecode(b);
            else
                throw new IOException("The filter " + name + " is not supported.");
        }
        return b;
    }
    
    /** Eliminates shared streams if they exist. */    
    public void eliminateSharedStreams() {
        if (!sharedStreams)
            return;
        sharedStreams = false;
        if (pages.length == 1)
            return;
        ArrayList newRefs = new ArrayList();
        ArrayList newStreams = new ArrayList();
        IntHashtable visited = new IntHashtable();
        for (int k = 0; k < pages.length; ++k) {
            PdfDictionary page = pages[k];
            if (page == null)
                continue;
            PdfObject contents = getPdfObject(page.get(PdfName.CONTENTS));
            if (contents == null)
                continue;
            if (contents.isStream()) {
                PRIndirectReference ref = (PRIndirectReference)page.get(PdfName.CONTENTS);
                if (visited.containsKey(ref.getNumber())) {
                    // need to duplicate
                    newRefs.add(ref);
                    newStreams.add(new PRStream((PRStream)contents, null));
                }
                else
                    visited.put(ref.getNumber(), 1);
            }
            else {
                PdfArray array = (PdfArray)contents;
                ArrayList list = array.getArrayList();
                for (int j = 0; j < list.size(); ++j) {
                    PRIndirectReference ref = (PRIndirectReference)list.get(j);
                    if (visited.containsKey(ref.getNumber())) {
                        // need to duplicate
                        newRefs.add(ref);
                        newStreams.add(new PRStream((PRStream)getPdfObject(ref), null));
                    }
                    else
                        visited.put(ref.getNumber(), 1);
                }
            }
        }
        if (newStreams.size() == 0)
            return;
        int start = 1;
        for (int pass = 0; pass < 2; ++pass) {
            for (int k = start; k < xrefObj.length; ++k) {
                if (xrefObj[k] == null) {
                    int p = newStreams.size() - 1;
                    xrefObj[k] = (PRStream)newStreams.get(p);
                    PRIndirectReference ref = (PRIndirectReference)newRefs.get(p);
                    ref.setNumber(k, 0);
                    if (p == 0)
                        return;
                    newStreams.remove(p);
                }
            }
            start = xrefObj.length;
            PdfObject nxo[] = new PdfObject[xrefObj.length + newStreams.size()];
            System.arraycopy(xrefObj, 0, nxo, 0, xrefObj.length);
            xrefObj = nxo;
        }
        
    }
    
    /** Checks if the document was changed.
     * @return <CODE>true</CODE> if the document was changed,
     * <CODE>false</CODE> otherwise
     */    
    public boolean isTampered() {
        return tampered;
    }
    
    public void setTampered(boolean tampered) {
        this.tampered = tampered;
    }
    
    /** Gets the XML metadata.
     * @throws IOException on error
     * @return the XML metadata
     */
    public byte[] getMetadata() throws IOException {
        PdfObject obj = getPdfObject(catalog.get(PdfName.METADATA));
        if (!(obj instanceof PRStream))
            return null;
        RandomAccessFileOrArray rf = getSafeFile();
        byte b[] = null;
        try {
            rf.reOpen();
            b = getStreamBytes((PRStream)obj, rf);
        }
        finally {
            try {
                rf.close();
            }
            catch (Exception e) {
                // empty on purpose
            }
        }
        return b;
    }
    
    public int getLastXref() {
        return lastXref;
    }
    
    public int getXrefSize() {
        return xrefObj.length;
    }
    
    public int getEofPos() {
        return eofPos;
    }
    
    public char getPdfVersion() {
        return pdfVersion;
    }
    
    public boolean isEncrypted() {
        return encrypted;
    }
    
    public int getPermissions() {
        return pValue;
    }
    
    public boolean is128Key() {
        return rValue == 3;
    }
    
    public PdfDictionary getTrailer() {
        return trailer;
    }
    
    PdfEncryption getDecrypt() {
        return decrypt;
    }
    
    static boolean equalsn(byte a1[], byte a2[]) {
        int length = a2.length;
        for (int k = 0; k < length; ++k) {
            if (a1[k] != a2[k])
                return false;
        }
        return true;
    }
    
    static boolean existsName(PdfDictionary dic, PdfName key, PdfName value) {
        PdfObject type = getPdfObject(dic.get(key));
        if (type == null || !type.isName())
            return false;
        PdfName name = (PdfName)type;
        return name.equals(value);
    }
    
    static String getFontName(PdfDictionary dic) {
        PdfObject type = getPdfObject(dic.get(PdfName.BASEFONT));
        if (type == null || !type.isName())
            return null;
        return PdfName.decodeName(type.toString());
    }
    
    static String getSubsetPrefix(PdfDictionary dic) {
        String s = getFontName(dic);
        if (s == null)
            return null;
        if (s.length() < 8 || s.charAt(6) != '+')
            return null;
        for (int k = 0; k < 6; ++k) {
            char c = s.charAt(k);
            if (c < 'A' || c > 'Z')
                return null;
        }
        return s;
    }
    
    /** Finds all the font subsets and changes the prefixes to some
     * random values.
     * @return the number of font subsets altered
     */    
    public int shuffleSubsetNames() {
        int total = 0;
        for (int k = 1; k < xrefObj.length; ++k) {
            PdfObject obj = xrefObj[k];
            if (obj == null || !obj.isDictionary())
                continue;
            PdfDictionary dic = (PdfDictionary)obj;
            if (!existsName(dic, PdfName.TYPE, PdfName.FONT))
                continue;
            if (existsName(dic, PdfName.SUBTYPE, PdfName.TYPE1)
                || existsName(dic, PdfName.SUBTYPE, PdfName.MMTYPE1)
                || existsName(dic, PdfName.SUBTYPE, PdfName.TRUETYPE)) {
                String s = getSubsetPrefix(dic);
                if (s == null)
                    continue;
                String ns = BaseFont.createSubsetPrefix() + s.substring(7);
                PdfName newName = new PdfName(ns);
                dic.put(PdfName.BASEFONT, newName);
                ++total;
                PdfDictionary fd = (PdfDictionary)getPdfObject(dic.get(PdfName.FONTDESCRIPTOR));
                if (fd == null)
                    continue;
                fd.put(PdfName.FONTNAME, newName);
            }
            else if (existsName(dic, PdfName.SUBTYPE, PdfName.TYPE0)) {
                String s = getSubsetPrefix(dic);
                PdfArray arr = (PdfArray)getPdfObject(dic.get(PdfName.DESCENDANTFONTS));
                if (arr == null)
                    continue;
                ArrayList list = arr.getArrayList();
                if (list.size() == 0)
                    continue;
                PdfDictionary desc = (PdfDictionary)getPdfObject((PdfObject)list.get(0));
                String sde = getSubsetPrefix(desc);
                if (sde == null)
                    continue;
                String ns = BaseFont.createSubsetPrefix();
                if (s != null)
                    dic.put(PdfName.BASEFONT, new PdfName(ns + s.substring(7)));
                PdfName newName = new PdfName(ns + sde.substring(7));
                desc.put(PdfName.BASEFONT, newName);
                ++total;
                PdfDictionary fd = (PdfDictionary)getPdfObject(desc.get(PdfName.FONTDESCRIPTOR));
                if (fd == null)
                    continue;
                fd.put(PdfName.FONTNAME, newName);
            }
        }
        return total;
    }
    
    /** Finds all the fonts not subset but embedded and marks them as subset.
     * @return the number of fonts altered
     */    
    public int createFakeFontSubsets() {
        int total = 0;
        for (int k = 1; k < xrefObj.length; ++k) {
            PdfObject obj = xrefObj[k];
            if (obj == null || !obj.isDictionary())
                continue;
            PdfDictionary dic = (PdfDictionary)obj;
            if (!existsName(dic, PdfName.TYPE, PdfName.FONT))
                continue;
            if (existsName(dic, PdfName.SUBTYPE, PdfName.TYPE1)
                || existsName(dic, PdfName.SUBTYPE, PdfName.MMTYPE1)
                || existsName(dic, PdfName.SUBTYPE, PdfName.TRUETYPE)) {
                String s = getSubsetPrefix(dic);
                if (s != null)
                    continue;
                s = getFontName(dic);
                if (s == null)
                    continue;
                String ns = BaseFont.createSubsetPrefix() + s;
                PdfDictionary fd = (PdfDictionary)getPdfObject(dic.get(PdfName.FONTDESCRIPTOR));
                if (fd == null)
                    continue;
                if (fd.get(PdfName.FONTFILE) == null && fd.get(PdfName.FONTFILE2) == null
                    && fd.get(PdfName.FONTFILE3) == null)
                    continue;
                PdfName newName = new PdfName(ns);
                dic.put(PdfName.BASEFONT, newName);
                fd.put(PdfName.FONTNAME, newName);
                ++total;
            }
        }
        return total;
    }
    
    private static PdfArray getNameArray(PdfObject obj) {
        if (obj == null)
            return null;
        obj = getPdfObject(obj);
        if (obj.isArray())
            return (PdfArray)obj;
        else if (obj.isDictionary()) {
            PdfObject arr2 = getPdfObject(((PdfDictionary)obj).get(PdfName.D));
            if (arr2 != null && arr2.isArray())
                return (PdfArray)arr2;
        }
        return null;
    }

    public HashMap getNamedDestination() {
        HashMap names = getNamedDestinationFromNames();
        names.putAll(getNamedDestinationFromStrings());
        return names;
    }
    
    public HashMap getNamedDestinationFromNames() {
        HashMap names = new HashMap();
        if (catalog.get(PdfName.DESTS) != null) {
            PdfDictionary dic = (PdfDictionary)getPdfObject(catalog.get(PdfName.DESTS));
            Set keys = dic.getKeys();
            for (Iterator it = keys.iterator(); it.hasNext();) {
                PdfName key = (PdfName)it.next();
                String name = PdfName.decodeName(key.toString());
                PdfArray arr = getNameArray(dic.get(key));
                if (arr != null)
                    names.put(name, arr);
            }
        }
        return names;
    }

    public HashMap getNamedDestinationFromStrings() {
        if (catalog.get(PdfName.NAMES) != null) {
            PdfDictionary dic = (PdfDictionary)getPdfObject(catalog.get(PdfName.NAMES));
            dic = (PdfDictionary)getPdfObject(dic.get(PdfName.DESTS));
            if (dic != null) {
                HashMap names = PdfNameTree.readTree(dic);
                for (Iterator it = names.entrySet().iterator(); it.hasNext();) {
                    Map.Entry entry = (Map.Entry)it.next();
                    PdfArray arr = getNameArray((PdfObject)entry.getValue());
                    if (arr != null)
                        entry.setValue(arr);
                    else
                        it.remove();
                }
                return names;
            }
        }
        return new HashMap();
    }
    
    private static void replaceNamedDestination(PdfObject obj, HashMap names) {
        if (obj != null && obj.isDictionary()) {
            PdfObject ob2 = getPdfObject(((PdfDictionary)obj).get(PdfName.DEST));
            String name = null;
            if (ob2 != null) {
                if (ob2.isName())
                    name = PdfName.decodeName(ob2.toString());
                else if (ob2.isString())
                    name = ob2.toString();
                PdfArray dest = (PdfArray)names.get(name);
                if (dest != null)
                    ((PdfDictionary)obj).put(PdfName.DEST, dest);
            }
            else if ((ob2 = getPdfObject(((PdfDictionary)obj).get(PdfName.A))) != null) {
                PdfDictionary dic = (PdfDictionary)ob2;
                PdfName type = (PdfName)getPdfObject(dic.get(PdfName.S));
                if (PdfName.GOTO.equals(type)) {
                    ob2 = getPdfObject(dic.get(PdfName.D));
                    if (ob2.isName())
                        name = PdfName.decodeName(ob2.toString());
                    else if (ob2.isString())
                        name = ob2.toString();
                    PdfArray dest = (PdfArray)names.get(name);
                    if (dest != null)
                        dic.put(PdfName.D, dest);
                }
            }
        }
    }
    
    /**
     * Removes all the fields from the document.
     */    
    public void removeFields() {
        for (int k = 0; k < pages.length; ++k) {
            PdfDictionary page = pages[k];
            PdfArray annots = (PdfArray)getPdfObject(page.get(PdfName.ANNOTS));
            if (annots == null)
                continue;
            ArrayList arr = annots.getArrayList();
            for (int j = 0; j < arr.size(); ++j) {
                PdfDictionary annot = (PdfDictionary)getPdfObject((PdfObject)arr.get(j));
                if (PdfName.WIDGET.equals(annot.get(PdfName.SUBTYPE)))
                    arr.remove(j--);
            }
            if (arr.isEmpty())
                page.remove(PdfName.ANNOTS);
        }
        catalog.remove(PdfName.ACROFORM);
    }
    
    /**
     * Removes all the annotations and fields from the document.
     */    
    public void removeAnnotations() {
        for (int k = 0; k < pages.length; ++k) {
            pages[k].remove(PdfName.ANNOTS);
        }
        catalog.remove(PdfName.ACROFORM);
    }
    
    private void iterateBookmarks(PdfDictionary outline, HashMap names) {
        while (outline != null) {
            replaceNamedDestination(outline, names);
            PdfDictionary first = (PdfDictionary)getPdfObject(outline.get(PdfName.FIRST));
            if (first != null) {
                iterateBookmarks(first, names);
            }
            outline = (PdfDictionary)getPdfObject(outline.get(PdfName.NEXT));
        }
    }
    
    /** Replaces all the local named links with the actual destinations. */    
    public void consolidateNamedDestinations() {
        if (consolidateNamedDestinations)
            return;
        consolidateNamedDestinations = true;
        HashMap names = getNamedDestination();
        if (names.size() == 0)
            return;
        for (int k = 0; k < pages.length; ++k) {
            PdfArray arr = (PdfArray)getPdfObject(pages[k].get(PdfName.ANNOTS));
            if (arr == null)
                continue;
            ArrayList list = arr.getArrayList();
            for (int an = 0; an < list.size(); ++an) {
                PdfObject obj = getPdfObject((PdfObject)list.get(an));
                replaceNamedDestination(obj, names);
            }
        }
        PdfDictionary outlines = (PdfDictionary)getPdfObject(catalog.get(PdfName.OUTLINES));
        if (outlines == null)
            return;
        iterateBookmarks((PdfDictionary)getPdfObject(outlines.get(PdfName.FIRST)), names);
    }
    
    protected static PdfDictionary duplicatePdfDictionary(PdfDictionary original, PdfDictionary copy, PdfReader newReader) {
        if (copy == null)
            copy = new PdfDictionary();
        for (Iterator it = original.getKeys().iterator(); it.hasNext();) {
            PdfName key = (PdfName)it.next();
            copy.put(key, duplicatePdfObject(original.get(key), newReader));
        }
        return copy;
    }
    
    protected static PdfObject duplicatePdfObject(PdfObject original, PdfReader newReader) {
        if (original == null)
            return null;
        switch (original.type()) {
            case PdfObject.DICTIONARY: {
                return duplicatePdfDictionary((PdfDictionary)original, null, newReader);
            }
            case PdfObject.STREAM: {
                PRStream org = (PRStream)original;
                PRStream stream = new PRStream(org, null, newReader);
                duplicatePdfDictionary(org, stream, newReader);
                return stream;
            }
            case PdfObject.ARRAY: {
                ArrayList list = ((PdfArray)original).getArrayList();
                PdfArray arr = new PdfArray();
                for (Iterator it = list.iterator(); it.hasNext();) {
                    arr.add(duplicatePdfObject((PdfObject)it.next(), newReader));
                }
                return arr;
            }
            case PdfObject.INDIRECT: {
                PRIndirectReference org = (PRIndirectReference)original;
                return new PRIndirectReference(newReader, org.getNumber(), org.getGeneration());
            }
            default:
                return original;
        }
    }
    
    protected void removeUnusedNode(PdfObject obj, boolean hits[]) {
        if (obj == null)
            return;
        switch (obj.type()) {
            case PdfObject.DICTIONARY: 
            case PdfObject.STREAM: {
                PdfDictionary dic = (PdfDictionary)obj;
                for (Iterator it = dic.getKeys().iterator(); it.hasNext();) {
                    PdfName key = (PdfName)it.next();
                    removeUnusedNode(dic.get(key), hits);
                }
                break;
            }
            case PdfObject.ARRAY: {
                ArrayList list = ((PdfArray)obj).getArrayList();
                PdfArray arr = new PdfArray();
                for (Iterator it = list.iterator(); it.hasNext();) {
                    removeUnusedNode((PdfObject)it.next(), hits);
                }
                break;
            }
            case PdfObject.INDIRECT: {
                PRIndirectReference ref = (PRIndirectReference)obj;
                int num = ref.getNumber();
                if (!hits[num]) {
                    hits[num] = true;
                    removeUnusedNode(getPdfObject(ref), hits);
                }
            }
        }
    }
    
    /** Removes all the unreachable objects.
     * @return the number of indirect objects removed
     */    
    public int removeUnusedObjects() {
        boolean hits[] = new boolean[xrefObj.length];
        removeUnusedNode(trailer, hits);
        int total = 0;
        for (int k = 1; k < hits.length; ++k) {
            if (!hits[k] && xrefObj[k] != null) {
                xrefObj[k] = null;
                ++total;
            }
        }
        return total;
    }
    
    /** Gets a read-only version of <CODE>AcroFields</CODE>.
     * @return a read-only version of <CODE>AcroFields</CODE>
     */    
    public AcroFields getAcroFields() {
        return new AcroFields(this, null);
    }
}