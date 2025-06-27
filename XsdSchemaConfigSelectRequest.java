package com.adp.esi.digitech.ds.config.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for saving/updating selected XSD nodes. Used in PATCH requests.
 *
 * @author rhidau
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XsdSchemaConfigSelectRequest {

	@NotBlank
	private String sourceKey;

	@NotBlank
	private String selectedXsdJson;
}