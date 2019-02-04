package com.bakdata.conquery.models.forms;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum FeatureGroup {
	FEATURE("feature_date_range"), OUTCOME("outcome_date_range");

	@Getter
	private final String dateRangeFieldName;
}
