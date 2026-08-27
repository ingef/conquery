package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.*;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.ColumnUtils;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.EventFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.event.FlagColumnsFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.sql.conversion.model.aggregator.FlagSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Implements a MultiSelect type filter, where an event can meet multiple criteria (as opposed to {@link MultiSelectFilter} which is restricted to one value per event).
 * This is achieved by using multiple {@link com.bakdata.conquery.models.types.ResultType.Primitive#BOOLEAN} columns, each defining if one property is met or not.
 * <p>
 * The selected flags are logically or-ed.
 */
@Getter
@CPSType(base = Filter.class, id = "FLAGS")
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
@ToString
public class FlagFilter extends EventFilter<Set<String>> {

	private final Map<String, ColumnId> flags;

	@Override
	protected void configureFrontend(
		FrontendFilterConfiguration.Top f,
		ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		f.setType(FrontendFilterType.Fields.MULTI_SELECT);

		f.setOptions(flags.keySet().stream().map(key -> new FrontendValue(key, key)).toList());
	}

	@Override
	public List<ColumnId> getRequiredColumns() {
		return new ArrayList<>(flags.values());
	}

	@Override
	public EventFilterNode<?> createFilterNode(Set<String> labels) {

		final Set<String> missing = new HashSet<>(labels.size());
		final List<Column> columns = new ArrayList<>();

		for (String label : labels) {
			final ColumnId columnId = flags.get(label);

			// Column is not defined with us.
			if (columnId == null) {
				missing.add(label);
				continue;
			}

			columns.add(columnId.resolve());
		}

		if (!missing.isEmpty()) {
			throw new ConqueryError.ExecutionCreationPlanMissingFlagsError(missing);
		}

		return new FlagColumnsFilterNode(columns.toArray(Column[]::new));
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns must be unique.")
	public boolean isAllColumnsOfSameTable() {
		return flags.values().stream().distinct().count() == flags.size();
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns must be BOOLEAN.")
	public boolean isAllColumnsBoolean() {
		boolean valid = true;
		for (ColumnId column : flags.values()) {
			valid &= ColumnUtils.assertValidColumnTypes(column, EnumSet.of(MajorTypeId.BOOLEAN));

		}

		return valid;
	}

	@Override
	public FilterConverter<FlagFilter, Set<String>> createConverter() {
		return new FlagSqlAggregator();
	}
}
