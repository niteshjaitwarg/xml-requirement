package com.adp.esi.digitech.ds.config.controller;

import com.adp.esi.digitech.ds.config.Constants;
import com.adp.esi.digitech.ds.config.exception.ConfigurationException;
import com.adp.esi.digitech.ds.config.model.ApiResponse;
import com.adp.esi.digitech.ds.config.model.ErrorResponse;
import com.adp.esi.digitech.ds.config.model.XsdNode;
import com.adp.esi.digitech.ds.config.service.XsdParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller responsible for handling XSD file upload and parsing.
 *
 * @author rhidau
 */
@RestController
@RequestMapping(Constants.BASE_URL + Constants.XSD_PARSE)
@RequiredArgsConstructor
@Slf4j
public class XsdParseController {

	private final XsdParseService xsdParseService;

	/**
	 * Endpoint to handle upload and parsing of XSD files.
	 *
	 * @param file the XSD file to parse
	 * @return ResponseEntity containing parsed {@link XsdNode} tree or error
	 *         response
	 */
	@PostMapping(Constants.XSD_UPLOAD)
	public ResponseEntity<ApiResponse<XsdNode>> parseXsdFile(@RequestParam("xsdfile") MultipartFile file) {
		log.info("Received XSD file upload: name={}, size={} bytes", file.getOriginalFilename(), file.getSize());

		// Validate input
		if (file == null || file.isEmpty()) {
			log.warn("Upload failed: No file uploaded or file is empty.");
			ErrorResponse error = new ErrorResponse(Constants.BAD_REQUEST, Constants.FILE_REQUIRED_ERROR);
			return ResponseEntity.badRequest().body(ApiResponse.error(ApiResponse.Status.ERROR, error));
		}

		String fileName = file.getOriginalFilename();
		String contentType = file.getContentType();

		boolean invalidFile = fileName == null || !fileName.toLowerCase().endsWith(".xsd");
		boolean invalidType = contentType != null && !Constants.XML_CONTENT_TYPE.equalsIgnoreCase(contentType)
				&& !Constants.TEXT_XML_CONTENT_TYPE.equalsIgnoreCase(contentType);

		if (invalidFile || invalidType) {
			log.warn("Upload failed: Unsupported file type - {}", fileName);
			ErrorResponse error = new ErrorResponse(Constants.UNSUPPORTED_MEDIA_TYPE,
					Constants.UNSUPPORTED_FILE_TYPE_ERROR);
			return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
					.body(ApiResponse.error(ApiResponse.Status.ERROR, error));
		}

		try {
			XsdNode rootNode = xsdParseService.parseXsd(file);
			log.info("XSD parsed successfully. Root node: {}", rootNode != null ? rootNode.getName() : "null");
			return ResponseEntity.ok(ApiResponse.success(ApiResponse.Status.SUCCESS, rootNode));
		} catch (ConfigurationException ce) {
			log.error("Known error occurred while parsing XSD: {}", ce.getMessage(), ce);
			ErrorResponse error = ce.getErrors() != null && !ce.getErrors().isEmpty()
					? new ErrorResponse(Constants.UNPROCESSABLE_ENTITY, ce.getMessage(), ce.getErrors())
					: new ErrorResponse(Constants.UNPROCESSABLE_ENTITY, ce.getMessage());
			return ResponseEntity.unprocessableEntity().body(ApiResponse.error(ApiResponse.Status.ERROR, error));
		} catch (Exception ex) {
			log.error("Unexpected error while parsing XSD: {}", ex.getMessage(), ex);
			ErrorResponse error = new ErrorResponse(Constants.INTERNAL_SERVER_ERROR,
					Constants.INTERNAL_ERROR_MSG + ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ApiResponse.error(ApiResponse.Status.ERROR, error));
		}
	}
}
