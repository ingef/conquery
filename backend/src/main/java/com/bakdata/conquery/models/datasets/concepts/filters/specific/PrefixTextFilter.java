package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.EventFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.event.PrefixTextFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.List;

@Getter
@Setter
@CPSType(id = "PREFIX_TEXT", base = Filter.class)
public class PrefixTextFilter extends EventFilter<String> {

	private ColumnId column;

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(column);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.STRING);
	}

	@Override
	public EventFilterNode<String> createFilterNode(String value) {
		return new PrefixTextFilterNode(getColumn().resolve(), value);
	}
}
