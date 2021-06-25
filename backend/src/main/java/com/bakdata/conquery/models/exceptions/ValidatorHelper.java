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


	public static <V extends ConstraintViolation<?>> void failOnError(Logger log, Set<V> violations) {

		Map<Optional<Object>, List<V>> mapByRoot = violations.stream().collect(Collectors.groupingBy(v -> Optional.of(v.getRootBean())));

		// Wrap grouper in Optional to also catch null values.
		// Combine all leaf fail reports into a single exception.
		if (!mapByRoot.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (Entry<Optional<Object>, List<V>> entry : mapByRoot.entrySet()) {
				Object root = entry.getKey().orElse(null);
				if (root != null) {
					sb.append("\nValidation failed on: ").append(root.getClass());
					if (log.isTraceEnabled()) {
						sb.append("(").append(root.toString()).append(")");
					}
				}

				// Group violations by their nodes, convert the violations to messages, then append them to sb
				entry.getValue().stream()
					 .collect(Collectors.groupingBy(v -> v.getLeafBean()))

					 .entrySet().stream()
					 .map(objectToViolation -> createViolationString(objectToViolation.getKey(), objectToViolation.getValue()))
					 .forEach(sb::append);
			}
			throw new ValidationException(sb.toString());
		}
	}

	/**
	 * Combines all violations for a given leaf object and gives the path to the root object if possible.
	 */
	private static <V extends ConstraintViolation<?>> String createViolationString(Object node, List<V> violations) {
		StringBuilder sb = new StringBuilder();

		if (node == null) {
			// If validations are not directly mappable to an object, directly return the violation messages.
			return violations.stream()
							 .map(ConstraintViolation::getMessage).collect(Collectors.joining("\n\t"));
		}

		// Build report for a specific failing leaf.
		if (log.isTraceEnabled()) {
			sb.append("\n\tFor the leaf type '").append(node.getClass()).append("':");
		}
		for (V violation : violations) {
			// List all the violations for the specific leaf. 
			sb.append("\n\t\t- ").append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("");
		}
		return sb.toString();
	}
}
