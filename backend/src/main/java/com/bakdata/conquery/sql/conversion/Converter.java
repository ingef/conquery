package com.bakdata.conquery.sql.conversion;

import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.sql.conversion.cqelement.concept.CteStep;

/**
 * A converter converts an input into a result object if the input matches the conversion class.
 *
 * @param <C> type that can be converted
 * @param <R> type of the result
 * @param <X> context of the convertible
 */
public interface Converter<C, R, X extends Context> {

	default <I> Optional<R> tryConvert(I input, X context) {
		if (getConversionClass().isInstance(input)) {
			return Optional.ofNullable(convert(getConversionClass().cast(input), context));
		}
		return Optional.empty();
	}

	/**
	 * All steps this {@link Converter} requires.
	 *
	 * @return PREPROCESSING, AGGREGATION_SELECT and FINAL {@link CteStep} as defaults. Override if more steps are required.
	 */
	default Set<CteStep> requiredSteps() {
		return CteStep.MANDATORY_STEPS;
	}

	Class<? extends C> getConversionClass();

	R convert(final C convert, final X context);

}
