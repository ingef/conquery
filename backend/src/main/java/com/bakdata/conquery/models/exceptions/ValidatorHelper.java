package com.bakdata.conquery.models.exceptions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@UtilityClass
@Slf4j
public final class ValidatorHelper {
	
	private static final String VERTICAL_DIVIDER = "\n------------------------------------";

	public static void failOnError(Logger log, Set<? extends ConstraintViolation<?>> violations) {
		failOnError(log, violations, null);
	}
	
	public static <V extends ConstraintViolation<?>> void failOnError(Logger log, Set<V> violations, String context) {
		
		Map<Optional<Object>, List<V>> mapByRoot = violations.stream().collect(Collectors.groupingBy(v -> Optional.of(v.getRootBean())));
		
		// Wrap grouper in Optional to also catch null values.
		// Combine all leaf fail reports into a single exception.
		if(!mapByRoot.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for(Entry<Optional<Object>, List<V>> entry : mapByRoot.entrySet()) {
				Object root = entry.getKey().orElse(null);
				if(root != null) {					
					sb.append("\nValidation failed on: ").append(root.getClass());
					if(log.isTraceEnabled() &&  root != null) {
						sb.append("(").append(root.toString()).append(")");
					}
				}
				Map<Optional<Object>, List<V>> mapByLeaf = entry.getValue().stream().collect(Collectors.groupingBy(v -> Optional.of(v.getLeafBean())));
					sb.append(
						mapByLeaf.entrySet().stream().map(ValidatorHelper::createViolationString).collect(Collectors.joining("")
					));
				
			}
			throw new ValidationException(sb.toString());
		}
	}
	
	/**
	 * Combines all violations for a given leaf object and gives the path to the root object if possible.
	 */
	private static <V extends ConstraintViolation<?>> String createViolationString(Map.Entry<Optional<Object>, List<V>> objectToViolation) {
		Object leaf = objectToViolation.getKey().orElse(null);
		List<V> violations = objectToViolation.getValue();
		StringBuilder sb = new StringBuilder();
		
		if(leaf == null) {
			// If validations are not directly mappable to an object, directly return the violation messages.
			return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("\n\t"));
		}
		
		// Build report for a specific failing leaf.
		if(log.isTraceEnabled()) {
			sb.append("\n\tFor the leaf type '").append(leaf.getClass()).append("':");
		}
		for(V violation : violations) {
			// List all the violations for the specific leaf. 
			sb.append("\n\t\t- ").append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("");
		}
		return sb.toString();
	}
}
