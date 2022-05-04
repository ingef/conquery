package com.bakdata.conquery.models.datasets.concepts;

import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Searchable {
	Stream<FEValue> getSearchValues(CSVConfig config, NamespaceStorage storage);

	@JsonIgnore
	default Searchable getSearchReference() {
		//Hopefully the only candidate will be Column
		return this;
	}

	int getMinSuffixLength();

	boolean isGenerateSuffixes();
}
