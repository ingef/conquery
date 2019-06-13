package com.bakdata.eva.forms.common;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TimeSelector {
	FIRST(TemporalSampler.EARLIEST),
	LAST(TemporalSampler.LATEST),
	RANDOM(TemporalSampler.RANDOM);

	private final TemporalSampler sampler;
	
	public OptionalInt sample(CDateSet data) {
		return sampler.sample(data);
	}
}
