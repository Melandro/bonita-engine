/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.io.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.bind.ValidationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 */
public class XMLHandler {

    private static final String ENCODING = "UTF-8";

    private final List<Class<? extends ElementBinding>> bindings;

    private final SchemaFactory factory;

    private Schema schema;

    private final DocumentBuilder documentBuilder;

    private final Transformer transformer;

    public XMLHandler(final List<Class<? extends ElementBinding>> bindings) throws ParserConfigurationException, TransformerConfigurationException {
        this.bindings = bindings;
        factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setValidating(true);
        // ignore white space can only be set if parser is validating
        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        // select xml schema as the schema language (a.o.t. DTD)
        documentBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING);
    }

    private Node addNode(final Document document, final Node parentNode, final XMLNode node) {
        final String name = node.getName();
        final Map<String, String> attributes = node.getAttributes();
        final String content = node.getContent();
        final Element element = document.createElement(name);
        if (content != null) {
            element.setTextContent(content);
        }
        if (attributes != null) {
            for (final Entry<String, String> attribute : attributes.entrySet()) {
                final String attributeName = attribute.getKey();
                final String attributeValue = attribute.getValue();
                if (attributeValue != null) {
                    element.setAttribute(attributeName, attributeValue);
                }
            }
        }
        for (final XMLNode xmlNode : node.getChildNodes()) {
            final Node child = addNode(document, element, xmlNode);
            element.appendChild(child);
        }
        if (parentNode == null) {
            return element;
        } else {
            return parentNode.appendChild(element);
        }
    }

    private Document getDocument(final XMLNode rootNode) {
        final Document document = documentBuilder.newDocument();
        document.setXmlVersion("1.0");
        document.setXmlStandalone(true);
        final Node root = addNode(document, null, rootNode);
        document.appendChild(root);
        return document;
    }

    public Object getObjectFromXML(final File file) throws XMLParseException, FileNotFoundException, IOException {
        final InputStreamReader isr = new InputStreamReader(new FileInputStream(file), ENCODING);
        Object objectFromXML = null;
        try {
            objectFromXML = getObjectFromXML(isr);
        } finally {
            isr.close();
        }
        return objectFromXML;
    }

    public Object getObjectFromXML(final Reader xmlReader) throws IOException, XMLParseException {
        final BindingHandler handler;
        handler = new BindingHandler(bindings);
        final XMLParserErrorHandler errorhandler = new XMLParserErrorHandler();
        try {
            final XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(errorhandler);
            final InputSource xmlInputSource = new InputSource(xmlReader);
            reader.parse(xmlInputSource);
            return handler.getModel();
        } catch (final SAXException e) {
            throw new XMLParseException(e);
        }
    }

    public void setSchema(final InputStream schemaStream) throws InvalidSchemaException {
        try {
            schema = factory.newSchema(new StreamSource(schemaStream));
        } catch (final SAXException e) {
            throw new InvalidSchemaException(e);
        }
    }

    public void validate(final File file) throws ValidationException, IOException {
        validate(new StreamSource(file));
    }

    private void validate(final StreamSource source) throws ValidationException, IOException {
        try {
            if (schema == null) {
                throw new ValidationException("No schema defined");
            }
            final Validator validator = schema.newValidator();
            validator.validate(source);
        } catch (final SAXException e) {
            throw new ValidationException(e);
        }
    }

    public void validate(final Reader content) throws ValidationException, IOException {
        validate(new StreamSource(content));
    }

    public byte[] write(final XMLNode rootNode) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] xmlContent = null;
        try {
            this.write(rootNode, outputStream);
            xmlContent = outputStream.toByteArray();
        } catch (final IOException e) {
        } finally {
            try {
                outputStream.close();
            } catch (final IOException e) {
            }
        }
        return xmlContent;
    }

    public void write(final XMLNode rootNode, final OutputStream outputStream) throws IOException {
        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(outputStream, ENCODING);
            this.write(rootNode, osw);
        } finally {
            if (osw != null) {
                osw.close();
            }
        }
    }

    private void write(final XMLNode rootNode, final StreamResult result) throws TransformerException {
        final Document document = getDocument(rootNode);
        final Source source = new DOMSource(document);
        transformer.transform(source, result);
    }

    public void write(final XMLNode rootNode, final Writer writer) throws IOException {
        try {
            final StreamResult sr = new StreamResult(writer);
            this.write(rootNode, sr);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

}
