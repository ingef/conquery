package com.bakdata.conquery.models.identifiable.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.csv.CSV;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Streams;

import lombok.RequiredArgsConstructor;

@CPSBase
@RequiredArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class IdMappingConfig {

	public void updateCsvData(CSV csvData, Namespace namespace) throws IOException, JSONException {
		List<Pair<String, List<String>>> data = new ArrayList<>();
		Iterator<String[]> csvIterator = csvData.iterateContent(null);

		// Ensure header matches the expected Header
		if (!Streams.zip(this.getHeader().stream(), Arrays.stream(csvIterator.next()), String::equalsIgnoreCase).allMatch(t -> t)) {
			throw new IllegalArgumentException("The uploaded CSVs Header does not match the expected");
		}

		// first column is the external key, the rest is part of the print key
		csvIterator.forEachRemaining(s -> data.add(Pair.of(s[0], Arrays.asList(Arrays.copyOfRange(s, 1, getHeaderSize())))));
		checkIntegrity(data);
		Map mapping = namespace.getStorage().getIdMapping();
		for (Pair<String, List<String>> entry : data) {
			mapping.put(entry.getKey(), entry.getValue());
		}

		namespace.getStorage().updateIdMapping(mapping);

	}

	@JsonIgnore
	abstract public List<IdMappingAccessor> getIdAccessors();

	@JsonIgnore
	abstract public List<String> getPrintIdFields();

	@JsonIgnore
	abstract public List<String> getHeader();

	public String[] toExternal(String csvId, Namespace namespace) {
		return namespace.getStorage().getIdMapping().get(csvId).toArray(new String[0]);
	}

	public IdAccessor mappingFromCsvHeader(String[] csvHeader) {
		for (IdMappingAccessor accessor : getIdAccessors()) {
			if (accessor.canBeApplied(Arrays.asList(csvHeader))) {
				return accessor.getApplicationMapping(Arrays.asList(csvHeader));
			}
		}
		return new DefaultIdAccessor();
	}

	private void checkIntegrity(List<Pair<String, List<String>>> data) {
		// check if all csv id's are unique
		long distinctSize = data.stream().map(Pair::getKey).distinct().count();
		if (distinctSize != data.size()) {
			throw new IllegalArgumentException("The inserted csv ids are not unique");
		}

		// check that each idMappingAccessor leads to at most one tuple
		for (IdMappingAccessor idMappingAccessor : getIdAccessors()) {
			distinctSize = data.stream().map(p -> idMappingAccessor.extract(p.getValue())).distinct().count();
			// check if we still have the same size as before
			if (distinctSize != data.size()) {
				throw new IllegalArgumentException("The inserted print ids are not unique respective to the idMapping Accessor "
					+ idMappingAccessor);
			}
		}
	}

	@JsonIgnore
	public Integer getHeaderSize() {
		return getHeader().size();
	}

}

