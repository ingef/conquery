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

	// stratification
	QUARTER_SERIES("quarter_series"),
	QUARTERS("quarters"),
	YEAR_SERIES("year_series"),
	YEARS("years"),
	COMPLETE("complete"),
	FULL_STRATIFICATION("full_stratification");

	private final String suffix;

	public static FormCteStep stratificationCte(Resolution resolution) {
		return switch (resolution) {
			case COMPLETE -> FormCteStep.COMPLETE;
			case YEARS -> FormCteStep.YEARS;
			case QUARTERS -> FormCteStep.QUARTERS;
			case DAYS -> throw new UnsupportedOperationException("Not implemented yet");
		};
	}

	public static FormCteStep seriesCte(Resolution resolution) {
		return switch (resolution) {
			case YEARS -> FormCteStep.YEAR_SERIES;
			case QUARTERS -> FormCteStep.QUARTER_SERIES;
			case DAYS, COMPLETE -> throw new UnsupportedOperationException("Not supported");
		};
	}

}
