/* Werner Randelshofer 2006-01-08
 * Replaced Java 1.1 collections by Java 1.2 collections.
 */
/* XMLElement.java
 *
 * $Revision: 1.4 $
 * $Date: 2002/03/24 10:27:59 $
 * $Name: RELEASE_2_2_1 $
 *
 * This file is part of NanoXML 2 Lite.
 * Copyright (C) 2000-2002 Marc De Scheemaecker, All Rights Reserved.
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software in
 *     a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *
 *  3. This notice may not be removed or altered from any source distribution.
 *****************************************************************************/

package nl.pvanassen.bplist.ext.nanoxml;

import java.io.*;
import java.util.*;

/**
 * XMLElement is a representation of an XML object. The object is able to parse
 * XML code.
 * <DL>
 * <DT><B>Parsing XML Data</B></DT>
 * <DD>You can parse XML data using the following code:
 * <UL>
 * <li><CODE>
 * XMLElement xml = new XMLElement();<BR>
 * FileReader reader = new FileReader("filename.xml");<BR>
 * xml.parseFromReader(reader);
 * </CODE></li>
 * </UL>
 * </DD>
 * </DL>
 * <DL>
 * <DT><B>Retrieving Attributes</B></DT>
 * <DD>You can enumerate the attributes of an element using the method {@link #enumerateAttributeNames() enumerateAttributeNames}. The attribute values can be retrieved using the
 * method {@link #getStringAttribute(java.lang.String) getStringAttribute}. The following example shows how to list the attributes of an element:
 * <UL>
 * <li><CODE>
 * XMLElement element = ...;<BR>
 * Iterator iter = element.getAttributeNames();<BR>
 * while (iter.hasNext()) {<BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;String key = (String) iter.next();<BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;String value = element.getStringAttribute(key);<BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;System.out.println(key + " = " + value);<BR>
 * }
 * </CODE></li>
 * </UL>
 * </DD>
 * </DL>
 * <DL>
 * <DT><B>Retrieving Child Elements</B></DT>
 * <DD>You can enumerate the children of an element using {@link #iterateChildren()
 * iterateChildren}. The number of child iterator can be retrieved using {@link #countChildren() countChildren}.</DD>
 * </DL>
 * <DL>
 * <DT><B>Elements Containing Character Data</B></DT>
 * <DD>If an iterator contains character data, like in the following example:
 * <UL>
 * <li><CODE>
 * &lt;title&gt;The Title&lt;/title&gt;
 * </CODE></li>
 * </UL>
 * you can retrieve that data using the method {@link #getContent() getContent}.</DD>
 * </DL>
 * <DL>
 * <DT><B>Subclassing XMLElement</B></DT>
 * <DD>When subclassing XMLElement, you need to override the method {@link #createAnotherElement() createAnotherElement} which has to return a new copy of the receiver.</DD>
 * </DL>
 *
 * @author Marc De Scheemaecker <A href="mailto:cyberelf@mac.com">cyberelf@mac.com</A>
 * @version 2005-06-18 Werner Randelshofer: Adapted for Java 2 Collections API. <br>
 *          $Name: RELEASE_2_2_1 $, $Revision: 1.4 $
 * @see XMLParseException
 */
public class XMLElement {

    /**
     * Serialization serial version ID.
     */
    static final long serialVersionUID = 6685035139346394777L;

    /**
     * Major version of NanoXML. Classes with the same major and minor version
     * are binary compatible. Classes with the same major version are source
     * compatible. If the major version is different, you may need to modify the
     * client source code.
     *
     * @see XMLElement#NANOXML_MINOR_VERSION
     */
    public static final int NANOXML_MAJOR_VERSION = 2;

    /**
     * Minor version of NanoXML. Classes with the same major and minor version
     * are binary compatible. Classes with the same major version are source
     * compatible. If the major version is different, you may need to modify the
     * client source code.
     *
     * @see XMLElement#NANOXML_MAJOR_VERSION
     */
    public static final int NANOXML_MINOR_VERSION = 2;

    /**
     * The attributes given to the element.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li>The field can be empty.
     * <li>The field is never <code>null</code>.
     * <li>The keySet().iterator and the values are strings.
     * </ul>
     * </dd>
     * </dl>
     */
    private Map<String, String> attributes;

    /**
     * Child iterator of the element.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li>The field can be empty.
     * <li>The field is never <code>null</code>.
     * <li>The iterator are instances of <code>XMLElement</code> or a subclass of <code>XMLElement</code>.
     * </ul>
     * </dd>
     * </dl>
     */
    private final List<XMLElement> children;

    /**
     * The name of the element.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li>The field is <code>null</code> iff the element is not initialized by either parse or setName.
     * <li>If the field is not <code>null</code>, it's not empty.
     * <li>If the field is not <code>null</code>, it contains a valid XML identifier.
     * </ul>
     * </dd>
     * </dl>
     */
    private String name;

    /**
     * The #PCDATA content of the object.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li>The field is <code>null</code> iff the element is not a #PCDATA element.
     * <li>The field can be any string, including the empty string.
     * </ul>
     * </dd>
     * </dl>
     */
    private String contents;

    /**
     * Conversion table for &amp;...; entities. The keySet().iterator are the
     * entity names without the &amp; and ; delimiters.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li>The field is never <code>null</code>.
     * <li>The field always contains the following associations: "lt"&nbsp;=&gt;&nbsp;"&lt;", "gt"&nbsp;=&gt;&nbsp;"&gt;", "quot"&nbsp;=&gt;&nbsp;"\"", "apos"&nbsp;=&gt;&nbsp;"'",
     * "amp"&nbsp;=&gt;&nbsp;"&amp;"
     * <li>The keySet().iterator are strings
     * <li>The values are char arrays
     * </ul>
     * </dd>
     * </dl>
     */
    private Map<String, char[]> entities;

