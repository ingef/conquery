package com.bakdata.conquery.models.identifiable.mapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.csv.CSV;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@CPSBase
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class IdMappingConfig {

	public PersistentIdMap generateIdMapping(CSV csvData) throws IOException, IllegalArgumentException {
		Iterator<String[]> csvIterator = csvData.iterateContent();

		PersistentIdMap mapping = new PersistentIdMap();


		if (!Arrays.equals(this.getHeader(), csvIterator.next(), StringUtils::compareIgnoreCase)) {
			throw new IllegalArgumentException("The uploaded CSVs Header does not match the expected");
		}

		// first column is the external key, the rest is part of the csv id
		csvIterator.forEachRemaining(
			(s) -> mapping.addMapping(
				new CsvEntityId(s[0]),
				new ExternalEntityId(Arrays.copyOfRange(s, 1, s.length)),
				getIdAccessors())
		);

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
	public String[] getPrintIdFields(){
		return ArrayUtils.subarray(getHeader(),1,getHeaderSize());
	}

	@JsonIgnore
	public abstract String[] getHeader();

	/**
	 * Is called once before a mapping is used before a query result is created
	 * to allow the mapping to have state information.
	 */
	public Map<String, Object> initToExternal(User user, ManagedExecution execution) {
		return Collections.emptyMap();
	}

	/**
	 * Converts the internal ID to the an external.
	 * 
	 * @param csvEntityId
	 * @param namespace
	 * @param state
	 * @return
	 */
	public ExternalEntityId toExternal(CsvEntityId csvEntityId, Namespace namespace, Map<String, Object> state) {
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
	public IdAccessor mappingFromCsvHeader(String[] csvHeader, NamespaceStorage namespaceStorage) {
		for (IdMappingAccessor accessor : getIdAccessors()) {
			if (accessor.canBeApplied(Arrays.asList(csvHeader))) {
				return accessor.getApplicationMapping(csvHeader, namespaceStorage);
			}
		}
		return DefaultIdAccessorImpl.INSTANCE;
	}
}
