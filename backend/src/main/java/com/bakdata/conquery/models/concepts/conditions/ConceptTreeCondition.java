package com.bakdata.conquery.models.concepts.conditions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.Prefix;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.util.CalculatedValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * A general condition that serves as a guard for concept tree nodes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface ConceptTreeCondition {

	//TODO better name?
	public static String COLUMN_PLACEHOLDER = "$COLUMN";



	public default void init(ConceptTreeNode node) throws ConceptConfigurationException {
	}

	public boolean matches(String value, CalculatedValue<Map<String, Object>> rowMap) throws ConceptConfigurationException;

	@JsonIgnore
	public Map<String, RangeSet<Prefix>> getColumnSpan();


	public static void mergeRanges(Map<String, RangeSet<Prefix>> into, Map<String, RangeSet<Prefix>> from) {
		for (Map.Entry<String, RangeSet<Prefix>> entry : from.entrySet()) {
			into.computeIfAbsent(entry.getKey(), (ignored) -> TreeRangeSet.create())
				.addAll(entry.getValue());
		}
	}

	public static Map<String, RangeSet<Prefix>> mergeAll(Collection<ConceptTreeCondition> nodes) {
		Map<String, RangeSet<Prefix>> merged = new HashMap<>();

		for (ConceptTreeCondition condition : nodes) {
			final Map<String, RangeSet<Prefix>> range = condition.getColumnSpan();

			ConceptTreeCondition.mergeRanges(merged, range);
		}
		return merged;
	}
}
