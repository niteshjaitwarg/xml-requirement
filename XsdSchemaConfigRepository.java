package com.adp.esi.digitech.ds.config.repo;

import com.adp.esi.digitech.ds.config.entity.XsdSchemaConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for XsdSchemaConfigEntity. Provides DB access and lookup methods
 * for XSD configs.
 *
 * @author rhidau
 */
@Repository
public interface XsdSchemaConfigRepository extends JpaRepository<XsdSchemaConfigEntity, Long> {
	Optional<XsdSchemaConfigEntity> findBySourceKey(String sourceKey);

	boolean existsBySourceKey(String sourceKey);
}