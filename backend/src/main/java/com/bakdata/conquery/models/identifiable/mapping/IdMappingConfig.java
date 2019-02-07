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

		PersistingIdMap mapping = namespace.getStorage().getIdMapping();

		if (!Streams.zip(this.getHeader().stream(), Arrays.stream(csvIterator.next()), String::equalsIgnoreCase).allMatch(t -> t)) {
			throw new IllegalArgumentException("The uploaded CSVs Header does not match the expected");
		}

		csvIterator.forEachRemaining(
			// first column is the external key, the rest is part of the print key
			(s)-> mapping.getCsvIdToExternalIdMap().put(new CsvId(s[0]), new ExternalId(Arrays.copyOfRange(s,1,s.length)))
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
	abstract public List<IdMappingAccessor> getIdAccessors();

	@JsonIgnore
	abstract public List<String> getPrintIdFields();

	@JsonIgnore
	abstract public List<String> getHeader();

	public ExternalId toExternal(CsvId csvId, Namespace namespace) {
		PersistingIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null){
			return mapping.getCsvIdToExternalIdMap().get(csvId);
		}
		else {
			return ExternalId.fromCsvId(csvId);
		}

	}

	@NonNull
	public IdAccessor mappingFromCsvHeader(String[] csvHeader, NamespaceStorage namespaceStorage) {
		for (IdMappingAccessor accessor : getIdAccessors()) {
			if (accessor.canBeApplied(Arrays.asList(csvHeader))) {
				return accessor.getApplicationMapping(Arrays.asList(csvHeader), namespaceStorage);
			}
		}
		return new DefaultIdAccessor();
	}


	/**
	 * Checks if the given CsvContent produces unique results in perspective to all IdMappinAccessors.
	 * @param data Map of CsvId to External Ids as read from the given CSV
	 */
	private void checkIntegrity(Map<CsvId,ExternalId> data) {
		// check that each idMappingAccessor leads to at most one tuple
		for (IdMappingAccessor idMappingAccessor : getIdAccessors()) {
			long distinctSize = data.values().stream().map(p -> idMappingAccessor.extract(p.getExternalId())).distinct().count();
			// check if we still have the same size as before
			if (distinctSize != data.size()) {
				throw new IllegalArgumentException("The inserted print ids are not unique respective to the idMapping Accessor "
					+ idMappingAccessor);
			}
		}
	}
}