    /**
     * The line number where the element starts.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li><code>lineNr &gt= 0</code>
     * </ul>
     * </dd>
     * </dl>
     */
    private int lineNr;

    /**
     * <code>true</code> if the case of the element and attribute names are case
     * insensitive.
     */
    private boolean ignoreCase;

    /**
     * <code>true</code> if the leading and trailing whitespace of #PCDATA
     * sections have to be ignored.
     */
    private boolean ignoreWhitespace;

    /**
     * Character read too much. This character provides push-back functionality
     * to the input reader without having to use a PushbackReader. If there is
     * no such character, this field is '\0'.
     */
    private char charReadTooMuch;

    /**
     * The reader provided by the caller of the parse method.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li>The field is not <code>null</code> while the parse method is running.
     * </ul>
     * </dd>
     * </dl>
     */
    private Reader reader;

    /**
     * The current line number in the source content.
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li>parserLineNr &gt; 0 while the parse method is running.
     * </ul>
     * </dd>
     * </dl>
     */
    private int parserLineNr;

    /**
     * Creates and initializes a new XML element. Calling the construction is
     * equivalent to:
     * <ul>
     * <li><code>new XMLElement(new HashMap(), false, true)
     * </code></li>
     * </ul>
     * <dl>
     * <dt><b>Postconditions:</b></dt>
     * <dd>
     * <ul>
     * <li>countChildren() =&gt; 0
     * <li>iterateChildren() =&gt; empty enumeration
     * <li>enumeratePropertyNames() =&gt; empty enumeration
     * <li>getChildren() =&gt; empty vector
     * <li>getContent() =&gt; ""
     * <li>getLineNr() =&gt; 0
     * <li>getName() =&gt; null
     * </ul>
     * </dd>
     * </dl>
     *
     * @see XMLElement#XMLElement(java.util.Map) XMLElement(HashMap)
     * @see XMLElement#XMLElement(boolean)
     * @see XMLElement#XMLElement(java.util.Map, boolean) XMLElement(HashMap,
     * boolean)
     */
    public XMLElement() {
        this(new HashMap<String, char[]>(), false, true, true);
    }

    /**
     * Creates and initializes a new XML element. Calling the construction is
     * equivalent to:
     * <ul>
     * <li><code>new XMLElement(entities, false, true)
     * </code></li>
     * </ul>
     *
     * @param entities The entity conversion table.
     *                 <dl>
     *                 <dt><b>Preconditions:</b></dt>
     *                 <dd>
     *                 <ul>
     *                 <li><code>entities != null</code>
     *                 </ul>
     *                 </dd>
     *                 </dl>
     *                 <dl>
     *                 <dt><b>Postconditions:</b></dt>
     *                 <dd>
     *                 <ul>
     *                 <li>countChildren() =&gt; 0</li>
     *                 <li>iterateChildren() =&gt; empty enumeration</li>
     *                 <li>enumeratePropertyNames() =&gt; empty enumeration</li>
     *                 <li>getChildren() =&gt; empty vector</li>
     *                 <li>getContent() =&gt; ""</li>
     *                 <li>getLineNr() =&gt; 0</li>
     *                 <li>getName() =&gt; null</li>
     *                 </ul>
     *                 </dd>
     *                 </dl>
     * @see XMLElement#XMLElement()
     * @see XMLElement#XMLElement(boolean)
     * @see XMLElement#XMLElement(java.util.Map, boolean) XMLElement(HashMap,
     * boolean)
     */
    public XMLElement(Map<String, char[]> entities) {
        this(entities, false, true, true);
    }

    /**
     * Creates and initializes a new XML element. Calling the construction is
     * equivalent to:
     * <ul>
     * <li><code>new XMLElement(new HashMap(), skipLeadingWhitespace, true)
     * </code></li>
     * </ul>
     *
     * @param skipLeadingWhitespace <code>true</code> if leading and trailing whitespace in PCDATA
     *                              content has to be removed.
     *                              <dl>
     *                              <dt><b>Postconditions:</b></dt>
     *                              <dd>
     *                              <ul>
     *                              <li>countChildren() =&gt; 0
     *                              <li>iterateChildren() =&gt; empty enumeration
     *                              <li>enumeratePropertyNames() =&gt; empty enumeration
     *                              <li>getChildren() =&gt; empty vector
     *                              <li>getContent() =&gt; ""
     *                              <li>getLineNr() =&gt; 0
     *                              <li>getName() =&gt; null
     *                              </ul>
     *                              </dd>
     *                              </dl>
     * @see XMLElement#XMLElement()
     * @see XMLElement#XMLElement(java.util.Map) XMLElement(HashMap)
     * @see XMLElement#XMLElement(java.util.Map, boolean) XMLElement(HashMap,
     * boolean)
     */
    public XMLElement(boolean skipLeadingWhitespace) {
        this(new HashMap<String, char[]>(), skipLeadingWhitespace, true, true);
    }

    /**
     * Creates and initializes a new XML element. Calling the construction is
     * equivalent to:
     * <ul>
     * <li><code>new XMLElement(entities, skipLeadingWhitespace, true)</code></li>
     * </ul>
     *
     * @param entities              The entity conversion table.
     * @param skipLeadingWhitespace <code>true</code> if leading and trailing whitespace in PCDATA
     *                              content has to be removed.
     *                              <dl>
     *                              <dt><b>Preconditions:</b></dt>
     *                              </dl>
     *                              <ul>
     *                              <li><code>entities != null</code>
     *                              </ul>
     *                              <dl>
     *                              <dt><b>Postconditions:</b></dt>
     *                              </dl>
     *                              <ul>
     *                              <li>countChildren() =&gt; 0
     *                              <li>iterateChildren() =&gt; empty enumeration
     *                              <li>enumeratePropertyNames() =&gt; empty enumeration
     *                              <li>getChildren() =&gt; empty vector
     *                              <li>getContent() =&gt; ""
     *                              <li>getLineNr() =&gt; 0
     *                              <li>getName() =&gt; null
     *                              </ul>
     * @see XMLElement#XMLElement()
     * @see XMLElement#XMLElement(boolean)
     * @see XMLElement#XMLElement(java.util.Map) XMLElement(HashMap)
     */
    public XMLElement(Map<String, char[]> entities, boolean skipLeadingWhitespace) {
        this(entities, skipLeadingWhitespace, true, true);
    }

