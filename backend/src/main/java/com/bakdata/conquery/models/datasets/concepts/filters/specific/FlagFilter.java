package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.query.filter.event.FlagColumnsFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@CPSType(base = Filter.class, id = "FLAGS")
@RequiredArgsConstructor
@ToString
public class FlagFilter extends Filter<String[]> {

	@NsIdRefCollection
	private final Map<String, Column> flags;

	@Override
	protected void configureFrontend(FrontendFilterConfiguration.Top f) throws ConceptConfigurationException {
		f.setType(FrontendFilterType.Fields.MULTI_SELECT);
		f.setOptions(null); // TODO map flags to FEValues
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

			columns[index] = column;
		}

		return new FlagColumnsFilterNode(columns);
	}
}
