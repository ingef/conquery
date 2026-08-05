package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.util.EnumSet;
import java.util.List;

import com.bakdata.conquery.models.common.ColumnUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.EventFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.event.PrefixTextFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@CPSType(id = "PREFIX_TEXT", base = Filter.class)
public class PrefixTextFilter extends EventFilter<String> {

	@Valid
	@NotNull
	private ColumnId column;


	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) {
		f.setType(FrontendFilterType.Fields.STRING);
	}

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(getColumn());
	}


	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		return ColumnUtils.assertValidColumnTypes(this, getColumn(), EnumSet.of(MajorTypeId.STRING));
	}

	@Override
	public EventFilterNode<?> createFilterNode(String value) {
		return new PrefixTextFilterNode(getColumn().resolve(), value);
	}
}
