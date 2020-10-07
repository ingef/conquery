package com.bakdata.conquery.models.identifiable.ids;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface NamespacedId {

	@JsonIgnore
	DatasetId getDataset();

	default String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString() + IId.JOIN_CHAR);
	}
}