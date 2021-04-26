package com.bakdata.conquery.models.concepts.conditions;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.Prefix;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.RangeSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This condition connects multiple conditions with an and.
 */
@CPSType(id = "AND", base = ConceptTreeCondition.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator(mode = JsonCreator.Mode.PROPERTIES))
public class AndCondition implements ConceptTreeCondition {

	@Getter
	@Valid
	@NotEmpty
	private final List<ConceptTreeCondition> conditions;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		for (ConceptTreeCondition cond : conditions) {
			if (!cond.matches(value, rowMap)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Map<String, RangeSet<Prefix>> getColumnSpan() {
		return ConceptTreeCondition.mergeAll(getConditions());
	}

	@Override
	public void init(ConceptTreeNode node) throws ConceptConfigurationException {
		for (ConceptTreeCondition cond : conditions) {
			cond.init(node);
		}
	}

}