    /**
     * Creates and initializes a new XML element.
     *
     * @param entities              The entity conversion table.
     * @param skipLeadingWhitespace <code>true</code> if leading and trailing whitespace in PCDATA
     *                              content has to be removed.
     * @param ignoreCase            <code>true</code> if the case of element and attribute names
     *                              have to be ignored.
     *                              <dl>
     *                              <dt><b>Preconditions:</b></dt>
     *                              </dl>
     *                              <ul>
     *                              <li><code>entities != null</code></li>
     *                              </ul>
     *                              <dl>
     *                              <dt><b>Postconditions:</b></dt>
     *                              </dl>
     *                              <ul>
     *                              <li>countChildren() =&gt; 0</li>
     *                              <li>iterateChildren() =&gt; empty enumeration</li>
     *                              <li>enumeratePropertyNames() =&gt; empty enumeration</li>
     *                              <li>getChildren() =&gt; empty vector</li>
     *                              <li>getContent() =&gt; ""</li>
     *                              <li>getLineNr() =&gt; 0</li>
     *                              <li>getName() =&gt; null</li>
     *                              </ul>
     * @see XMLElement#XMLElement()
     * @see XMLElement#XMLElement(boolean)
     * @see XMLElement#XMLElement(java.util.Map) XMLElement(HashMap)
     * @see XMLElement#XMLElement(java.util.Map, boolean) XMLElement(HashMap,
     * boolean)
     */
    public XMLElement(Map<String, char[]> entities, boolean skipLeadingWhitespace, boolean ignoreCase) {
        this(entities, skipLeadingWhitespace, true, ignoreCase);
    }

    /**
     * Creates and initializes a new XML element.
     * 
     * This constructor should <I>only</I> be called from {@link #createAnotherElement() createAnotherElement} to create child iterator.
     *
     * @param entities                 The entity conversion table.
     * @param skipLeadingWhitespace    <code>true</code> if leading and trailing whitespace in PCDATA
     *                                 content has to be removed.
     * @param fillBasicConversionTable <code>true</code> if the basic entities need to be added to
     *                                 the entity list.
     * @param ignoreCase               <code>true</code> if the case of element and attribute names
     *                                 have to be ignored.
     *                                 <dl>
     *                                 <dt><b>Preconditions:</b></dt>
     *                                 </dl>
     *                                 <ul>
     *                                 <li><code>entities != null</code> </li>
     *                                 <li>if <code> fillBasicConversionTable == false</code> then <code>entities </code> contains at least the following entries:
     *                                 <code>amp </code>, <code>lt</code>, <code>gt</code>, <code>apos</code> and <code>quot</code></li>
     *                                 </ul>
     *                                 <dl>
     *                                 <dt><b>Postconditions:</b></dt>
     *                                 </dl>
     *                                 <ul>
     *                                 <li>countChildren() =&gt; 0 </li>
     *                                 <li>iterateChildren() =&gt; empty enumeration </li>
     *                                 <li>enumeratePropertyNames() =&gt; empty enumeration </li>
     *                                 <li>getChildren() =&gt; empty vector</li>
     *                                 <li>getContent() =&gt; "" </li>
     *                                 <li> getLineNr() =&gt; 0 </li>
     *                                 <li>getName() =&gt; null</li>
     *                                 </ul>
     * @see XMLElement#createAnotherElement()
     */
    protected XMLElement(Map<String, char[]> entities, boolean skipLeadingWhitespace, boolean fillBasicConversionTable, boolean ignoreCase) {
        ignoreWhitespace = skipLeadingWhitespace;
        this.ignoreCase = ignoreCase;
        name = null;
        contents = "";
        attributes = new HashMap<String, String>();
        children = new LinkedList<XMLElement>();
        this.entities = entities;
        lineNr = 0;
        if (fillBasicConversionTable) {
            this.entities.put("amp", new char[]{'&'});
            this.entities.put("quot", new char[]{'"'});
            this.entities.put("apos", new char[]{'\''});
            this.entities.put("lt", new char[]{'<'});
            this.entities.put("gt", new char[]{'>'});
        }
    }

