package com.bakdata.conquery.models.datasets.concepts.conditions;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import lombok.Getter;
import lombok.Setter;

/**
 * This condition connects multiple conditions with an and.
 */
@CPSType(id="AND", base=CTCondition.class)
public class AndCondition implements CTCondition {

	@Setter @Getter @Valid @NotEmpty
	private List<CTCondition> conditions;

	@Override
	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException {
		for(CTCondition cond:conditions) {
			if(!cond.matches(value, rowMap)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void init(ConceptTreeNode node) throws ConceptConfigurationException {
		for(CTCondition cond:conditions) {
			cond.init(node);
		}
	}
}
