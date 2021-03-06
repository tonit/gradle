/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.internal;

import groovy.lang.Closure;
import groovy.util.IndentPrinter;
import groovy.util.Node;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.maven.XmlProvider;
import org.gradle.util.SystemProperties;
import org.gradle.util.TextUtil;
import org.gradle.util.UncheckedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class XmlTransformer implements Transformer<String> {
    private final List<Action<? super XmlProvider>> actions = new ArrayList<Action<? super XmlProvider>>();
    private String indentation = "  ";

    public void addAction(Action<? super XmlProvider> provider) {
        actions.add(provider);
    }

    public void setIndentation(String indentation) {
        this.indentation = indentation;
    }

    public void addAction(Closure closure) {
        actions.add((Action<XmlProvider>) DefaultGroovyMethods.asType(closure, Action.class));
    }

    public String transform(String original) {
        return doTransform(original).toString();
    }

    public void transform(String original, Writer destination) {
        doTransform(original).writeTo(destination);
    }

    public void transform(String original, OutputStream destination) {
        doTransform(original).writeTo(destination);
    }

    public void transform(Node original, Writer destination) {
        doTransform(original).writeTo(destination);
    }

    public void transform(Node original, OutputStream destination) {
        doTransform(original).writeTo(destination);
    }

    private XmlProviderImpl doTransform(String original) {
        XmlProviderImpl provider = new XmlProviderImpl(original);
        provider.apply(actions);
        return provider;
    }

    private XmlProviderImpl doTransform(Node original) {
        XmlProviderImpl provider = new XmlProviderImpl(original);
        provider.apply(actions);
        return provider;
    }

    private class XmlProviderImpl implements XmlProvider {
        private StringBuilder builder;
        private Node node;
        private String stringValue;
        private Element element;

        public XmlProviderImpl(String original) {
            this.stringValue = original;
        }

        public XmlProviderImpl(Node original) {
            this.node = original;
        }

        public void apply(Iterable<Action<? super XmlProvider>> actions) {
            for (Action<? super XmlProvider> action : actions) {
                action.execute(this);
            }
        }

        @Override
        public String toString() {
            StringWriter writer = new StringWriter();
            writeTo(writer);
            return writer.toString();
        }

        public void writeTo(Writer writer) {
            doWriteTo(writer, null);
        }

        public void writeTo(OutputStream stream) {
            try {
                Writer writer = new OutputStreamWriter(stream, "UTF-8");
                doWriteTo(writer, "UTF-8");
                writer.flush();
            } catch (IOException e) {
                throw UncheckedException.asUncheckedException(e);
            }
        }

        public StringBuilder asString() {
            if (builder == null) {
                builder = new StringBuilder(toString());
                node = null;
                element = null;
            }
            return builder;
        }

        public Node asNode() {
            if (node == null) {
                try {
                    node = new XmlParser().parseText(toString());
                } catch (Exception e) {
                    throw UncheckedException.asUncheckedException(e);
                }
                builder = null;
                element = null;
            }
            return node;
        }

        public Element asElement() {
            if (element == null) {
                Document document;
                try {
                    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(toString())));
                } catch (Exception e) {
                    throw UncheckedException.asUncheckedException(e);
                }
                element = document.getDocumentElement();
                builder = null;
                node = null;
            }
            return element;
        }

        private void doWriteTo(Writer writer, String encoding) {
            writeXmlDeclaration(writer, encoding);

            try {
                if (node != null) {
                    final PrintWriter printWriter = new PrintWriter(writer);
                    IndentPrinter indentPrinter = new IndentPrinter(printWriter, indentation) {
                        @Override
                        public void println() {
                            printWriter.println();
                        }
                    };
                    XmlNodePrinter nodePrinter = new XmlNodePrinter(indentPrinter);
                    nodePrinter.setPreserveWhitespace(true);
                    nodePrinter.print(node);
                    printWriter.flush();
                } else if (element != null) {
                    printNode(element, writer);
                } else if (builder != null) {
                    writer.append(TextUtil.toPlatformLineSeparators(stripXmlDeclaration(builder)));
                } else {
                    writer.append(TextUtil.toPlatformLineSeparators(stripXmlDeclaration(stringValue)));
                }
            } catch (IOException e) {
                throw UncheckedException.asUncheckedException(e);
            }
        }

        private void printNode(org.w3c.dom.Node node, Writer destination) {
            removeEmptyTextNodes(node); // empty text nodes hinder subsequent formatting
            int indentAmount = determineIndentAmount();

            try {
                TransformerFactory factory = TransformerFactory.newInstance();
                try {
                    factory.setAttribute("indent-number", indentAmount);
                } catch (IllegalArgumentException ignored) {
                    /* unsupported by this transformer */
                }

                javax.xml.transform.Transformer transformer = factory.newTransformer();
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                try {
                    // some impls support this but not factory.setAttribute("indent-number")
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));
                } catch (IllegalArgumentException ignored) {
                    /* unsupported by this transformer */
                }

                transformer.transform(new DOMSource(node), new StreamResult(destination));
            } catch (TransformerException e) {
                throw UncheckedException.asUncheckedException(e);
            }
        }

        private int determineIndentAmount() {
            if (indentation.equals("\t")) { // not supported by javax.xml.transform.Transformer; use two spaces instead
                return 2;
            }
            return indentation.length(); // assume indentation uses spaces
        }

        private void removeEmptyTextNodes(org.w3c.dom.Node node) {
            org.w3c.dom.NodeList children = node.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE && child.getNodeValue().trim().length() == 0) {
                    node.removeChild(child);
                } else {
                    removeEmptyTextNodes(child);
                }
            }
        }

        private void writeXmlDeclaration(Writer writer, String encoding) {
            try {
                writer.write("<?xml version=\"1.0\"");
                if (encoding != null) {
                    writer.write(" encoding=\"");
                    writer.write(encoding);
                    writer.write("\"");
                }
                writer.write("?>");
                writer.write(SystemProperties.getLineSeparator());
            } catch (IOException e) {
                throw UncheckedException.asUncheckedException(e);
            }
        }
        private boolean hasXmlDeclaration(String xml) {
            return xml.startsWith("<?xml"); // XML declarations must be located at first position of first line
        }

        private String stripXmlDeclaration(CharSequence sequence) {
            String str = sequence.toString();
            if (hasXmlDeclaration(str)) {
                str = str.substring(str.indexOf("?>") + 2);
                str = StringUtils.stripStart(str, null);
            }
            return str;
        }
    }
}
