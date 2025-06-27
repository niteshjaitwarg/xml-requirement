package com.adp.esi.digitech.ds.config.service;

import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigCreateRequest;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigDto;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigSelectRequest;

import java.util.List;

/**
 * Service interface for XSD Schema configuration business logic. Enforces
 * contract for all operations.
 *
 * @author rhidau
 */
public interface XsdSchemaConfigService {

	XsdSchemaConfigDto uploadAndParseXsd(XsdSchemaConfigCreateRequest request, String userEmail);

	XsdSchemaConfigDto updateSelectedTree(XsdSchemaConfigSelectRequest request, String userEmail);

	List<XsdSchemaConfigDto> listAllSchemas();

	XsdSchemaConfigDto getSchemaById(Long id);

	void deleteSchema(Long id, String userEmail);
}