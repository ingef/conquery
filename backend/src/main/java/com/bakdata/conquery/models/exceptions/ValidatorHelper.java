package com.bakdata.conquery.models.exceptions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;

import lombok.experimental.UtilityClass;
import org.slf4j.Logger;

@UtilityClass
public final class ValidatorHelper {
	
	private static final String VERTICAL_DIVIDER = "------------------------------------\n";

	public static void failOnError(Logger log, Set<? extends ConstraintViolation<?>> violations) throws JSONException {
		failOnError(log, violations, null);
	}
	
	public static <V extends ConstraintViolation<?>> void failOnError(Logger log, Set<V> violations, String context) throws JSONException {
		
		Map<Object, List<V>> mapByLeaf = violations.stream().collect(Collectors.groupingBy(ConstraintViolation::getLeafBean));
		
		throw new JSONException(mapByLeaf.entrySet().stream().map(ValidatorHelper::createViolationString).collect(Collectors.joining(VERTICAL_DIVIDER)));
	}
	
	/**
	 * Combines all violations for a given leaf object and gives the path to the root object if possible.
	 */
	private static <V extends ConstraintViolation<?>> String createViolationString(Map.Entry<Object, List<V>> objectToViolation) {
		Object leaf = objectToViolation.getKey();
		List<V> violations = objectToViolation.getValue();
		V firstViolation = violations.get(0);
		
		if(leaf == null) {
			// if validation is not directly mappable to an object
			return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("\n"));
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("\nThe object of type ").append(leaf.getClass()).append(" caused the following problem(s):\n");
		for(V violation : violations) {
			sb.append(violation.getMessage()).append("\n");
		}
		if(firstViolation.getRootBean() != null && !firstViolation.getRootBean().equals(firstViolation.getLeafBean())) {
			sb.append("The object was nested in an object of type ").append(firstViolation.getRootBean().getClass()).append(" through the path ").append(firstViolation.getPropertyPath()).append("\n");
		}
		sb.append("String representation of the failing object:\n").append(leaf);
		return sb.toString();
	}
}
