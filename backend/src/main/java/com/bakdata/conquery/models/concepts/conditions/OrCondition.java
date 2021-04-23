package com.bakdata.conquery.models.concepts.conditions;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.google.common.collect.RangeSet;
import lombok.Getter;
import lombok.Setter;

/**
 * This condition connects multiple conditions with an or.
 */
@CPSType(id = "OR", base = ConceptTreeCondition.class)
public class OrCondition implements ConceptTreeCondition {

	@Setter
	@Getter
	@Valid
	@NotEmpty
	private List<ConceptTreeCondition> conditions;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		for (ConceptTreeCondition cond : conditions) {
			if (cond.matches(value, rowMap)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<String, RangeSet<String>> getColumnSpan() {
		return ConceptTreeCondition.mergeAll(getConditions());
	}

	@Override
	public void init(ConceptTreeNode node) throws ConceptConfigurationException {
		for (ConceptTreeCondition cond : conditions) {
			cond.init(node);
		}
	}
}
