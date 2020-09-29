package com.bakdata.conquery.models.query.concept.specific;

import com.bakdata.conquery.models.concepts.Concept;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This class is used to disambiguate {@link CQConcept} based on the {@link Concept} they are targeting.
 * This can be used to inject Query logic transparently without the front-end knowing about it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CQConceptSpecifier {
	public Class<? extends Concept<?>> concept();
}
