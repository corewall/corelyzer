/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Questions or comments about CoreWall should be directed to
 * cavern@evl.uic.edu
 *
 *****************************************************************************/
package corelyzer.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/*
 * Copyright 1999-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A sample DOM writer. This sample program illustrates how to traverse a DOM
 * tree in order to print a document that is parsed.
 * 
 * @author Andy Clark, IBM
 * @author ARUN RAO, EVL/UIC - Modified by
 * @version $Id: Writer.java 329525 2005-10-30 05:39:22Z mrglavas $
 */
public class XMLWriter {

	//
	// Constants
	//

	// feature ids

	/** Namespaces feature id (http://xml.org/sax/features/namespaces). */
	protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

	/** Validation feature id (http://xml.org/sax/features/validation). */
	protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

	/**
	 * Schema validation feature id
	 * (http://apache.org/xml/features/validation/schema).
	 */
	protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

	/**
	 * Schema full checking feature id
	 * (http://apache.org/xml/features/validation/schema-full-checking).
	 */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";

	/**
	 * Honour all schema locations feature id
	 * (http://apache.org/xml/features/honour-all-schemaLocations).
	 */
	protected static final String HONOUR_ALL_SCHEMA_LOCATIONS_ID = "http://apache.org/xml/features/honour-all-schemaLocations";

	/**
	 * Validate schema annotations feature id
	 * (http://apache.org/xml/features/validate-annotations).
	 */
	protected static final String VALIDATE_ANNOTATIONS_ID = "http://apache.org/xml/features/validate-annotations";

	/**
	 * Generate synthetic schema annotations feature id
	 * (http://apache.org/xml/features/generate-synthetic-annotations).
	 */
	protected static final String GENERATE_SYNTHETIC_ANNOTATIONS_ID = "http://apache.org/xml/features/generate-synthetic-annotations";

	/**
	 * Dynamic validation feature id
	 * (http://apache.org/xml/features/validation/dynamic).
	 */
	protected static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";

	/**
	 * Load external DTD feature id
	 * (http://apache.org/xml/features/nonvalidating/load-external-dtd).
	 */
	protected static final String LOAD_EXTERNAL_DTD_FEATURE_ID = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

	/** XInclude feature id (http://apache.org/xml/features/xinclude). */
	protected static final String XINCLUDE_FEATURE_ID = "http://apache.org/xml/features/xinclude";

	/**
	 * XInclude fixup base URIs feature id
	 * (http://apache.org/xml/features/xinclude/fixup-base-uris).
	 */
	protected static final String XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID = "http://apache.org/xml/features/xinclude/fixup-base-uris";

	/**
	 * XInclude fixup language feature id
	 * (http://apache.org/xml/features/xinclude/fixup-language).
	 */
	protected static final String XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID = "http://apache.org/xml/features/xinclude/fixup-language";

	// default settings

	/** Default parser name. */
	protected static final String DEFAULT_PARSER_NAME = "dom.wrappers.Xerces";

	/** Default namespaces support (true). */
	protected static final boolean DEFAULT_NAMESPACES = true;

	/** Default validation support (false). */
	protected static final boolean DEFAULT_VALIDATION = false;

	/** Default load external DTD (true). */
	protected static final boolean DEFAULT_LOAD_EXTERNAL_DTD = true;

	/** Default Schema validation support (false). */
	protected static final boolean DEFAULT_SCHEMA_VALIDATION = false;

	/** Default Schema full checking support (false). */
	protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;

	/** Default honour all schema locations (false). */
	protected static final boolean DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS = false;

	/** Default validate schema annotations (false). */
	protected static final boolean DEFAULT_VALIDATE_ANNOTATIONS = false;

	/** Default generate synthetic schema annotations (false). */
	protected static final boolean DEFAULT_GENERATE_SYNTHETIC_ANNOTATIONS = false;

	/** Default dynamic validation support (false). */
	protected static final boolean DEFAULT_DYNAMIC_VALIDATION = false;

	/** Default XInclude processing support (false). */
	protected static final boolean DEFAULT_XINCLUDE = false;

	/** Default XInclude fixup base URIs support (true). */
	protected static final boolean DEFAULT_XINCLUDE_FIXUP_BASE_URIS = true;

	/** Default XInclude fixup language support (true). */
	protected static final boolean DEFAULT_XINCLUDE_FIXUP_LANGUAGE = true;

	/** Default canonical output (false). */
	protected static final boolean DEFAULT_CANONICAL = false;

	//
	// Data
	//

