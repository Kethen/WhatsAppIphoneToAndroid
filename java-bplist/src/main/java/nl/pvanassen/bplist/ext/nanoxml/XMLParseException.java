/* XMLParseException.java
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

/**
 * An XMLParseException is thrown when an error occures while parsing an XML
 * string.
 * <P>
 * $Revision: 1.4 $<BR>
 * $Date: 2002/03/24 10:27:59 $
 * <P>
 * 
 * @see XMLElement
 * 
 * @author Marc De Scheemaecker
 * @version $Name: RELEASE_2_2_1 $, $Revision: 1.4 $
 */
public class XMLParseException extends RuntimeException {

    private static final long serialVersionUID = 6392534332039988250L;

    /**
     * Indicates that no line number has been associated with this exception.
     */
    public static final int NO_LINE = -1;

    /**
     * The line number in the source code where the error occurred, or
     * <code>NO_LINE</code> if the line number is unknown.
     * 
     * <dl>
     * <dt><b>Invariants:</b></dt>
     * <dd>
     * <ul>
     * <li><code>lineNr &gt; 0 || lineNr == NO_LINE</code></li>
     * </ul>
     * </dd>
     * </dl>
     */
    private int lineNr;

    /**
     * Creates an exception.
     * 
     * @param name
     *            The name of the element where the error is located.
     * @param message
     *            A message describing what went wrong.
     * 
     *            <dl>
     *            <dt><b>Preconditions:</b></dt>
     *            </dl>
     *            <ul>
     *            <li><code>message != null</code>
     *            </ul>
     *
     *            <dl>
     *            <dt><b>Postconditions:</b></dt>
     *            </dl>
     *            <ul>
     *            <li>getLineNr() =&gt; NO_LINE
     *            </ul>
     */
    public XMLParseException(String name, String message) {
	super("XML Parse Exception during parsing of "
		+ ((name == null) ? "the XML definition"
			: ("a " + name + " element")) + ": " + message);
	this.lineNr = XMLParseException.NO_LINE;
    }

    /**
     * Creates an exception.
     * 
     * @param name
     *            The name of the element where the error is located.
     * @param lineNr
     *            The number of the line in the input.
     * @param message
     *            A message describing what went wrong.
     * 
     *            <dl>
     *            <dt><b>Preconditions:</b></dt>
     *            </dl>
     *            <ul>
     *            <li><code>message != null</code>
     *            <li><code>lineNr &gt; 0</code>
     *            </ul>
     *
     *            <dl>
     *            <dt><b>Postconditions:</b></dt>
     *            </dl>
     *            <ul>
     *            <li>getLineNr() =&gt; lineNr
     *            </ul>
     */
    public XMLParseException(String name, int lineNr, String message) {
	super("XML Parse Exception during parsing of "
		+ ((name == null) ? "the XML definition"
			: ("a " + name + " element")) + " at line " + lineNr
		+ ": " + message);
	this.lineNr = lineNr;
    }

    /**
     * Where the error occurred, or <code>NO_LINE</code> if the line number is
     * unknown.
     * @return Line number
     * @see XMLParseException#NO_LINE
     */
    public int getLineNr() {
	return this.lineNr;
    }

}
