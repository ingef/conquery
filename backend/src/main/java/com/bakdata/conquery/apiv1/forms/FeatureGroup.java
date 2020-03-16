package com.bakdata.conquery.apiv1.forms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeatureGroup {
	FEATURE,
	OUTCOME,
	SINGLE_GROUP;
}
