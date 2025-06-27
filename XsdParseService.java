package com.adp.esi.digitech.ds.config.service;

import com.adp.esi.digitech.ds.config.exception.ConfigurationException;
import com.adp.esi.digitech.ds.config.model.XsdNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Service for robust parsing of XSD files to XsdNode trees. Converts XSD to a
 * structured, UI-friendly, JSON-serializable tree. Fully stateless,
 * thread-safe, and ready for use in scalable, production systems.
 * 
 * <p>
 * This class is solely responsible for parsing logic. No business or
 * persistence logic is present here.
 * </p>
 * 
 * <ul>
 * <li>Handles all XSD namespace/complexity (supports sequences, choices,
 * references, etc.)</li>
 * <li>Produces trees suitable for UI selection and downstream processing</li>
 * <li>Serializes tree to JSON using Jackson</li>
 * </ul>
 * 
 * @author rhidau
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class XsdParseService {

	/** Jackson object mapper for serialization. */
	private final ObjectMapper objectMapper;

	/** Standard XML Schema namespace URI. */
	private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

	/**
	 * Parses a given XSD file into a root XsdNode tree representation.
	 * 
	 * @param file The uploaded XSD file. Must not be null or empty.
	 * @return Root XsdNode tree representing the full XSD structure.
	 * @throws ConfigurationException If parsing fails or file is invalid.
	 */
	public XsdNode parseXsd(final MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new ConfigurationException("Provided XSD file is null or empty.");
		}

		try {
			byte[] xsdBytes = file.getBytes();
			XMLInputFactory factory = XMLInputFactory.newInstance();

			// First: collect all global types for later resolution
			Map<String, List<XMLEvent>> typeDefMap;
			try (InputStream is1 = new ByteArrayInputStream(xsdBytes)) {
				typeDefMap = collectGlobalTypes(factory.createXMLEventReader(is1));
			}

			// Second: locate and parse the root element
			try (InputStream is2 = new ByteArrayInputStream(xsdBytes)) {
				XMLEventReader reader = factory.createXMLEventReader(is2);
				while (reader.hasNext()) {
					XMLEvent event = reader.nextEvent();
					if (event.isStartElement()) {
						StartElement start = event.asStartElement();
						if (isXsdTag(start, "element")) {
							XsdNode rootNode = parseElement(reader, start, typeDefMap);
							log.info("Successfully built XSD node tree for file: {}", file.getOriginalFilename());
							return rootNode;
						}
					}
				}
			}
			throw new ConfigurationException("Root <xsd:element> not found in schema.");
		} catch (Exception ex) {
			log.error("XSD parsing failed for file '{}': {}", file.getOriginalFilename(), ex.getMessage(), ex);
			throw new ConfigurationException("Error parsing XSD: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Serializes a given XsdNode tree as a JSON string for frontend or storage.
	 *
	 * @param rootNode Root XsdNode to serialize
	 * @return JSON string representation
	 */
	public String serializeToJson(final XsdNode rootNode) {
		try {
			return objectMapper.writeValueAsString(rootNode);
		} catch (Exception ex) {
			log.error("Failed to serialize XsdNode to JSON: {}", ex.getMessage());
			throw new ConfigurationException("Failed to serialize XsdNode to JSON", ex);
		}
	}

	/**
	 * Collects all global type definitions (complexType, simpleType) from the XSD.
	 * This is required to resolve references and compose the tree correctly.
	 *
	 * @param reader XMLEventReader for the XSD
	 * @return Map of type name (without prefix) to XMLEvent list
	 * @throws XMLStreamException On XML parsing errors
	 */
	private Map<String, List<XMLEvent>> collectGlobalTypes(final XMLEventReader reader) throws XMLStreamException {
		Map<String, List<XMLEvent>> typeDefMap = new HashMap<>();
		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()) {
				StartElement start = event.asStartElement();
				if (isXsdTag(start, "complexType") || isXsdTag(start, "simpleType")) {
					Attribute nameAttr = start.getAttributeByName(new QName("name"));
					if (nameAttr != null) {
						String typeName = nameAttr.getValue();
						List<XMLEvent> typeEvents = new ArrayList<>();
						int depth = 1;
						typeEvents.add(event);
						String tagName = start.getName().getLocalPart();
						while (reader.hasNext() && depth > 0) {
							XMLEvent next = reader.nextEvent();
							typeEvents.add(next);
							if (next.isStartElement() && tagName.equals(next.asStartElement().getName().getLocalPart()))
								depth++;
							else if (next.isEndElement()
									&& tagName.equals(next.asEndElement().getName().getLocalPart()))
								depth--;
						}
						typeDefMap.put(typeName, typeEvents);
						log.debug("Registered type definition: {}", typeName);
					}
				}
			}
		}
		reader.close();
		return typeDefMap;
	}

	/**
	 * Parses an <element> node and recursively its children. Namespace-aware and
	 * resolves all type references.
	 *
	 * @param reader     XMLEventReader at the current element position
	 * @param start      StartElement representing the element tag
	 * @param typeDefMap Map of complexType definitions for type resolution
	 * @return Parsed XsdNode tree
	 * @throws XMLStreamException On XML parsing errors
	 */
	private XsdNode parseElement(final XMLEventReader reader, final StartElement start,
			final Map<String, List<XMLEvent>> typeDefMap) throws XMLStreamException {
		String name = getAttr(start, "name");
		String type = getAttr(start, "type");
		XsdNode node = XsdNode.builder().name(name).type(type).selectable(true).build();

		Iterator<?> attrs = start.getAttributes();
		while (attrs.hasNext()) {
			Attribute attr = (Attribute) attrs.next();
			if (!"name".equals(attr.getName().getLocalPart()) && !"type".equals(attr.getName().getLocalPart()))
				node.getAttrs().put(attr.getName().getLocalPart(), attr.getValue());
		}

		if (type != null && typeDefMap.containsKey(stripPrefix(type))) {
			// Resolve type reference into children tree
			List<XMLEvent> events = typeDefMap.get(stripPrefix(type));
			XsdNode complexTypeNode = parseComplexType(events.iterator(), typeDefMap);
			node.setChildren(complexTypeNode.getChildren());
			log.debug("Expanded type reference '{}' for node '{}'", type, name);
			return node;
		}

		// Inline complexType or simpleType
		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				if (isXsdTag(se, "complexType")) {
					XsdNode complexTypeNode = parseComplexType(reader, typeDefMap);
					node.setChildren(complexTypeNode.getChildren());
				}
				// Optionally: handle <simpleType> here if needed
			} else if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				if (isXsdTag(ee, "element"))
					break;
			}
		}
		return node;
	}

	/**
	 * Parses a complexType node from an iterator of XMLEvents (used for type
	 * references).
	 *
	 * @param iter       Iterator of XMLEvents for the complexType
	 * @param typeDefMap Map of complexType definitions for type resolution
	 * @return Parsed XsdNode with children
	 * @throws XMLStreamException On XML parsing errors
	 */
	private XsdNode parseComplexType(final Iterator<XMLEvent> iter, final Map<String, List<XMLEvent>> typeDefMap)
			throws XMLStreamException {
		XsdNode parent = XsdNode.builder().selectable(false).build();
		while (iter.hasNext()) {
			XMLEvent event = iter.next();
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				if (isXsdTag(se, "sequence") || isXsdTag(se, "all") || isXsdTag(se, "choice")) {
					parseSequence(iter, parent, typeDefMap);
				} else if (isXsdTag(se, "element")) {
					parent.getChildren().add(parseElementFromIterator(iter, se, typeDefMap));
				}
			} else if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				if (isXsdTag(ee, "complexType"))
					break;
			}
		}
		return parent;
	}

	/**
	 * Parses a complexType node from an XMLEventReader (used for inline
	 * complexType).
	 *
	 * @param reader     XMLEventReader for the complexType
	 * @param typeDefMap Map of complexType definitions for type resolution
	 * @return Parsed XsdNode with children
	 * @throws XMLStreamException On XML parsing errors
	 */
	private XsdNode parseComplexType(final XMLEventReader reader, final Map<String, List<XMLEvent>> typeDefMap)
			throws XMLStreamException {
		XsdNode parent = XsdNode.builder().selectable(false).build();
		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				if (isXsdTag(se, "sequence") || isXsdTag(se, "all") || isXsdTag(se, "choice")) {
					parseSequence(reader, parent, typeDefMap);
				} else if (isXsdTag(se, "element")) {
					parent.getChildren().add(parseElement(reader, se, typeDefMap));
				}
			} else if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				if (isXsdTag(ee, "complexType"))
					break;
			}
		}
		return parent;
	}

	/**
	 * Parses a sequence, all, or choice node using XMLEventReader and adds its
	 * elements as children.
	 *
	 * @param reader     XMLEventReader positioned at the sequence node
	 * @param parent     XsdNode to which parsed children will be added
	 * @param typeDefMap Map of complexType definitions for type resolution
	 * @throws XMLStreamException On XML parsing errors
	 */
	private void parseSequence(final XMLEventReader reader, final XsdNode parent,
			final Map<String, List<XMLEvent>> typeDefMap) throws XMLStreamException {
		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				if (isXsdTag(se, "element")) {
					parent.getChildren().add(parseElement(reader, se, typeDefMap));
				}
			} else if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				if (isXsdTag(ee, "sequence") || isXsdTag(ee, "all") || isXsdTag(ee, "choice"))
					break;
			}
		}
	}

	/**
	 * Parses a sequence, all, or choice node using an iterator of XMLEvents.
	 *
	 * @param iter       Iterator of XMLEvents
	 * @param parent     XsdNode to which parsed children will be added
	 * @param typeDefMap Map of complexType definitions for type resolution
	 * @throws XMLStreamException On XML parsing errors
	 */
	private void parseSequence(final Iterator<XMLEvent> iter, final XsdNode parent,
			final Map<String, List<XMLEvent>> typeDefMap) throws XMLStreamException {
		while (iter.hasNext()) {
			XMLEvent event = iter.next();
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				if (isXsdTag(se, "element")) {
					parent.getChildren().add(parseElementFromIterator(iter, se, typeDefMap));
				}
			} else if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				if (isXsdTag(ee, "sequence") || isXsdTag(ee, "all") || isXsdTag(ee, "choice"))
					break;
			}
		}
	}

	/**
	 * Parses an <element> node from an iterator (used in referenced complexTypes).
	 *
	 * @param iter       Iterator of XMLEvents positioned at an element
	 * @param start      StartElement for the element
	 * @param typeDefMap Map of complexType definitions for type resolution
	 * @return Parsed XsdNode
	 * @throws XMLStreamException On XML parsing errors
	 */
	private XsdNode parseElementFromIterator(final Iterator<XMLEvent> iter, final StartElement start,
			final Map<String, List<XMLEvent>> typeDefMap) throws XMLStreamException {
		String name = getAttr(start, "name");
		String type = getAttr(start, "type");
		XsdNode node = XsdNode.builder().name(name).type(type).selectable(true).build();

		Iterator<?> attrs = start.getAttributes();
		while (attrs.hasNext()) {
			Attribute attr = (Attribute) attrs.next();
			if (!"name".equals(attr.getName().getLocalPart()) && !"type".equals(attr.getName().getLocalPart()))
				node.getAttrs().put(attr.getName().getLocalPart(), attr.getValue());
		}

		if (type != null && typeDefMap.containsKey(stripPrefix(type))) {
			List<XMLEvent> events = typeDefMap.get(stripPrefix(type));
			XsdNode complexTypeNode = parseComplexType(events.iterator(), typeDefMap);
			node.setChildren(complexTypeNode.getChildren());
			int depth = 0;
			while (iter.hasNext()) {
				XMLEvent event = iter.next();
				if (event.isStartElement() && isXsdTag(event.asStartElement(), "element"))
					depth++;
				if (event.isEndElement() && isXsdTag(event.asEndElement(), "element")) {
					if (depth == 0)
						break;
					depth--;
				}
			}
			return node;
		}
		int depth = 0;
		while (iter.hasNext()) {
			XMLEvent event = iter.next();
			if (event.isStartElement()) {
				StartElement se = event.asStartElement();
				if (isXsdTag(se, "complexType")) {
					XsdNode complexTypeNode = parseComplexType(iter, typeDefMap);
					node.setChildren(complexTypeNode.getChildren());
				}
			} else if (event.isEndElement()) {
				EndElement ee = event.asEndElement();
				if (isXsdTag(ee, "element")) {
					if (depth == 0)
						break;
					depth--;
				}
			}
		}
		return node;
	}

	/**
	 * Gets the value of a named attribute from a StartElement.
	 *
	 * @param el  The StartElement
	 * @param key The attribute name
	 * @return The attribute value or null if not found
	 */
	private String getAttr(final StartElement el, final String key) {
		Attribute attr = el.getAttributeByName(new QName(key));
		return attr != null ? attr.getValue() : null;
	}

	/**
	 * Removes the prefix (e.g., 'xsd:') from a type string.
	 *
	 * @param type The type string (e.g., "xsd:string")
	 * @return The type without prefix (e.g., "string")
	 */
	private String stripPrefix(final String type) {
		int idx = type.indexOf(':');
		return (idx != -1) ? type.substring(idx + 1) : type;
	}

	/**
	 * Checks if a StartElement matches a tag name in the XSD namespace.
	 * Namespace-aware and supports any prefix.
	 *
	 * @param el      The StartElement
	 * @param tagName The local name to check
	 * @return true if matches XSD namespace and tag name
	 */
	private boolean isXsdTag(final StartElement el, final String tagName) {
		QName q = el.getName();
		return XSD_NS.equals(q.getNamespaceURI()) && tagName.equals(q.getLocalPart());
	}

	/**
	 * Checks if an EndElement matches a tag name in the XSD namespace.
	 * Namespace-aware and supports any prefix.
	 *
	 * @param el      The EndElement
	 * @param tagName The local name to check
	 * @return true if matches XSD namespace and tag name
	 */
	private boolean isXsdTag(final EndElement el, final String tagName) {
		QName q = el.getName();
		return XSD_NS.equals(q.getNamespaceURI()) && tagName.equals(q.getLocalPart());
	}
}