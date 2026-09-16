package com.bakdata.conquery.sql.compiler.naming;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.bakdata.conquery.sql.compiler.ir.CteStep;
import com.bakdata.conquery.sql.compiler.ir.JoinMode;
import com.bakdata.conquery.sql.model.node.ConceptNode;
import com.bakdata.conquery.sql.model.operation.ResolvedFilter;
import com.bakdata.conquery.sql.model.operation.ResolvedSelect;
import com.bakdata.conquery.sql.model.schema.ResolvedConnector;

/**
 * Stateful generator for unique, length-constrained SQL aliases and CTE names.
 *
 * <p>The same instance must be reused throughout a conversion context tree. Naming operations accept resolved SQL
 * model types so their stable technical identifiers remain part of the compiler contract. Instances are not
 * thread-safe.</p>
 */
public class SqlNameGenerator {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final Map<String, Integer> operationCountMap = new HashMap<>();
	private final int nameMaxLength;

	private int conceptCount;
	private int andCount;
	private int orCount;

	public SqlNameGenerator(int nameMaxLength) {
		this.nameMaxLength = nameMaxLength;
	}

	public String cteStepName(CteStep cteStep, String nodeLabel) {
		return ensureValidLength(cteStep.cteName(nodeLabel));
	}

	public String selectName(ResolvedSelect select) {
		return operationName(select.name());
	}

	public String filterName(ResolvedFilter filter) {
		return operationName(filter.name());
	}

	/** TODO Remove this legacy-backend bridge once backend converters consume resolved operations. */
	public String legacyOperationName(String operationName) {
		return operationName(operationName);
	}

	private String operationName(String operationName) {
		int operationCount = operationCountMap.merge(operationName, 1, Integer::sum);
		String normalizedName = lowerAndReplaceWhitespace(operationName);
		return ensureValidLength("%s-%d".formatted(normalizedName, operationCount));
	}

	public String conceptName(ConceptNode concept) {
		return conceptName(concept.logicalId());
	}

	/** TODO Remove this legacy-backend bridge once backend converters consume resolved query nodes. */
	public String legacyConceptName(String conceptName) {
		return conceptName(conceptName);
	}

	private String conceptName(String conceptLabel) {
		String normalizedLabel = lowerAndReplaceWhitespace(conceptLabel);
		return ensureValidLength("concept_%s-%d".formatted(normalizedLabel, ++conceptCount));
	}

	public String conceptConnectorName(ConceptNode concept, ResolvedConnector connector) {
		return conceptConnectorName(concept.logicalId(), connector.logicalId());
	}

	/** TODO Remove this legacy-backend bridge once backend converters consume resolved query nodes. */
	public String legacyConceptConnectorName(String conceptName, String connectorName) {
		return conceptConnectorName(conceptName, connectorName);
	}

	private String conceptConnectorName(String conceptLabel, String connectorName) {
		String normalizedConceptLabel = lowerAndReplaceWhitespace(conceptLabel);
		String normalizedConnectorName = lowerAndReplaceWhitespace(connectorName);
		return ensureValidLength("concept_%s_%s-%d".formatted(normalizedConceptLabel, normalizedConnectorName, conceptCount));
	}

	public String joinedNodeName(JoinMode logicalOperation) {
		return switch (logicalOperation) {
			case INNER -> "AND-%d".formatted(++andCount);
			case FULL_OUTER -> "OR-%d".formatted(++orCount);
			case LEFT -> throw new UnsupportedOperationException("Creating CTE names for LEFT joins is not supported");
		};
	}

	private String ensureValidLength(String input) {
		if (input.length() > nameMaxLength) {
			int removeIndex = input.length() - nameMaxLength;
			return input.substring(removeIndex);
		}
		return input;
	}

	private static String lowerAndReplaceWhitespace(String name) {
		return WHITESPACE.matcher(name.toLowerCase())
				.replaceAll("_");
	}
}
