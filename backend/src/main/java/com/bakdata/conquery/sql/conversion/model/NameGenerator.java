package com.bakdata.conquery.sql.conversion.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to ensure no generated database identifier, like a CTE name or a select alias, is unique and does not exceed the maximum allowed length of the DBMS.
 * <p>
 * Carries counters for CONCEPT, AND and OR nodes as well as for all distinct select aliases. For every generated identifier, the counter will be
 * incremented to ensure unique names.
 * <ul>
 *     <li><b>Always reuse the same NameGenerator during the lifetime of a {@link ConversionContext}.</b></li>
 *     <li><b>NameGenerator is not thread-safe!</b></li>
 * </ul>
 */
@Slf4j
@Data
public class NameGenerator {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final Map<String, Integer> selectCountMap;
	private final int nameMaxLength;

	private int conceptCount;
	private int andCount;
	private int orCount;

	public NameGenerator(int nameMaxLength) {
		this.nameMaxLength = nameMaxLength;
		this.conceptCount = 0;
		this.andCount = 0;
		this.orCount = 0;
		this.selectCountMap = new HashMap<>();
	}

	public String cteStepName(CteStep cteStep, String nodeLabel) {
		return ensureValidLength(cteStep.cteName(nodeLabel));
	}

	public String selectName(Labeled<?> selectOrFilter) {
		int selectCount = this.selectCountMap.merge(selectOrFilter.getName(), 1, Integer::sum);
		String name = lowerAndReplaceWhitespace(selectOrFilter.getName());
		return ensureValidLength("%s-%d".formatted(name, selectCount));
	}

	public String conceptName(CQConcept concept) {
		String conceptLabel = lowerAndReplaceWhitespace(concept.getUserOrDefaultLabel(Locale.ENGLISH));
		return ensureValidLength("concept_%s-%d".formatted(conceptLabel, ++conceptCount));
	}

	public String joinedNodeName(LogicalOperation logicalOperation) {
		return switch (logicalOperation) {
			case AND -> "AND-%d".formatted(++andCount);
			case OR -> "OR-%d".formatted(++orCount);
		};
	}

	private String ensureValidLength(String input) {
		if (input.length() > nameMaxLength) {
			log.trace("CTE or select name too long: {}", input);
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
