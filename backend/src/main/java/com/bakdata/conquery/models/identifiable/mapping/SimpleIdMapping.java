package com.bakdata.conquery.models.identifiable.mapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.io.csv.CSV;
import org.apache.commons.lang3.tuple.Pair;

public class SimpleIdMapping extends IdMapping{

	public SimpleIdMapping(List<Pair<String, List<String>>> data) {
		super(data);
	}
	public SimpleIdMapping(CSV csvData) throws IOException {super(csvData);}

	@Override
	public List<IdMappingAccessor> getIdAccessors() {
		ArrayList<IdMappingAccessor> idAccessors = new ArrayList<>();
		idAccessors.add(new IdMappingAccessor(Arrays.asList(0)));
		return idAccessors;
	}

	@Override
	public List<String> getPrintIdFields() {
		return Arrays.asList("id");
	}
}