    /**
     * Adds a child element.
     *
     * @param child The child element to add.
     *              <dl>
     *              <dt><b>Preconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li><code>child != null</code>
     *              <li><code>child.getName() != null</code>
     *              <li><code>child</code> does not have a parent element
     *              </ul>
     *              <dl>
     *              <dt><b>Postconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li>countChildren() =&gt; old.countChildren() + 1
     *              <li>iterateChildren() =&gt; old.iterateChildren() + child
     *              <li>getChildren() =&gt; old.iterateChildren() + child
     *              </ul>
     * @see XMLElement#countChildren()
     * @see XMLElement#iterateChildren()
     * @see XMLElement#getChildren()
     * @see XMLElement#removeChild(XMLElement) removeChild(XMLElement)
     */
    public void addChild(XMLElement child) {
        children.add(child);
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     *              
     *              <dl>
     *              <dt><b>Preconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li><code>name != null</code>
     *              <li><code>name</code> is a valid XML identifier
     *              <li><code>value != null</code>
     *              </ul>
     *              
     *              <dl>
     *              <dt><b>Postconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li>enumerateAttributeNames() =&gt; old.enumerateAttributeNames() + name
     *              <li>getAttribute(name) =&gt; value
     *              </ul>
     * @see XMLElement#setDoubleAttribute(java.lang.String, double)
     * setDoubleAttribute(String, double)
     * @see XMLElement#setIntAttribute(java.lang.String, int)
     * setIntAttribute(String, int)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getAttribute(java.lang.String) getAttribute(String)
     * @see XMLElement#getAttribute(java.lang.String, java.lang.Object)
     * getAttribute(String, Object)
     * @see XMLElement#getStringAttribute(java.lang.String)
     * getStringAttribute(String)
     * @see XMLElement#getStringAttribute(java.lang.String, java.lang.String)
     * getStringAttribute(String, String)
     */
    public void setAttribute(String name, Object value) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        attributes.put(name, value.toString());
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     *              <dl>
     *              <dt><b>Preconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li><code>name != null</code>
     *              <li><code>name</code> is a valid XML identifier
     *              </ul>
     *              
     *              <dl>
     *              <dt><b>Postconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li>enumerateAttributeNames() =&gt; old.enumerateAttributeNames() + name
     *              <li>getIntAttribute(name) =&gt; value
     *              </ul>
     * @see XMLElement#setDoubleAttribute(java.lang.String, double)
     * setDoubleAttribute(String, double)
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getIntAttribute(java.lang.String) getIntAttribute(String)
     * @see XMLElement#getIntAttribute(java.lang.String, int)
     * getIntAttribute(String, int)
     */
    public void setIntAttribute(String name, int value) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        attributes.put(name, Integer.toString(value));
    }

