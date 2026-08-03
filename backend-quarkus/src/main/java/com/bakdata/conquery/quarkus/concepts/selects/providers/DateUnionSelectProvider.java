package com.bakdata.conquery.quarkus.concepts.selects.providers;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.SelectConversionContext;
import com.bakdata.conquery.quarkus.concepts.selects.definitions.DateUnionSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DateUnionSelectProvider extends AbstractSelectProvider<DateUnionSelectDefinition> {

	public DateUnionSelectProvider() {
		super(DateUnionSelectDefinition.class);
	}

	@Override
	public String type() {
		return "DATE_UNION";
	}

	@Override
	public DatasetCatalogRepository.Select convert(SelectConversionContext context, DateUnionSelectDefinition payload) {
		List<ColumnId> columns = dateRangeColumns(context, payload);
		requireDateRangeTypes(context, columns);
		return select(context, payload, list("DATE_RANGE"), columns);
	}
}
