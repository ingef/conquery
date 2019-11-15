package com.bakdata.conquery.models.query.concept.specific;

import com.bakdata.conquery.models.concepts.Concept;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CQConceptSpecifier {
	public Class<? extends Concept<?>> concept();
}