    /**
     * Adds or modifies an attribute.
     *
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     *              <dl>
     *              <dt><b>Preconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li><code>name != null</code>
     *              <li><code>name</code> is a valid XML identifier
     *              </ul>
     *              
     *              <dl>
     *              <dt><b>Postconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li>enumerateAttributeNames() =&gt; old.enumerateAttributeNames() + name
     *              <li>getDoubleAttribute(name) =&gt; value
     *              </ul>
     * @see XMLElement#setIntAttribute(java.lang.String, int)
     * setIntAttribute(String, int)
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getDoubleAttribute(java.lang.String)
     * getDoubleAttribute(String)
     * @see XMLElement#getDoubleAttribute(java.lang.String, double)
     * getDoubleAttribute(String, double)
     */
    public void setDoubleAttribute(String name, double value) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        attributes.put(name, Double.toString(value));
    }

    /**
     * Returns the number of child iterator of the element.
     * <dl>
     * <dt><b>Postconditions:</b></dt>
     * </dl>
     * <ul>
     * <li><code>result =&gt; 0</code>
     * </ul>
     *
     * @see XMLElement#addChild(XMLElement) addChild(XMLElement)
     * @see XMLElement#iterateChildren()
     * @see XMLElement#getChildren()
     * @see XMLElement#removeChild(XMLElement) removeChild(XMLElement)
     */
    public int countChildren() {
        return children.size();
    }

    /**
     * Enumerates the attribute names.
     * <dl>
     * <dt><b>Postconditions:</b></dt>
     * </dl>
     * <ul>
     * <li><code>result != null</code>
     * </ul>
     *
     * @see XMLElement#setDoubleAttribute(java.lang.String, double)
     * setDoubleAttribute(String, double)
     * @see XMLElement#setIntAttribute(java.lang.String, int)
     * setIntAttribute(String, int)
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#getAttribute(java.lang.String) getAttribute(String)
     * @see XMLElement#getAttribute(java.lang.String, java.lang.Object)
     * getAttribute(String, String)
     * @see XMLElement#getStringAttribute(java.lang.String)
     * getStringAttribute(String)
     * @see XMLElement#getStringAttribute(java.lang.String, java.lang.String)
     * getStringAttribute(String, String)
     * @see XMLElement#getIntAttribute(java.lang.String) getIntAttribute(String)
     * @see XMLElement#getIntAttribute(java.lang.String, int)
     * getIntAttribute(String, int)
     * @see XMLElement#getDoubleAttribute(java.lang.String)
     * getDoubleAttribute(String)
     * @see XMLElement#getDoubleAttribute(java.lang.String, double)
     * getDoubleAttribute(String, double)
     * @see XMLElement#getBooleanAttribute(java.lang.String, java.lang.String, java.lang.String, boolean) getBooleanAttribute(String, String,
     * String, boolean)
     */
    public Iterator<String> enumerateAttributeNames() {
        return attributes.keySet().iterator();
    }

    /**
     * Enumerates the child iterator.
     * <dl>
     * <dt><b>Postconditions:</b></dt>
     * </dl>
     * <ul>
     * <li><code>result != null</code>
     * </ul>
     *
     * @see XMLElement#addChild(XMLElement) addChild(XMLElement)
     * @see XMLElement#countChildren()
     * @see XMLElement#getChildren()
     * @see XMLElement#removeChild(XMLElement) removeChild(XMLElement)
     */
    public Iterator<XMLElement> iterateChildren() {
        return children.iterator();
    }

    /**
     * Returns the child iterator as a ArrayList. It is safe to modify this
     * ArrayList.
     * <dl>
     * <dt><b>Postconditions:</b></dt>
     * </dl>
     * <ul>
     * <li><code>result != null</code>
     * </ul>
     *
     * @see XMLElement#addChild(XMLElement) addChild(XMLElement)
     * @see XMLElement#countChildren()
     * @see XMLElement#iterateChildren()
     * @see XMLElement#removeChild(XMLElement) removeChild(XMLElement)
     */
    public List<XMLElement> getChildren() {
        return new ArrayList<XMLElement>(children);
    }

    /**
     * A lookup method to find elements
     *
     * @param name Tag name to look for
     * @return First element matching tag name
     */
    public XMLElement getFirstChildWithName(String name) {
        for (XMLElement elem : children) {
            if (name.equals(elem.getName())) {
                return elem;
            }
        }
        return null;
    }

    /**
     * A lookup method to find elements
     *
     * @param name Tag name to look for
     * @return All elements matching tag name
     */
    public List<XMLElement> getChildrenWithName(String name) {
        List<XMLElement> elements = new LinkedList<>();
        for (XMLElement elem : children) {
            if (name.equals(elem.getName())) {
                elements.add(elem);
            }
        }
        return elements;
    }

    /**
     * Returns the PCDATA content of the object. If there is no such content, <CODE>null</CODE> is returned.
     *
     * @see XMLElement#setContent(java.lang.String) setContent(String)
     */
    public String getContent() {
        return contents;
    }

    /**
     * Returns the line nr in the source data on which the element is found.
     * This method returns <code>0</code> there is no associated source data.
     * <dl>
     * <dt><b>Postconditions:</b></dt>
     * <dd>
     * <ul>
     * <li><code>result =&gt; 0</code>
     * </ul>
     * </dd>
     * </dl>
     */
    public int getLineNr() {
        return lineNr;
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>null</code> is returned.
     *
     * @param name The name of the attribute.
     *             <dl>
     *             <dt><b>Preconditions:</b></dt>
     *             <dd>
     *             <ul>
     *             <li><code>name != null</code>
     *             <li><code>name</code> is a valid XML identifier
     *             </ul>
     *             </dd>
     *             </dl>
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getAttribute(java.lang.String, java.lang.Object)
     * getAttribute(String, Object)
     */
    public Object getAttribute(String name) {
        return this.getAttribute(name, null);
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param name         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *                     <dl>
     *                     <dt><b>Preconditions:</b></dt>
     *                     <dd>
     *                     <ul>
     *                     <li><code>name != null</code>
     *                     <li><code>name</code> is a valid XML identifier
     *                     </ul>
     *                     </dd>
     *                     </dl>
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getAttribute(java.lang.String) getAttribute(String)
     */
    public Object getAttribute(String name, Object defaultValue) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        Object value = attributes.get(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * 
     * As an example, if valueSet contains the mapping <code>"one" =&gt;
     * "1"</code> and the element contains the attribute <code>attr="one"</code> , then <code>getAttribute("attr", mapping, defaultKey, false)</code> returns <code>"1"</code>.
     *
     * @param name          The name of the attribute.
     * @param valueSet      HashMap mapping keySet().iterator to values.
     * @param defaultKey    Key to use if the attribute is missing.
     * @param allowLiterals <code>true</code> if literals are valid.
     *                      <dl>
     *                      <dt><b>Preconditions:</b></dt><dd>
     *                      <ul>
     *                      <li><code>name != null</code> <li><code>name</code> is a valid XML identifier <li><code>valueSet</code> != null <li>the keySet().iterator of <code>valueSet</code>
     *                      are strings
     *                      </ul>
     *                      </dd>
     *                      </dl>
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getAttribute(java.lang.String) getAttribute(String)
     * @see XMLElement#getAttribute(java.lang.String, java.lang.Object)
     * getAttribute(String, Object)
     */
    public Object getAttribute(String name, Map<String, Object> valueSet, String defaultKey, boolean allowLiterals) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        String key = attributes.get(name);
        Object result;
        if (key == null) {
            key = defaultKey;
        }
        result = valueSet.get(key);
        if (result == null) {
            if (allowLiterals) {
                result = key;
            } else {
                throw invalidValue(name, key);
            }
        }
        return result;
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>null</code> is returned.
     *
     * @param name The name of the attribute.
     *             <dl>
     *             <dt><b>Preconditions:</b></dt>
     *             <dd>
     *             <ul>
     *             <li><code>name != null</code>
     *             <li><code>name</code> is a valid XML identifier
     *             </ul>
     *             </dd>
     *             </dl>
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getStringAttribute(java.lang.String, java.lang.String)
     * getStringAttribute(String, String)
     */
    public String getStringAttribute(String name) {
        return this.getStringAttribute(name, null);
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param name         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *                     <dl>
     *                     <dt><b>Preconditions:</b></dt>
     *                     <dd>
     *                     <ul>
     *                     <li><code>name != null</code>
     *                     <li><code>name</code> is a valid XML identifier
     *                     </ul>
     *                     </dd>
     *                     </dl>
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getStringAttribute(java.lang.String)
     * getStringAttribute(String)
     */
    public String getStringAttribute(String name, String defaultValue) {
        return (String) this.getAttribute(name, defaultValue);
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * 
     * As an example, if valueSet contains the mapping <code>"one" =&gt;
     * "1"</code> and the element contains the attribute <code>attr="one"</code> , then <code>getAttribute("attr", mapping, defaultKey, false)</code> returns <code>"1"</code>.
     *
     * @param name          The name of the attribute.
     * @param valueSet      HashMap mapping keySet().iterator to values.
     * @param defaultKey    Key to use if the attribute is missing.
     * @param allowLiterals <code>true</code> if literals are valid.
     *                      <dl>
     *                      <dt><b>Preconditions:</b></dt><dd>
     *                      <ul>
     *                      <li><code>name != null</code> <li><code>name</code> is a valid XML identifier <li><code>valueSet</code> != null <li>the keySet().iterator of <code>valueSet</code>
     *                      are strings <li>the values of <code>valueSet</code> are strings
     *                      </ul>
     *                      </dd>
     *                      </dl>
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getStringAttribute(java.lang.String)
     * getStringAttribute(String)
     * @see XMLElement#getStringAttribute(java.lang.String, java.lang.String)
     * getStringAttribute(String, String)
     */
    public String getStringAttribute(String name, Map<String, Object> valueSet, String defaultKey, boolean allowLiterals) {
        return (String) this.getAttribute(name, valueSet, defaultKey, allowLiterals);
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>0</code> is returned.
     *
     * @param name The name of the attribute.
     *             <dl>
     *             <dt><b>Preconditions:</b></dt>
     *             <dd>
     *             <ul>
     *             <li><code>name != null</code>
     *             <li><code>name</code> is a valid XML identifier
     *             </ul>
     *             </dd>
     *             </dl>
     * @see XMLElement#setIntAttribute(java.lang.String, int)
     * setIntAttribute(String, int)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getIntAttribute(java.lang.String, int)
     * getIntAttribute(String, int)
     */
    public int getIntAttribute(String name) {
        return this.getIntAttribute(name, 0);
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param name         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *                     <dl>
     *                     <dt><b>Preconditions:</b></dt>
     *                     <dd>
     *                     <ul>
     *                     <li><code>name != null</code>
     *                     <li><code>name</code> is a valid XML identifier
     *                     </ul>
     *                     </dd>
     *                     </dl>
     * @see XMLElement#setIntAttribute(java.lang.String, int)
     * setIntAttribute(String, int)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getIntAttribute(java.lang.String) getIntAttribute(String)
     */
    public int getIntAttribute(String name, int defaultValue) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        String value = attributes.get(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw invalidValue(name, value);
            }
        }
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * 
     * As an example, if valueSet contains the mapping <code>"one" =&gt; 1</code> and the element contains the attribute <code>attr="one"</code>, then
     * <code>getIntAttribute("attr", mapping, defaultKey, false)</code> returns <code>1</code>.
     *
     * @param name                The name of the attribute.
     * @param valueSet            HashMap mapping keySet().iterator to values.
     * @param defaultKey          Key to use if the attribute is missing.
     * @param allowLiteralNumbers <code>true</code> if literal numbers are valid.
     *                            <dl>
     *                            <dt><b>Preconditions:</b></dt><dd>
     *                            <ul>
     *                            <li><code>name != null</code> <li><code>name</code> is a valid XML identifier <li><code>valueSet</code> != null <li>the keySet().iterator of <code>valueSet</code>
     *                            are strings <li>the values of <code>valueSet</code> are Integer objects <li><code> defaultKey</code> is either <code>null</code>, a key in <code> valueSet</code>
     *                            or an integer.
     *                            </ul>
     *                            </dd>
     *                            </dl>
     * @see XMLElement#setIntAttribute(java.lang.String, int)
     * setIntAttribute(String, int)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getIntAttribute(java.lang.String) getIntAttribute(String)
     * @see XMLElement#getIntAttribute(java.lang.String, int)
     * getIntAttribute(String, int)
     */
    public int getIntAttribute(String name, Map<String, Object> valueSet, String defaultKey, boolean allowLiteralNumbers) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        Object key = attributes.get(name);
        Integer result;
        if (key == null) {
            key = defaultKey;
        }
        try {
            result = (Integer) valueSet.get(key);
        } catch (ClassCastException e) {
            throw invalidValueSet(name);
        }
        if (result == null) {
            if (!allowLiteralNumbers) {
                throw invalidValue(name, (String) key);
            }
            try {
                result = Integer.valueOf((String) key);
            } catch (NumberFormatException e) {
                throw invalidValue(name, (String) key);
            }
        }
        return result.intValue();
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>0.0</code> is returned.
     *
     * @param name The name of the attribute.
     *             <dl>
     *             <dt><b>Preconditions:</b></dt>
     *             <dd>
     *             <ul>
     *             <li><code>name != null</code>
     *             <li><code>name</code> is a valid XML identifier
     *             </ul>
     *             </dd>
     *             </dl>
     * @see XMLElement#setDoubleAttribute(java.lang.String, double)
     * setDoubleAttribute(String, double)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getDoubleAttribute(java.lang.String, double)
     * getDoubleAttribute(String, double)
     */
    public double getDoubleAttribute(String name) {
        return this.getDoubleAttribute(name, 0.);
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>defaultValue</code> is returned.
     *
     * @param name         The name of the attribute.
     * @param defaultValue Key to use if the attribute is missing.
     *                     <dl>
     *                     <dt><b>Preconditions:</b></dt>
     *                     <dd>
     *                     <ul>
     *                     <li><code>name != null</code>
     *                     <li><code>name</code> is a valid XML identifier
     *                     </ul>
     *                     </dd>
     *                     </dl>
     * @see XMLElement#setDoubleAttribute(java.lang.String, double)
     * setDoubleAttribute(String, double)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getDoubleAttribute(java.lang.String)
     * getDoubleAttribute(String)
     */
    public double getDoubleAttribute(String name, double defaultValue) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        String value = attributes.get(name);
        if (value == null) {
            return defaultValue;
        } else {
            try {
                return Double.valueOf(value).doubleValue();
            } catch (NumberFormatException e) {
                throw invalidValue(name, value);
            }
        }
    }

    /**
     * Returns an attribute by looking up a key in a hashtable. If the attribute
     * doesn't exist, the value corresponding to defaultKey is returned.
     * 
     * As an example, if valueSet contains the mapping <code>"one" =&gt;
     * 1.0</code> and the element contains the attribute <code>attr="one"</code> , then <code>getDoubleAttribute("attr", mapping, defaultKey, false)</code> returns <code>1.0</code>.
     *
     * @param name                The name of the attribute.
     * @param valueSet            HashMap mapping keySet().iterator to values.
     * @param defaultKey          Key to use if the attribute is missing.
     * @param allowLiteralNumbers <code>true</code> if literal numbers are valid.
     *                            <dl>
     *                            <dt><b>Preconditions:</b></dt><dd>
     *                            </dl>
     *                            <ul>
     *                            <li><code>name != null</code> <li><code>name</code> is a valid XML identifier <li><code>valueSet != null</code> <li>the keySet().iterator of <code>valueSet</code>
     *                            are strings <li>the values of <code>valueSet</code> are Double objects <li><code> defaultKey</code> is either <code>null</code>, a key in <code> valueSet</code>
     *                            or a double.
     *                            </ul>
     * @see XMLElement#setDoubleAttribute(java.lang.String, double)
     * setDoubleAttribute(String, double)
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#getDoubleAttribute(java.lang.String)
     * getDoubleAttribute(String)
     * @see XMLElement#getDoubleAttribute(java.lang.String, double)
     * getDoubleAttribute(String, double)
     */
    public double getDoubleAttribute(String name, Map<String, Object> valueSet, String defaultKey, boolean allowLiteralNumbers) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        Object key = attributes.get(name);
        Double result;
        if (key == null) {
            key = defaultKey;
        }
        try {
            result = (Double) valueSet.get(key);
        } catch (ClassCastException e) {
            throw invalidValueSet(name);
        }
        if (result == null) {
            if (!allowLiteralNumbers) {
                throw invalidValue(name, (String) key);
            }
            try {
                result = Double.valueOf((String) key);
            } catch (NumberFormatException e) {
                throw invalidValue(name, (String) key);
            }
        }
        return result.doubleValue();
    }

    /**
     * Returns an attribute of the element. If the attribute doesn't exist, <code>defaultValue</code> is returned. If the value of the attribute is
     * equal to <code>trueValue</code>, <code>true</code> is returned. If the
     * value of the attribute is equal to <code>falseValue</code>, <code>false</code> is returned. If the value doesn't match <code>trueValue</code> or <code>falseValue</code>, an
     * exception is
     * thrown.
     *
     * @param name         The name of the attribute.
     * @param trueValue    The value associated with <code>true</code>.
     * @param falseValue   The value associated with <code>true</code>.
     * @param defaultValue Value to use if the attribute is missing.
     *                     <dl>
     *                     <dt><b>Preconditions:</b></dt>
     *                     </dl>
     *                     <ul>
     *                     <li><code>name != null</code>
     *                     <li><code>name</code> is a valid XML identifier
     *                     <li><code>trueValue</code> and <code>falseValue</code> are different strings.
     *                     </ul>
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#removeAttribute(java.lang.String) removeAttribute(String)
     * @see XMLElement#enumerateAttributeNames()
     */
    public boolean getBooleanAttribute(String name, String trueValue, String falseValue, boolean defaultValue) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        Object value = attributes.get(name);
        if (value == null) {
            return defaultValue;
        } else if (value.equals(trueValue)) {
            return true;
        } else if (value.equals(falseValue)) {
            return false;
        } else {
            throw invalidValue(name, (String) value);
        }
    }

    /**
     * Returns the name of the element.
     * @return The name of the element
     * @see XMLElement#setName(java.lang.String) setName(String)
     */
    public String getName() {
        return name;
    }

    /**
     * Removes a child element.
     *
     * @param child The child element to remove.
     *              <dl>
     *              <dt><b>Preconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li><code>child != null</code>
     *              <li><code>child</code> is a child element of the receiver
     *              </ul>
     *              
     *              <dl>
     *              <dt><b>Postconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li>countChildren() =&gt; old.countChildren() - 1
     *              <li>iterateChildren() =&gt; old.iterateChildren() - child
     *              <li>getChildren() =&gt; old.iterateChildren() - child
     *              </ul>
     * @see XMLElement#addChild(XMLElement) addChild(XMLElement)
     * @see XMLElement#countChildren()
     * @see XMLElement#iterateChildren()
     * @see XMLElement#getChildren()
     */
    public void removeChild(XMLElement child) {
        children.remove(child);
    }

    /**
     * Removes an attribute.
     *
     * @param name The name of the attribute.
     *             <dl>
     *             <dt><b>Preconditions:</b></dt>
     *             </dl>
     *             <ul>
     *             <li><code>name != null</code>
     *             <li><code>name</code> is a valid XML identifier
     *             </ul>
     *             <dl>
     *             <dt><b>Postconditions:</b></dt>
     *             </dl>
     *             <ul>
     *             <li>enumerateAttributeNames() =&gt; old.enumerateAttributeNames() - name
     *             <li>getAttribute(name) =&gt; <code>null</code>
     *             </ul>
     * @see XMLElement#enumerateAttributeNames()
     * @see XMLElement#setDoubleAttribute(java.lang.String, double)
     * setDoubleAttribute(String, double)
     * @see XMLElement#setIntAttribute(java.lang.String, int)
     * setIntAttribute(String, int)
     * @see XMLElement#setAttribute(java.lang.String, java.lang.Object)
     * setAttribute(String, Object)
     * @see XMLElement#getAttribute(java.lang.String) getAttribute(String)
     * @see XMLElement#getAttribute(java.lang.String, java.lang.Object)
     * getAttribute(String, Object)
     * @see XMLElement#getStringAttribute(java.lang.String)
     * getStringAttribute(String)
     * @see XMLElement#getStringAttribute(java.lang.String, java.lang.String)
     * getStringAttribute(String, String)
     * @see XMLElement#getIntAttribute(java.lang.String) getIntAttribute(String)
     * @see XMLElement#getIntAttribute(java.lang.String, int)
     * getIntAttribute(String, int)
     * @see XMLElement#getDoubleAttribute(java.lang.String)
     * getDoubleAttribute(String)
     * @see XMLElement#getDoubleAttribute(java.lang.String, double)
     * getDoubleAttribute(String, double)
     * @see XMLElement#getBooleanAttribute(java.lang.String, java.lang.String, java.lang.String, boolean) getBooleanAttribute(String, String,
     * String, boolean)
     */
    public void removeAttribute(String name) {
        if (ignoreCase) {
            name = name.toUpperCase();
        }
        attributes.remove(name);
    }

    /**
     * Creates a new similar XML element.
     * 
     * You should override this method when subclassing XMLElement.
     * @return Similar xml element
     */
    public XMLElement createAnotherElement() {
        return new XMLElement(entities, ignoreWhitespace, false, ignoreCase);
    }

    /**
     * Changes the content string.
     *
     * @param content The new content string.
     */
    public void setContent(String content) {
        contents = content;
    }

    /**
     * Changes the name of the element.
     *
     * @param name The new name.
     *             <dl>
     *             <dt><b>Preconditions:</b></dt>
     *             </dl>
     *             <ul>
     *             <li><code>name != null</code>
     *             <li><code>name</code> is a valid XML identifier
     *             </ul>
     * @see XMLElement#getName()
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Writes the XML element to a string.
     *
     * @see XMLElement#write(java.io.Writer) write(Writer)
     */
    @Override
    public String toString() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            write(writer);
            writer.flush();
            return new String(out.toByteArray());
        } catch (IOException e) {
            // Java exception handling suxx
            return super.toString();
        }
    }

    /**
     * Writes the XML element to a writer.
     *
     * @param writer The writer to write the XML data to.
     *               <dl>
     *               <dt><b>Preconditions:</b></dt>
     *               </dl>
     *               <ul>
     *               <li><code>writer != null</code>
     *               <li><code>writer</code> is not closed
     *               </ul>
     * @throws java.io.IOException If the data could not be written to the writer.
     * @see XMLElement#toString()
     */
    public void write(Writer writer) throws IOException {
        if (name == null) {
            writeEncoded(writer, contents);
            return;
        }
        writer.write('<');
        writer.write(name);
        if (!attributes.isEmpty()) {
            Iterator<String> iter = attributes.keySet().iterator();
            while (iter.hasNext()) {
                writer.write(' ');
                String key = iter.next();
                String value = attributes.get(key);
                writer.write(key);
                writer.write('=');
                writer.write('"');
                writeEncoded(writer, value);
                writer.write('"');
            }
        }
        if ((contents != null) && (contents.length() > 0)) {
            writer.write('>');
            writeEncoded(writer, contents);
            writer.write('<');
            writer.write('/');
            writer.write(name);
            writer.write('>');
        } else if (children.isEmpty()) {
            writer.write('/');
            writer.write('>');
        } else {
            writer.write('>');
            Iterator<XMLElement> iter = iterateChildren();
            while (iter.hasNext()) {
                XMLElement child = iter.next();
                child.write(writer);
            }
            writer.write('<');
            writer.write('/');
            writer.write(name);
            writer.write('>');
        }
    }

    /**
     * Writes a string encoded to a writer.
     *
     * @param writer The writer to write the XML data to.
     * @param str    The string to write encoded.
     *               <dl>
     *               <dt><b>Preconditions:</b></dt>
     *               </dl>
     *               <ul>
     *               <li><code>writer != null</code>
     *               <li><code>writer</code> is not closed
     *               <li><code>str != null</code>
     *               </ul>
     */
    private void writeEncoded(Writer writer, String str) throws IOException {
        for (int i = 0; i < str.length(); i += 1) {
            char ch = str.charAt(i);
            switch (ch) {
                case '<':
                    writer.write('&');
                    writer.write('l');
                    writer.write('t');
                    writer.write(';');
                    break;
                case '>':
                    writer.write('&');
                    writer.write('g');
                    writer.write('t');
                    writer.write(';');
                    break;
                case '&':
                    writer.write('&');
                    writer.write('a');
                    writer.write('m');
                    writer.write('p');
                    writer.write(';');
                    break;
                case '"':
                    writer.write('&');
                    writer.write('q');
                    writer.write('u');
                    writer.write('o');
                    writer.write('t');
                    writer.write(';');
                    break;
                case '\'':
                    writer.write('&');
                    writer.write('a');
                    writer.write('p');
                    writer.write('o');
                    writer.write('s');
                    writer.write(';');
                    break;
                default:
                    int unicode = ch;
                    if ((unicode < 32) || (unicode > 126)) {
                        writer.write('&');
                        writer.write('#');
                        writer.write('x');
                        writer.write(Integer.toString(unicode, 16));
                        writer.write(';');
                    } else {
                        writer.write(ch);
                    }
            }
        }
    }

    /**
     * Creates a parse exception for when an invalid valueset is given to a
     * method.
     *
     * @param name The name of the entity.
     *             <dl>
     *             <dt><b>Preconditions:</b></dt>
     *             </dl>
     *             <ul>
     *             <li><code>name != null</code>
     *             </ul>
     */
    private XMLParseException invalidValueSet(String name) {
        String msg = "Invalid value set (entity name = \"" + name + "\")";
        return new XMLParseException(getName(), parserLineNr, msg);
    }

    /**
     * Creates a parse exception for when an invalid value is given to a method.
     *
     * @param name  The name of the entity.
     * @param value The value of the entity.
     *              <dl>
     *              <dt><b>Preconditions:</b></dt>
     *              </dl>
     *              <ul>
     *              <li><code>name != null</code>
     *              <li><code>value != null</code>
     *              </ul>
     */
    private XMLParseException invalidValue(String name, String value) {
        String msg = "Attribute \"" + name + "\" does not contain a valid " + "value (\"" + value + "\")";
        return new XMLParseException(getName(), parserLineNr, msg);
    }

}
