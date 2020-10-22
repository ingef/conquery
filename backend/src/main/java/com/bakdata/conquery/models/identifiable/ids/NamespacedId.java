package com.bakdata.conquery.models.identifiable.ids;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

public interface NamespacedId {

	@JsonIgnore
	DatasetId getDataset();

	default String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString() + IId.JOIN_CHAR);
	}
}