	/** Main program entry point. */
	public static void write(final DocumentImpl doc, final OutputStream ostream) {
		System.out.println("Going to write " + doc);
		// variables
		Writer writer = null;
		boolean canonical = DEFAULT_CANONICAL;

		// setup writer
		if (writer == null) {
			writer = new Writer();
			try {
				writer.setOutput(ostream, "UTF8");
			} catch (UnsupportedEncodingException e) {
				System.err.println("error: Unable to set output. Exiting.");
				System.exit(1);
			}
		}

		// parse file
		writer.setCanonical(canonical);
		try {
			writer.write(doc);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	/** Print writer. */
	protected PrintWriter fOut;

	/** Canonical output. */
	protected boolean fCanonical;

	//
	// Constructors
	//

	/** Processing XML 1.1 document. */
	protected boolean fXML11;

	/** Default constructor. */
	public XMLWriter() {
	} // <init>()

	//
	// Public methods
	//

	public XMLWriter(final boolean canonical) {
		fCanonical = canonical;
	} // <init>(boolean)

	/** Extracts the XML version from the Document. */
	protected String getVersion(final Document document) {
		if (document == null) {
			return null;
		}
		String version = null;
		Method getXMLVersion = null;
		try {
			getXMLVersion = document.getClass().getMethod("getXmlVersion", new Class[] {});
			// If Document class implements DOM L3, this method will exist.
			if (getXMLVersion != null) {
				version = (String) getXMLVersion.invoke(document, (Object[]) null);
			}
		} catch (Exception e) {
			// Either this locator object doesn't have
			// this method, or we're on an old JDK.
		}
		return version;
	} // getVersion(Document)

	/** Normalizes and print the given character. */
	protected void normalizeAndPrint(final char c, final boolean isAttValue) {

		switch (c) {
			case '<': {
				fOut.print("&lt;");
				break;
			}
			case '>': {
				fOut.print("&gt;");
				break;
			}
			case '&': {
				fOut.print("&amp;");
				break;
			}
			case '"': {
				// A '"' that appears in character data
				// does not need to be escaped.
				if (isAttValue) {
					fOut.print("&quot;");
				} else {
					fOut.print("\"");
				}
				break;
			}
			case '\r': {
				// If CR is part of the document's content, it
				// must not be printed as a literal otherwise
				// it would be normalized to LF when the document
				// is reparsed.
				fOut.print("&#xD;");
				break;
			}
			case '\n': {
				if (fCanonical) {
					fOut.print("&#xA;");
					break;
				}
				// else, default print char
			}
			default: {
				// In XML 1.1, control chars in the ranges [#x1-#x1F, #x7F-#x9F]
				// must be escaped.
				//
				// Escape space characters that would be normalized to #x20 in
				// attribute values
				// when the document is reparsed.
				//
				// Escape NEL (0x85) and LSEP (0x2028) that appear in content
				// if the document is XML 1.1, since they would be normalized to
				// LF
				// when the document is reparsed.
				if (fXML11 && (c >= 0x01 && c <= 0x1F && c != 0x09 && c != 0x0A || c >= 0x7F && c <= 0x9F || c == 0x2028) || isAttValue
						&& (c == 0x09 || c == 0x0A)) {
					fOut.print("&#x");
					fOut.print(Integer.toHexString(c).toUpperCase());
					fOut.print(";");
				} else {
					fOut.print(c);
				}
			}
		}
	} // normalizeAndPrint(char,boolean)

	/** Normalizes and prints the given string. */
	protected void normalizeAndPrint(final String s, final boolean isAttValue) {

		int len = s != null ? s.length() : 0;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			normalizeAndPrint(c, isAttValue);
		}

	} // normalizeAndPrint(String,boolean)

	/** Sets whether output is canonical. */
	public void setCanonical(final boolean canonical) {
		fCanonical = canonical;
	} // setCanonical(boolean)

	//
	// Protected methods
	//

	/** Sets the output writer. */
	public void setOutput(final java.io.Writer writer) {

		fOut = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);

	} // setOutput(java.io.Writer)

	/** Sets the output stream for printing. */
	public void setOutput(final OutputStream stream, String encoding) throws UnsupportedEncodingException {

		if (encoding == null) {
			encoding = "UTF8";
		}

		java.io.Writer writer = new OutputStreamWriter(stream, encoding);
		fOut = new PrintWriter(writer);

	} // setOutput(OutputStream,String)

