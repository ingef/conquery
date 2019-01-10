package com.bakdata.conquery.models.identifiable.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.io.csv.CSV;

import lombok.Getter;

public abstract class IdMapping {
	@Getter
	private final Map<String, List<String>> data;

	public IdMapping(CSV csvData) throws IOException {
		List<Pair<String, List<String>>> data = new ArrayList<>();
		Iterator<String[]> csvIterator = csvData.iterateContent(null);
		// we skip the first row because it contains the headers
		csvIterator.next();
		// first column is the external key, the rest is part of the print key
		csvIterator.forEachRemaining(s -> data.add(Pair.of(s[0], Arrays.asList(Arrays.copyOfRange(s, 1, s.length)))));
		this.data = new HashMap<>();
		checkIntegrity(data);
	}

	public IdMapping(List<Pair<String, List<String>>> data) {
		this.data = new HashMap<>();
		checkIntegrity(data);
	}

	abstract public List<IdMappingAccessor> getIdAccessors();

	abstract public List<String> getPrintIdFields();

	private void checkIntegrity(List<Pair<String, List<String>>> data) {
		// check if all internal id's are unique
		long distinctSize = data
				.stream()
				.map(Pair::getKey)
				.distinct()
				.count();
		if (distinctSize != data.size()) {
			throw new IllegalArgumentException("The inserted csv ids are not unique");
		}

		// check that each idMappingAccessor leads to at most one tuple
		for (IdMappingAccessor idMappingAccessor : getIdAccessors()) {
			distinctSize = data
					.stream()
					.map(p -> idMappingAccessor.extract(p.getValue()))
					.distinct()
					//.peek(p -> log.info(p.stream().collect(Collectors.joining("."))))
					.count();
			// check if we still have the same size as before
			if (distinctSize != data.size()) {
				throw new IllegalArgumentException("The inserted print ids are not unique respective to the idMapping Accessor " + idMappingAccessor);
			}
		}
		for (Pair<String, List<String>> entry : data) {
			this.data.put(entry.getKey(), entry.getValue());
		}
	}

	public List<String> forCsv(String internalId) {
		return data.getOrDefault(internalId, null);
	}

	public List<String> toPrintId(String csvId) {
		return data.get(csvId);
	}

	/*
	 * @return the referred csvId or null if the id is absent
	 * @throws an IllegalArgument Exception if not enough columns
	 * of the printId are not Null so no fitting accessor can be found
	 */
	public String toCsvId(List<String> printId) {
		// find a fitting accessor
		IdMappingAccessor accessor = null;
		for (IdMappingAccessor idAccessor : getIdAccessors()) {
			if (idAccessor.validId(printId)) {
				accessor = idAccessor;
			}
		}
		if (accessor == null) {
			throw new IllegalArgumentException("No fitting accessor could be found");
		}
		List<String> partOfId = accessor.extract(printId);
		String internalId = null;

		for (Entry<String, List<String>> data : data.entrySet()) {
			if (accessor.extract(data.getValue()).equals(partOfId)) {
				internalId = data.getKey();
			}
		}

		return internalId;
	}

	public class IdMappingAccessor {
		@Getter
		private final List<Integer> idsUsed = new ArrayList<>();

		public IdMappingAccessor(List<Integer> idsUsed) {
			this.idsUsed.addAll(idsUsed);
		}

		@Override
		public String toString() {
			return "Using Fields: " + idsUsed;
		}

		/*package*/ boolean validId(List<String> externalId) {
			for (Integer integer : idsUsed) {
				if (externalId.get(integer) == null) {
					return false;
				}
			}
			return true;
		}

		/*package*/ List<String> extract(List<String> dataLine) {
			List<String> output = new ArrayList<>();
			for (Integer index : idsUsed) {
				output.add(dataLine.get(index));
			}
			return output;
		}
	}
}
