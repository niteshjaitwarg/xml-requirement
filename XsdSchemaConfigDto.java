package com.adp.esi.digitech.ds.config.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO for presenting XSD schema configuration data. Used for API responses.
 *
 * @author rhidau
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XsdSchemaConfigDto {

	private Long id;
	private String bu;
	private String platform;
	private String dataCategory;
	private String sourceKey;
	private String originalXsdJson;
	private String selectedXsdJson;
	private Instant createdAt;
	private Instant updatedAt;
	private String status;
}