	/** Returns a sorted list of attributes. */
	protected Attr[] sortAttributes(final NamedNodeMap attrs) {

		int len = attrs != null ? attrs.getLength() : 0;
		Attr array[] = new Attr[len];
		for (int i = 0; i < len; i++) {
			array[i] = (Attr) attrs.item(i);
		}
		for (int i = 0; i < len - 1; i++) {
			String name = array[i].getNodeName();
			int index = i;
			for (int j = i + 1; j < len; j++) {
				String curName = array[j].getNodeName();
				if (curName.compareTo(name) < 0) {
					name = curName;
					index = j;
				}
			}
			if (index != i) {
				Attr temp = array[i];
				array[i] = array[index];
				array[index] = temp;
			}
		}

		return array;

	} // sortAttributes(NamedNodeMap):Attr[]

	//
	// write
	//

	/** Writes the specified node, recursively. */
	public void write(final Node node) {

		// is there anything to do?
		if (node == null) {
			return;
		}

		short type = node.getNodeType();
		switch (type) {
			case Node.DOCUMENT_NODE: {
				Document document = (Document) node;
				fXML11 = "1.1".equals(getVersion(document));
				if (!fCanonical) {
					if (fXML11) {
						fOut.println("<?xml version=\"1.1\" encoding=\"UTF-8\"?>");
					} else {
						fOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
					}
					fOut.flush();
					write(document.getDoctype());
				}
				write(document.getDocumentElement());
				break;
			}

			case Node.DOCUMENT_TYPE_NODE: {
				DocumentType doctype = (DocumentType) node;
				fOut.print("<!DOCTYPE ");
				fOut.print(doctype.getName());
				String publicId = doctype.getPublicId();
				String systemId = doctype.getSystemId();
				if (publicId != null) {
					fOut.print(" PUBLIC '");
					fOut.print(publicId);
					fOut.print("' '");
					fOut.print(systemId);
					fOut.print('\'');
				} else if (systemId != null) {
					fOut.print(" SYSTEM '");
					fOut.print(systemId);
					fOut.print('\'');
				}
				String internalSubset = doctype.getInternalSubset();
				if (internalSubset != null) {
					fOut.println(" [");
					fOut.print(internalSubset);
					fOut.print(']');
				}
				fOut.println('>');
				break;
			}

			case Node.ELEMENT_NODE: {
				fOut.print('<');
				fOut.print(node.getNodeName());
				Attr attrs[] = sortAttributes(node.getAttributes());
				for (int i = 0; i < attrs.length; i++) {
					Attr attr = attrs[i];
					fOut.print(' ');
					fOut.print(attr.getNodeName());
					fOut.print("=\"");
					normalizeAndPrint(attr.getNodeValue(), true);
					fOut.print('"');
				}
				fOut.print('>');
				fOut.flush();

				Node child = node.getFirstChild();
				while (child != null) {
					write(child);
					child = child.getNextSibling();
				}
				break;
			}

			case Node.ENTITY_REFERENCE_NODE: {
				if (fCanonical) {
					Node child = node.getFirstChild();
					while (child != null) {
						write(child);
						child = child.getNextSibling();
					}
				} else {
					fOut.print('&');
					fOut.print(node.getNodeName());
					fOut.print(';');
					fOut.flush();
				}
				break;
			}

			case Node.CDATA_SECTION_NODE: {
				if (fCanonical) {
					normalizeAndPrint(node.getNodeValue(), false);
				} else {
					fOut.print("<![CDATA[");
					fOut.print(node.getNodeValue());
					fOut.print("]]>");
				}
				fOut.flush();
				break;
			}

			case Node.TEXT_NODE: {
				normalizeAndPrint(node.getNodeValue(), false);
				fOut.flush();
				break;
			}

			case Node.PROCESSING_INSTRUCTION_NODE: {
				fOut.print("<?");
				fOut.print(node.getNodeName());
				String data = node.getNodeValue();
				if (data != null && data.length() > 0) {
					fOut.print(' ');
					fOut.print(data);
				}
				fOut.print("?>");
				fOut.flush();
				break;
			}

			case Node.COMMENT_NODE: {
				if (!fCanonical) {
					fOut.print("<!--");
					String comment = node.getNodeValue();
					if (comment != null && comment.length() > 0) {
						fOut.print(comment);
					}
					fOut.print("-->");
					fOut.flush();
				}
			}
		}

		if (type == Node.ELEMENT_NODE) {
			fOut.print("</");
			fOut.print(node.getNodeName());
			fOut.print('>');
			fOut.flush();
		}

	} // write(Node)

} // main(String[])
