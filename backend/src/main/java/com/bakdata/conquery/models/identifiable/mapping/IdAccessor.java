package com.bakdata.conquery.models.identifiable.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdAccessor {
	/*package*/ final IdMappingAccessor accessor;
	/*package*/ final Map<Integer, Integer> applicationMapping;
	private NamespaceStorage storage;

	public static String[] removeNonIdFields(String[] csvLine, List<CQExternal.FormatColumn> formatColumns){
		List<String> result = new ArrayList<>();
		for (int i = 0; i < csvLine.length; i++) {
			if(formatColumns.get(i) == CQExternal.FormatColumn.ID){
				result.add(csvLine[i]);
			}
		}

		return result.toArray(new String[0]);
	}

	public String apply(String[] csvLine) {
		List<String> reorderedCsvLine = reorder(csvLine);
		List<String> partOfId = this.accessor.extract(reorderedCsvLine);
		for (Map.Entry<String, List<String>> data : storage.getIdMapping().entrySet()) {
			if (accessor.extract(data.getValue()).equals(partOfId)) {
				return data.getKey();
			}
		}
		// fallback: we join everything relevant together
		return String.join("|", partOfId);
	}

	/*
	@returns Outputs a csvLine like the one in the original uploaded id mapping CSV
	 */
	private List<String> reorder(String[] csvLine) {
		List<String> reorderedCsvLine = new ArrayList<>(accessor.getMapping().getHeaderSize());
		for (int i = 0; i < csvLine.length; i++) {
			int indexInHeader = applicationMapping.get(i);
			if (indexInHeader != -1) {
				reorderedCsvLine.set(indexInHeader, csvLine[i]);
			}
		}
		return reorderedCsvLine;
	}

}
