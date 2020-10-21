package com.bakdata.conquery.models.exceptions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;

import io.dropwizard.logback.shaded.guava.base.Optional;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

@UtilityClass
public final class ValidatorHelper {
	
	private static final String VERTICAL_DIVIDER = "------------------------------------\n";

	public static void failOnError(Logger log, Set<? extends ConstraintViolation<?>> violations) {
		failOnError(log, violations, null);
	}
	
	public static <V extends ConstraintViolation<?>> void failOnError(Logger log, Set<V> violations, String context) {
		
		// Wrap grouper in Optional to also catch null values.
		Map<Optional<Object>, List<V>> mapByLeaf = violations.stream().collect(Collectors.groupingBy(v -> Optional.of(v.getLeafBean())));
		
		// Combine all leaf fail reports into a single exception.
		if(!mapByLeaf.isEmpty()) {
			throw new ValidationException(mapByLeaf.entrySet().stream().map(ValidatorHelper::createViolationString).collect(Collectors.joining(VERTICAL_DIVIDER)));			
		}
	}
	
	/**
	 * Combines all violations for a given leaf object and gives the path to the root object if possible.
	 */
	private static <V extends ConstraintViolation<?>> String createViolationString(Map.Entry<Optional<Object>, List<V>> objectToViolation) {
		Object leaf = objectToViolation.getKey().orNull();
		List<V> violations = objectToViolation.getValue();
		V firstViolation = violations.get(0);
		
		if(leaf == null) {
			// If validations are not directly mappable to an object, directly return the violation messages.
			return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("\n"));
		}
		
		// Build report for a specific failing leaf.
		StringBuilder sb = new StringBuilder();
		sb.append("\nThe object of type ").append(leaf.getClass()).append(" caused the following problem(s):\n");
		for(V violation : violations) {
			// List all the violations for the specific leaf. 
			sb.append("\t- ").append(violation.getMessage()).append("\n");
		}
		if(firstViolation.getRootBean() != null && !firstViolation.getRootBean().equals(firstViolation.getLeafBean())) {
			// Extract the path to the failing leaf from the root object.
			sb.append("The object was nested in an object of type ").append(firstViolation.getRootBean().getClass()).append(" through the path ").append(firstViolation.getPropertyPath()).append("\n");
		}
		// Its maybe helpful to see what the failing leaf looks like.
		sb.append("String representation of the failing object:\n").append(leaf);
		return sb.toString();
	}
}
