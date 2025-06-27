package com.adp.esi.digitech.ds.config.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Entity representing uploaded XSD schema configuration. Stores business
 * metadata and XSD JSON representations.
 *
 * @author rhidau
 */
@Entity
@Table(name = "xsd_schema_config", uniqueConstraints = @UniqueConstraint(columnNames = "sourceKey"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XsdSchemaConfigEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String bu;

	@Column(nullable = false)
	private String platform;

	@Column(nullable = false)
	private String dataCategory;

	@Column(nullable = false, unique = true)
	private String sourceKey;

	@Lob
	@Column(nullable = false, columnDefinition = "CLOB")
	private String originalXsdJson;

	@Lob
	@Column(columnDefinition = "CLOB")
	private String selectedXsdJson;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	@Column(nullable = false)
	@Builder.Default
	private String status = "ACTIVE"; // For future soft-delete/archival support

	@Column
	private String createdBy;

	@Column
	private String updatedBy;
}