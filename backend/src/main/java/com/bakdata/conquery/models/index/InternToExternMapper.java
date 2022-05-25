package com.bakdata.conquery.models.index;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@CPSBase
@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
public interface InternToExternMapper extends NamespacedIdentifiable<InternToExternMapperId>, Named<InternToExternMapperId> {

	void init();

	boolean initialized();

	String external(String internalValue);

	void setDataset(Dataset dataset);

	@Override
	InternToExternMapperId getId();
}
