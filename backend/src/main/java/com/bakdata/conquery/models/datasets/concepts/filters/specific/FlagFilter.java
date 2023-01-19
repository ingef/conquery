package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.apiv1.frontend.FrontendValue;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.event.FlagColumnsFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@CPSType(base = Filter.class, id = "FLAGS")
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
@ToString
public class FlagFilter extends Filter<String[]> {

	@NsIdRefCollection
	private final Map<String, Column> flags;

	@Override
	protected void configureFrontend(FrontendFilterConfiguration.Top f) throws ConceptConfigurationException {
		f.setType(FrontendFilterType.Fields.MULTI_SELECT);

		f.setOptions(flags.keySet().stream().map(key -> new FrontendValue(key, key)).toList());
	}

	@Override
	public List<Column> getRequiredColumns() {
		return new ArrayList<>(flags.values());
	}

	@Override
	public FilterNode<?> createFilterNode(String[] strings) {
		final Column[] columns = new Column[strings.length];

		for (int index = 0; index < strings.length; index++) {
			final String string = strings[index];
			final Column column = flags.get(string);

			// Column is not defined with us.
			if (column == null) {
				//TODO message
				throw new ConqueryError.ExecutionCreationPlanError();
			}

			columns[index] = column;
		}

		return new FlagColumnsFilterNode(columns);
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns must be unique.")
	public boolean isAllColumnsOfSameTable() {
		return flags.values().stream().distinct().count() == flags.size();
	}
}
