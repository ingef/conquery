package com.bakdata.eva.models.forms;

import com.bakdata.eva.EvaConstants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeatureGroup {
	FEATURE(EvaConstants.FEATURE_PREFIX),
	OUTCOME(EvaConstants.OUTCOME_PREFIX),
	SINGLE_GROUP("");
	
	/**
	 * Is used for the column name in the CSVs that are send to the REnd.
	 */
	private final String prefix;
}
