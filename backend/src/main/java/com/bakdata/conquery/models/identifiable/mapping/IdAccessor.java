package com.bakdata.conquery.models.identifiable.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.models.query.concept.specific.CQExternal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdAccessor {
	@Getter
	private final IdMappingAccessor accessor;
	@Getter
	private final Map<Integer, Integer> applicationMapping;
	private final NamespaceStorage storage;

	public static String[] removeNonIdFields(String[] csvLine, List<CQExternal.FormatColumn> formatColumns){
		List<String> result = new ArrayList<>();
		for (int i = 0; i < csvLine.length; i++) {
			if(formatColumns.get(i) == CQExternal.FormatColumn.ID){
				result.add(csvLine[i]);
			}
		}

		return result.toArray(new String[0]);
	}

	public CsvId apply(String[] csvLine) {
		String[] reorderedCsvLine = reorder(csvLine);
		String[] partOfId = this.accessor.extract(reorderedCsvLine);
		CsvId csvId = storage.getIdMapping().getExternalIdPartCsvIdMap().get(new ExternalIdPart(this.accessor, partOfId));
		if(csvId != null){
			return csvId;
		}
		// fallback: we join everything relevant together
		return CsvId.getFallbackCsvId(partOfId);
	}

	/*
	* Reorder takes a line from an uploaded CSV an reorders the id columns, to match the specified format.
	* @returns Outputs a csvLine like the one in the original uploaded id mapping CSV
 	*/
	private String[] reorder(String[] csvLine) {
		String[] reorderedCsvLine = new String[accessor.getMapping().getHeaderSize()];
		for (int i = 0; i < csvLine.length; i++) {
			int indexInHeader = applicationMapping.get(i);
			if (indexInHeader != -1) {
				reorderedCsvLine[indexInHeader] = csvLine[i];
			}
		}
		return reorderedCsvLine;
	}

}
