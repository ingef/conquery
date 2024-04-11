package com.bakdata.conquery.sql.conversion.forms;

import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FormCteStep implements CteStep {

	// prerequisite
	EXTRACT_IDS("extract_ids"),

	// stratification new approach
	INDEX_START("index_start"),
	INT_SERIES("int_series"),
	YEAR_COUNTS("year_counts"),
	QUARTER_COUNTS("quarter_counts"),
	DAY_COUNTS("day_counts"),

	// stratification
	DAYS("days"),
	QUARTERS("quarters"),
	YEARS("years"),
	COMPLETE("complete"),
	FULL_STRATIFICATION("full_stratification");

	private final String suffix;

	public static FormCteStep countsCte(Resolution resolution) {
		return switch (resolution) {
			case COMPLETE -> throw new UnsupportedOperationException("TODO can we use this?");
			case YEARS -> YEAR_COUNTS;
			case QUARTERS -> QUARTER_COUNTS;
			case DAYS -> DAY_COUNTS;
		};
	}

	public static FormCteStep stratificationCte(Resolution resolution) {
		return switch (resolution) {
			case COMPLETE -> FormCteStep.COMPLETE;
			case YEARS -> FormCteStep.YEARS;
			case QUARTERS -> FormCteStep.QUARTERS;
			case DAYS -> FormCteStep.DAYS;
		};
	}

}
