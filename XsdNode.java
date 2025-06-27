package com.adp.esi.digitech.ds.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POJO for representing a node in XSD element tree. Each node is selectable for
 * UI tree rendering.
 * 
 * @author rhidau
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XsdNode {
	/** Name of the XML element */
	private String name;

	/** Type of the XML element (if present in XSD) */
	private String type;

	/** Attributes of the element (e.g., minOccurs, maxOccurs, etc.) */
	@Builder.Default
	private Map<String, String> attrs = new HashMap<>();

	/** Child nodes of the element */
	@Builder.Default
	private List<XsdNode> children = new ArrayList<>();

	/** Should the UI render this node as selectable (checkbox, etc.) */
	@Builder.Default
	private boolean selectable = true;
}