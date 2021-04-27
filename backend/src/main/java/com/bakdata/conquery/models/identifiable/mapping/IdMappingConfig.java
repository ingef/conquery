package com.bakdata.conquery.models.identifiable.mapping;

import java.util.*;
import java.util.function.Function;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


@Slf4j
@CPSBase
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class IdMappingConfig {

	public PersistentIdMap generateIdMapping(Iterator<String[]> csvIterator) throws IllegalArgumentException {

		PersistentIdMap mapping = new PersistentIdMap();

		if (!Arrays.equals(this.getHeader(), csvIterator.next(), StringUtils::compareIgnoreCase)) {
			throw new IllegalArgumentException("The uploaded CSVs Header does not match the expected");
		}

		// first column is the external key, the rest is part of the csv id
		csvIterator.forEachRemaining(
			(s) -> mapping.addMapping(new CsvEntityId(s[0]), new ExternalEntityId(Arrays.copyOfRange(s, 1, s.length)), getIdAccessors()));

		mapping.checkIntegrity(Arrays.asList(getIdAccessors()));

		return mapping;
	}

	@JsonIgnore
	public int getHeaderSize() {
		return getHeader().length;
	}

	@JsonIgnore
	public abstract IdMappingAccessor[] getIdAccessors();

	@JsonIgnore
	public List<String> getPrintIdFields() {
		return List.of(ArrayUtils.subarray(getHeader(), 1, getHeaderSize()));
	}

	/**
	 * Header of the Mapping-CSV file.
	 */
	@JsonIgnore
	public abstract String[] getHeader();

	/**
	 * Is called once before a mapping is used before a query result is created to
	 * allow the mapping to have state information.
	 */
	public IdMappingState initToExternal(User user, ManagedExecution<?> execution) {
		// This mapping does not need a per-query state, so we return an immutable empty map.
		return null;
	}

	/**
	 * Converts the internal ID to the an external.
	 */
	public ExternalEntityId toExternal(CsvEntityId csvEntityId, Namespace namespace, IdMappingState state) {
		// The state may be uses by implementations of this class
		PersistentIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null) {
			ExternalEntityId externalEntityId = mapping.toExternal(csvEntityId);
			if (externalEntityId != null) {
				return externalEntityId;
			}
		}
		return ExternalEntityId.from(csvEntityId);
	}

	@NonNull
	public IdAccessor mappingFromCsvHeader(String[] csvHeader, PersistentIdMap idMapping) {
		for (IdMappingAccessor accessor : getIdAccessors()) {
			if (accessor.canBeApplied(Arrays.asList(csvHeader))) {
				log.info("Using accessor (with required headers {}) to extract mapping from CSV with the header containing the ID columns: {}", accessor.getHeader(), csvHeader);
				return accessor.getApplicationMapping(csvHeader, idMapping);
			}
		}
		log.info("Using the default accessor implementation.");
		return DefaultIdAccessorImpl.INSTANCE;
	}


}
