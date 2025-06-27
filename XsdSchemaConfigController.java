package com.adp.esi.digitech.ds.config.controller;

import com.adp.esi.digitech.ds.config.constants.ApiConstants;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigCreateRequest;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigDto;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigSelectRequest;
import com.adp.esi.digitech.ds.config.service.XsdSchemaConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for all XSD Schema Config APIs. Handles upload, update, list,
 * fetch, and delete operations.
 *
 * @author rhidau
 */
@RestController
@RequestMapping(ApiConstants.BASE_URL)
@RequiredArgsConstructor
@Slf4j
@Validated
public class XsdSchemaConfigController {

	private final XsdSchemaConfigService service;

	/**
	 * Upload and parse XSD, store config. Overwrites if sourceKey exists.
	 */
	@PostMapping(ApiConstants.UPLOAD)
	public ResponseEntity<XsdSchemaConfigDto> upload(@ModelAttribute @Valid XsdSchemaConfigCreateRequest request,
			@RequestHeader("X-User-Email") String userEmail) {
		log.info("API: Upload XSD for sourceKey={}", request.getSourceKey());
		return ResponseEntity.ok(service.uploadAndParseXsd(request, userEmail));
	}

	/**
	 * PATCH: Save selected tree for a schema.
	 */
	@PatchMapping(ApiConstants.SELECT)
	public ResponseEntity<XsdSchemaConfigDto> select(@RequestBody @Valid XsdSchemaConfigSelectRequest request,
			@RequestHeader("X-User-Email") String userEmail) {
		log.info("API: Save selected XSD nodes for sourceKey={}", request.getSourceKey());
		return ResponseEntity.ok(service.updateSelectedTree(request, userEmail));
	}

	/**
	 * List all uploaded schemas.
	 */
	@GetMapping(ApiConstants.SCHEMAS)
	public ResponseEntity<List<XsdSchemaConfigDto>> list() {
		log.info("API: List all uploaded schemas.");
		return ResponseEntity.ok(service.listAllSchemas());
	}

	/**
	 * Get one schema by ID.
	 */
	@GetMapping(ApiConstants.SCHEMA_BY_ID)
	public ResponseEntity<XsdSchemaConfigDto> getById(@PathVariable Long id) {
		log.info("API: Get schema by id={}", id);
		return ResponseEntity.ok(service.getSchemaById(id));
	}

	/**
	 * Delete a schema by ID (soft delete).
	 */
	@DeleteMapping(ApiConstants.SCHEMA_BY_ID)
	public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Email") String userEmail) {
		log.info("API: Delete schema by id={}", id);
		service.deleteSchema(id, userEmail);
		return ResponseEntity.noContent().build();
	}
}