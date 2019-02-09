package com.bakdata.conquery.models.identifiable.mapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.csv.CSV;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Streams;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@CPSBase
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class IdMappingConfig {

	public void updateCsvData(CSV csvData, Namespace namespace) throws IOException, JSONException {
		Iterator<String[]> csvIterator = csvData.iterateContent(null);

		PersistentIdMap mapping = namespace.getStorage().getIdMapping();

		if (!Streams.zip(this.getHeader().stream(), Arrays.stream(csvIterator.next()), String::equalsIgnoreCase).allMatch(t -> t)) {
			throw new IllegalArgumentException("The uploaded CSVs Header does not match the expected");
		}

		csvIterator.forEachRemaining(
			// first column is the external key, the rest is part of the print key
			(s)-> mapping.getCsvIdToExternalIdMap().put(new CsvEntityId(s[0]), new ExternalEntityId(Arrays.copyOfRange(s,1,s.length)))
		);

		checkIntegrity(mapping.getCsvIdToExternalIdMap());
		for (IdMappingAccessor accessor : getIdAccessors()) {
			accessor.updateMapping(namespace.getStorage().getIdMapping());
		}
		namespace.getStorage().updateIdMapping(mapping);
	}

	@JsonIgnore
	public Integer getHeaderSize() {
		return getHeader().size();
	}

	@JsonIgnore
	public abstract List<IdMappingAccessor> getIdAccessors();

	@JsonIgnore
	public abstract List<String> getPrintIdFields();

	@JsonIgnore
	public abstract List<String> getHeader();

	public ExternalEntityId toExternal(CsvEntityId csvEntityId, Namespace namespace) {
		PersistentIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null){
			return mapping.getCsvIdToExternalIdMap().get(csvEntityId);
		}
		else {
			return ExternalEntityId.from(csvEntityId);
		}

	}

	@NonNull
	public IdAccessor mappingFromCsvHeader(String[] csvHeader, NamespaceStorage namespaceStorage) {
		for (IdMappingAccessor accessor : getIdAccessors()) {
			if (accessor.canBeApplied(Arrays.asList(csvHeader))) {
				return accessor.getApplicationMapping(csvHeader, namespaceStorage);
			}
		}
		return new DefaultIdAccessorImpl();
	}


	/**
	 * Checks if the given CsvContent produces unique results in perspective to all IdMappingAccessors.
	 * @param data Map of CsvEntityId to External Ids as read from the given CSV.
	 * @throws IllegalArgumentException if the inserted Ids are not unique.
	 */
	private void checkIntegrity(Map<CsvEntityId, ExternalEntityId> data) {
		// check that each idMappingAccessor leads to at most one tuple
		for (IdMappingAccessor idMappingAccessor : getIdAccessors()) {
			long distinctSize = data.values().stream().map(p -> idMappingAccessor.extract(p.getExternalId())).distinct().count();
			// check if we still have the same size as before
			if (distinctSize != data.size()) {
				throw new IllegalArgumentException("The inserted IDs are not unique respective to the idMapping Accessor "
					+ idMappingAccessor);
			}
		}
	}
}
