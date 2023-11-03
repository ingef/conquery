package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.event.FlagColumnsFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Implements a MultiSelect type filter, where an event can meet multiple criteria (as opposed to {@link MultiSelectFilter} which is restricted to one value per event).
 * This is achieved by using multiple {@link com.bakdata.conquery.models.types.ResultType.BooleanT} columns, each defining if one property is met or not.
 *
 * The selected flags are logically or-ed.
 */
@CPSType(base = Filter.class, id = "FLAGS")
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
@ToString
public class FlagFilter extends Filter<String[]> {

	@Getter
	@NsIdRefCollection
	private final Map<String, Column> flags;

	@Override
	protected void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		f.setType(FrontendFilterType.Fields.MULTI_SELECT);

		f.setOptions(flags.keySet().stream().map(key -> new FrontendValue(key, key)).toList());
	}

	@Override
	public List<Column> getRequiredColumns() {
		return new ArrayList<>(flags.values());
	}

	@Override
	public FilterNode<?> createFilterNode(String[] labels) {
		final Column[] columns = new Column[labels.length];

		final Set<String> missing = new HashSet<>(labels.length);

		for (int index = 0; index < labels.length; index++) {
			final String label = labels[index];
			final Column column = flags.get(label);

			// Column is not defined with us.
			if (column == null) {
				missing.add(label);
			}

			columns[index] = column;
		}

		if(!missing.isEmpty()){
			throw new ConqueryError.ExecutionCreationPlanMissingFlagsError(missing);
		}

		return new FlagColumnsFilterNode(columns);
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns must be unique.")
	public boolean isAllColumnsOfSameTable() {
		return flags.values().stream().distinct().count() == flags.size();
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns must be BOOLEAN.")
	public boolean isAllColumnsBoolean() {
		return flags.values().stream().map(Column::getType).allMatch(MajorTypeId.BOOLEAN::equals);
	}
}
