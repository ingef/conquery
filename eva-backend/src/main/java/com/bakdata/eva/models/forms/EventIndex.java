package com.bakdata.eva.models.forms;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum EventIndex {
	BEFORE(FeatureGroup.FEATURE),
	NEUTRAL(null),
	AFTER(FeatureGroup.OUTCOME);
	
	/**
	 * The group, the event belongs to.
	 */
	private final FeatureGroup group;
	
	
}