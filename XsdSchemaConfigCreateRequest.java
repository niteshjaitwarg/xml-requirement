package com.adp.esi.digitech.ds.config.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating/uploading a new XSD schema config. Used in
 * multipart/form-data POST requests.
 *
 * @author rhidau
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XsdSchemaConfigCreateRequest {

	@NotBlank
	private String bu;

	@NotBlank
	private String platform;

	@NotBlank
	private String dataCategory;

	@NotBlank
	private String sourceKey;

	@NotNull
	private MultipartFile file;
}