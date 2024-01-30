package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.ExistsSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.SelectContext;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import lombok.Value;
import org.jooq.impl.DSL;

@Value
public class ExistsSqlAggregator implements SqlAggregator {

	SqlSelects sqlSelects;
	WhereClauses whereClauses;

	private ExistsSqlAggregator(String alias) {
		FieldWrapper<Integer> existsSelect = new UniversalSqlSelect<>(DSL.field("1", Integer.class).as(alias));
		this.sqlSelects = SqlSelects.builder()
									.finalSelect(existsSelect)
									.build();
		this.whereClauses = WhereClauses.builder().build();
	}

	public static ExistsSqlAggregator create(ExistsSelect existsSelect, SelectContext selectContext) {
		String alias = selectContext.getNameGenerator().selectName(existsSelect);
		return new ExistsSqlAggregator(alias);
	}

}
