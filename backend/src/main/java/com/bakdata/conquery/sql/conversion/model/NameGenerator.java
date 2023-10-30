package com.bakdata.conquery.sql.conversion.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.identifiable.Labeled;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NameGenerator {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private final Map<String, Integer> selectCountMap = new HashMap<>();
	private final int nameMaxLength;

	private int conceptCount = 0;
	private int andCount = 0;
	private int orCount = 0;

	public NameGenerator(int nameMaxLength) {
		this.nameMaxLength = nameMaxLength;
	}

	public String cteStepName(String cteName, CteStep cteStep) {
		if (cteStep.suffix().isEmpty()) {
			return ensureValidLength(cteName);
		}
		String withValidLength = "%s-%s".formatted(cteName, cteStep.suffix());
		return ensureValidLength(withValidLength);
	}

	public String selectName(Labeled<?> selectOrFilter) {
		Integer selectCount = this.selectCountMap.merge(selectOrFilter.getName(), 1, Integer::sum);
		return ensureValidLength("%s-%s".formatted(selectOrFilter.getName(), selectCount));
	}

	public String conceptName(CQConcept concept) {
		String conceptLabel = WHITESPACE.matcher(concept.getUserOrDefaultLabel(Locale.ENGLISH).toLowerCase()).replaceAll("_");
		return ensureValidLength("concept_%s-%s".formatted(++conceptCount, conceptLabel));
	}

	public String joinedNodeName(LogicalOperation logicalOperation) {
		return switch (logicalOperation) {
			case AND -> "AND-%s".formatted(++andCount);
			case OR -> "OR-%s".formatted(++orCount);
		};
	}

	private String ensureValidLength(String input) {
		if (input.length() > nameMaxLength) {
			log.debug("CTE or select name too long: {}", input);
			int removeIndex = input.length() - nameMaxLength;
			return input.substring(removeIndex);
		}
		return input;
	}

}
