package com.adp.esi.digitech.ds.config.service;

import com.adp.esi.digitech.ds.config.constants.ApiConstants;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigCreateRequest;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigDto;
import com.adp.esi.digitech.ds.config.dto.XsdSchemaConfigSelectRequest;
import com.adp.esi.digitech.ds.config.entity.XsdSchemaConfigEntity;
import com.adp.esi.digitech.ds.config.exception.ConfigurationException;
import com.adp.esi.digitech.ds.config.model.XsdNode;
import com.adp.esi.digitech.ds.config.repo.XsdSchemaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of XsdSchemaConfigService. Handles business logic for XSD
 * config CRUD, selection, overwriting and deletion.
 *
 * @author rhidau
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class XsdSchemaConfigServiceImpl implements XsdSchemaConfigService {

	private final XsdSchemaConfigRepository repository;
	private final XsdParseService xsdParseService;
	private final ModelMapper modelMapper;

	/**
	 * Uploads and parses an XSD file, storing originalXsdJson and metadata.
	 * Overwrites config with the same sourceKey if it exists.
	 *
	 * @param request   The create request DTO.
	 * @param userEmail The email of the user performing the action.
	 * @return The saved/updated config DTO.
	 */
	@Override
	@Transactional
	public XsdSchemaConfigDto uploadAndParseXsd(XsdSchemaConfigCreateRequest request, String userEmail) {
		log.info("Uploading and parsing XSD for sourceKey={}", request.getSourceKey());
		MultipartFile file = request.getFile();
		if (file == null || file.isEmpty()) {
			log.error(ApiConstants.FILE_EMPTY_ERROR);
			throw new ConfigurationException(ApiConstants.FILE_EMPTY_ERROR);
		}

		// Parse XSD file to tree and serialize to JSON
		XsdNode rootNode = xsdParseService.parseXsd(file);
		String originalJson = xsdParseService.serializeToJson(rootNode);

		// Overwrite config if same sourceKey exists
		XsdSchemaConfigEntity entity = repository.findBySourceKey(request.getSourceKey())
				.orElseGet(() -> XsdSchemaConfigEntity.builder().sourceKey(request.getSourceKey()).build());
		entity.setBu(request.getBu());
		entity.setPlatform(request.getPlatform());
		entity.setDataCategory(request.getDataCategory());
		entity.setOriginalXsdJson(originalJson);
		entity.setSelectedXsdJson(null); // Clear previous selection
		entity.setStatus("ACTIVE");
		entity.setUpdatedBy(userEmail);
		if (entity.getCreatedBy() == null)
			entity.setCreatedBy(userEmail);

		XsdSchemaConfigEntity saved = repository.save(entity);
		log.info("XSD Schema config saved for sourceKey={}, id={}", saved.getSourceKey(), saved.getId());
		return modelMapper.map(saved, XsdSchemaConfigDto.class);
	}

	/**
	 * Updates the selected XSD nodes for a configuration by sourceKey.
	 *
	 * @param request   The select request DTO.
	 * @param userEmail The email of the user performing the action.
	 * @return The updated config DTO.
	 */
	@Override
	@Transactional
	public XsdSchemaConfigDto updateSelectedTree(XsdSchemaConfigSelectRequest request, String userEmail) {
		log.info("Updating selected tree for sourceKey={}", request.getSourceKey());
		XsdSchemaConfigEntity entity = repository.findBySourceKey(request.getSourceKey())
				.orElseThrow(() -> new ConfigurationException(ApiConstants.SCHEMA_NOT_FOUND));
		entity.setSelectedXsdJson(request.getSelectedXsdJson());
		entity.setUpdatedBy(userEmail);
		XsdSchemaConfigEntity updated = repository.save(entity);
		log.info("Selected tree updated for sourceKey={}, id={}", updated.getSourceKey(), updated.getId());
		return modelMapper.map(updated, XsdSchemaConfigDto.class);
	}

	/**
	 * Lists all uploaded schema configs.
	 *
	 * @return List of config DTOs.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<XsdSchemaConfigDto> listAllSchemas() {
		log.info("Listing all XSD schema configs.");
		return repository.findAll().stream().map(e -> modelMapper.map(e, XsdSchemaConfigDto.class))
				.collect(Collectors.toList());
	}

	/**
	 * Fetches a single schema config by its ID.
	 *
	 * @param id The config ID.
	 * @return The config DTO.
	 */
	@Override
	@Transactional(readOnly = true)
	public XsdSchemaConfigDto getSchemaById(Long id) {
		log.info("Fetching schema config by id={}", id);
		XsdSchemaConfigEntity entity = repository.findById(id)
				.orElseThrow(() -> new ConfigurationException(ApiConstants.SCHEMA_NOT_FOUND));
		return modelMapper.map(entity, XsdSchemaConfigDto.class);
	}

	/**
	 * Deletes a schema config by ID (marks as DELETED, does not hard delete).
	 *
	 * @param id        The config ID.
	 * @param userEmail The user performing the delete.
	 */
	@Override
	@Transactional
	public void deleteSchema(Long id, String userEmail) {
		log.info("Deleting schema config by id={}", id);
		XsdSchemaConfigEntity entity = repository.findById(id)
				.orElseThrow(() -> new ConfigurationException(ApiConstants.SCHEMA_NOT_FOUND));
		entity.setStatus("DELETED");
		entity.setUpdatedBy(userEmail);
		repository.save(entity);
		log.info("Schema config soft-deleted for id={}", id);
	}
}