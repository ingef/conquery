package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.models.query.concept.CQElement;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Abstract data class specifying the data needed for a TemporalQuery
 */
@Getter
@AllArgsConstructor
public abstract class AbstractTemporalQuery implements CQElement {

	CQElement index;
	CQElement preceding;

	TemporalSampler sampler;
}
