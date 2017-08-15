/*
 * @(#)BinaryPListParser.java
 *
 * Copyright (c) 2005-2013 Werner Randelshofer, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package nl.pvanassen.bplist.converter;

import java.io.*;
import java.util.*;

import javax.xml.datatype.*;

import nl.pvanassen.bplist.ext.base64.Base64;
import nl.pvanassen.bplist.ext.nanoxml.XMLElement;
import nl.pvanassen.bplist.parser.*;

/**
 * Reads a binary PList file and returns it as a NanoXML XMLElement.
 * <p>
 * The NanoXML XMLElement returned by this reader is equivalent to the XMLElement returned, if a PList file in XML format is parsed with NanoXML.
 * <p>
 * Description about property list taken from <a href= "http://developer.apple.com/documentation/Cocoa/Conceptual/PropertyLists/index.html#//apple_ref/doc/uid/10000048i" > Apple's
 * online documentation</a>:
 * <p>
 * "A property list is a data representation used by Mac OS X Cocoa and Core Foundation as a convenient way to store, organize, and access standard object types. Frequently called
 * a plist, a property list is an object of one of several certain Cocoa or Core Foundation types, including arrays, dictionaries, strings, binary data, numbers, dates, and Boolean
 * values. If the object is a container (an array or dictionary), all objects contained within it must also be supported property list objects. (Arrays and dictionaries can contain
 * objects not supported by the architecture, but are then not property lists, and cannot be saved and restored with the various property list methods.)" Description of the binary
 * plist format derived from http://opensource.apple.com/source/CF/CF-635/CFBinaryPList.c EBNF description of the file format:
 * 
 * <pre>
 * bplist ::= header objectTable
 * offsetTable trailer
 * 
 * header ::= magicNumber fileFormatVersion magicNumber ::= "bplist"
 * fileFormatVersion ::= "00"
 * 
 * objectTable ::= { null | bool | fill | number | date | data | string |
 * uid | array | dict }
 * 
 * null ::= 0b0000 0b0000
 * 
 * bool ::= false | true false ::= 0b0000 0b1000 true ::= 0b0000 0b1001
 * 
 * fill ::= 0b0000 0b1111 // fill byte
 * 
 * number ::= int | real int ::= 0b0001 0bnnnn byte*(2^nnnn) // 2^nnnn
 * big-endian bytes real ::= 0b0010 0bnnnn byte*(2^nnnn) // 2^nnnn
 * big-endian bytes
 * 
 * unknown::= 0b0011 0b0000 byte*8 // 8 byte float big-endian bytes ?
 * 
 * date ::= 0b0011 0b0011 byte*8 // 8 byte float big-endian bytes
 * 
 * data ::= 0b0100 0bnnnn [int] byte* // nnnn is number of bytes // unless
 * 0b1111 then a int // variable-sized object follows // to indicate the
 * number of bytes
 * 
 * string ::= asciiString | unicodeString asciiString ::= 0b0101 0bnnnn
 * [int] byte* unicodeString ::= 0b0110 0bnnnn [int] short* // nnnn is
 * number of bytes // unless 0b1111 then a int // variable-sized object
 * follows // to indicate the number of bytes
 * 
 * uid ::= 0b1000 0bnnnn byte* // nnnn+1 is # of bytes
 * 
 * array ::= 0b1010 0bnnnn [int] objref* // // nnnn is number of objref //
 * unless 0b1111 then a int // variable-sized object follows // to indicate
 * the number of objref
 * 
 * dict ::= 0b1010 0bnnnn [int] keyref* objref* // nnnn is number of keyref
 * and // objref pairs // unless 0b1111 then a int // variable-sized object
 * follows // to indicate the number of pairs
 * 
 * objref = byte | short // if refCount // is less than 256 then objref is
 * // an unsigned byte, otherwise it // is an unsigned big-endian short
 * 
 * keyref = byte | short // if refCount // is less than 256 then objref is
 * // an unsigned byte, otherwise it // is an unsigned big-endian short
 * 
 * unused ::= 0b0111 0bxxxx | 0b1001 0bxxxx | 0b1011 0bxxxx | 0b1100 0bxxxx
 * | 0b1110 0bxxxx | 0b1111 0bxxxx
 * 
 * 
 * offsetTable ::= { int } // List of ints, byte size of which // is given
 * in trailer // These are the byte offsets into // the file. // The number
 * of the ffsets is given // in the trailer.
 * 
 * trailer ::= refCount offsetCount objectCount topLevelOffset
 * 
 * refCount ::= byte*8 // unsigned big-endian long offsetCount ::= byte*8 //
 * unsigned big-endian long objectCount ::= byte*8 // unsigned big-endian
 * long topLevelOffset ::= byte*8 // unsigned big-endian long
 * </pre>
 * 
 * *
 * 
 * @see nl.pvanassen.bplist.ext.nanoxml.XMLElement
 * @author Werner Randelshofer
 * @version $Id$
 */
