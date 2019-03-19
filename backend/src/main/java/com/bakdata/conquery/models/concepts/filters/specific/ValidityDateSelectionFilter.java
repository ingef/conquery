package com.bakdata.conquery.models.concepts.filters.specific;

import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.concepts.ValidityDate;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
@CPSType(id="VALIDITY_DATE_SELECTION", base=Filter.class)
public class ValidityDateSelectionFilter extends Filter<String> implements ISelectFilter {

	
	
	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(FEFilterType.SELECT);
		f.setOptions(
			FEValue.fromLabels(
				getConnector()
				.getValidityDates()
				.stream()
				.collect(Collectors.toMap(ValidityDate::getName, ValidityDate::getLabel))
			)
		);
	}
	
	@Override
	public Column[] getRequiredColumns() {
		return getConnector().getValidityDates().stream().map(ValidityDate::getColumn).toArray(Column[]::new);
	}
	
	@Override
	public FilterNode createAggregator(String value) {
		return null;
	}
}