public class ConvertToXml {
    /** Factory for generating XML data types. */
    private final static DatatypeFactory DATATYPE_FACTORY;
    private final ElementParser parser = new ElementParser();
    
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException("Can't create XML datatype factory.", ex);
        }
    }
    
    /**
     * Parses a binary PList file and turns it into a XMLElement. The XMLElement
     * is equivalent with a XML PList file parsed using NanoXML.
     * 
     * @param file bplist to parse
     * @return Returns the parsed XMLElement.
     * @throws IOException If the file is not found
     */
    public XMLElement convertToXml(File file) throws IOException {
        // Convert the object table to XML and return it
        XMLElement root = new XMLElement(new HashMap<String, char[]>(), false, false);
        root.setName("plist");
        root.setAttribute("version", "1.0");
        convertObjectTableToXML(root, parser.parseObjectTable(file).get(0));
        return root;
    }
    
    /**
     * Parses a binary PList file and turns it into a XMLElement. The XMLElement
     * is equivalent with a XML PList file parsed using NanoXML.
     * 
     * @param list Parsed tree
     * @return Returns the parsed XMLElement.
     * @throws IOException If the file is not found
     */
    public XMLElement convertToXml(List<BPListElement<?>> list) throws IOException {
        // Convert the object table to XML and return it
        XMLElement root = new XMLElement(new HashMap<String, char[]>(), false, false);
        root.setName("plist");
        root.setAttribute("version", "1.0");
        convertObjectTableToXML(root, list.get(0));
        return root;
    }
    
    /**
     * Converts the object table in the binary PList into an XMLElement.
     */
    private void convertObjectTableToXML(XMLElement parent, BPListElement<?> object) {
        XMLElement elem = parent.createAnotherElement();
        if (object.getType() == BPListType.SHORT_DICT || object.getType() == BPListType.BYTE_DICT) {
            Map<String,BPListElement<?>> dictionary = (Map<String,BPListElement<?>>)object.getValue();
            elem.setName("dict");
            for (Map.Entry<String,BPListElement<?>> entry : dictionary.entrySet()) {
                XMLElement key = parent.createAnotherElement();
                key.setName("key");
                key.setContent(entry.getKey());
                elem.addChild(key);
                convertObjectTableToXML(elem, entry.getValue());
            }
        } else if (object.getType() == BPListType.SHORT_ARRAY || object.getType() == BPListType.BYTE_ARRAY) {
            List<BPListElement<?>> elements = (List<BPListElement<?>>)object.getValue();
            elem.setName("array");
            for (BPListElement<?> element : elements) {
                convertObjectTableToXML(elem, element);
            }
        } else if (object.getType() == BPListType.ASCII_STRING || object.getType() == BPListType.UNICODE_STRING) {
            elem.setName("string");
            elem.setContent(object.getValue().toString());
        } else if (object.getType() == BPListType.LONG) {
            elem.setName("integer");
            elem.setContent(object.getValue().toString());
        } else if (object.getType() == BPListType.FLOAT) {
            elem.setName("real");
            elem.setContent(object.getValue().toString());
        } else if (object.getType() == BPListType.DOUBLE) {
            elem.setName("real");
            elem.setContent(object.getValue().toString());
        } else if (object.getType() == BPListType.BOOLEAN) {
            elem.setName("boolean");
            elem.setContent(object.getValue().toString());
        } else if (object.getType() == BPListType.DATA) {
            elem.setName("data");
            BPListElement<byte[]> data = (BPListElement<byte[]>)object; 
            elem.setContent(Base64.encodeBytes(data.getValue(), Base64.DONT_BREAK_LINES));
        } else if (object.getType() == BPListType.DATE) {
            elem.setName("date");
            BPListElement<Date> date = (BPListElement<Date>)object; 
            elem.setContent(fromDate(date.getValue()).toXMLFormat() + "Z");
        } else if (object.getType() == BPListType.UID) {
            elem.setName("UID");
            elem.setContent(object.getValue().toString());
        } else {
            elem.setName("unsupported");
            elem.setContent(object.toString());
        }
        parent.addChild(elem);
    }


    private static XMLGregorianCalendar fromDate(Date date) {
        GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gc.setTime(date);
        XMLGregorianCalendar xmlgc = DATATYPE_FACTORY.newXMLGregorianCalendar(gc);
        xmlgc.setFractionalSecond(null);
        xmlgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
        return xmlgc;
    }
    // main function and helper functions that takes a bplist input and outputs a plist xml
    public static void main(String[] argv){
        if(argv.length != 2){
            System.out.println("Usage: java nl.pvanassen.bplist.converter.ConvertToXml in.bplist out.plist");
                return;
        }
        try{
            File input = new File(argv[0]);
            File output = new File(argv[1]);
            FileOutputStream outputStream = new FileOutputStream(output);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            PrintWriter writer = new PrintWriter(bufferedOutputStream);
            ByteArrayOutputStream bOS = new ByteArrayOutputStream();
            ConvertToXml converter = new ConvertToXml();
            PrintWriter sOS = new PrintWriter(bOS);
            dig(sOS, converter.convertToXml(input), 0);
            sOS.close();
            byte[] xmlByteArray = bOS.toByteArray();
            ByteArrayInputStream bIS = new ByteArrayInputStream(xmlByteArray);
            String xml = "";
            Scanner scan = new Scanner(bIS);
            while(scan.hasNextLine()){
                xml += scan.nextLine() + "\n";
            }
            scan.close();
            System.out.println(xml);
            writer.print(xml);
            writer.close();
        }catch(Exception ex){
            System.out.println("Welp something went wrong");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return;
    }
    private static void dig(PrintWriter out, XMLElement xml, int indent){
	if(xml == null){
		System.out.println("Something's wrong with the pblist input");
		return;
	}
        String name = xml.getName();
        // get all attribute
        indent(out, indent);
        out.print("<" + name);
        Iterator<String> attributeNames = xml.enumerateAttributeNames();
        while(attributeNames.hasNext()){
            String attributeName = attributeNames.next();
            out.print(" " + attributeName + "=\"" + xml.getStringAttribute(attributeName) + "\"");
        }
        out.print(">\n");
        Iterator<XMLElement> children = xml.iterateChildren();
        if(!children.hasNext()){
            indent(out, indent);
            out.print(xml.getContent() + "\n");
        }
        while(children.hasNext()){
            XMLElement child = children.next();
            dig(out, child, indent + 1);
        }
        indent(out, indent);
        out.print("</" + name + ">\n");
    }
    private static void indent(PrintWriter out, int indent){
        for(int i = 0;i < indent;i++){
            out.print("\t");
        }
    }
}
