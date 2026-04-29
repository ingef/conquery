package com.bakdata.conquery.sql.conversion.cqelement;

import static org.jooq.impl.DSL.*;

import java.util.Optional;

import com.bakdata.conquery.apiv1.query.CQYes;
import com.bakdata.conquery.models.config.ColumnConfig;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import org.jooq.Field;
import org.jooq.Record;

public class CQYesConverter implements NodeConverter<CQYes> {

	private static final String ALL_IDS_CTE = "all_ids";

	@Override
	public Class<? extends CQYes> getConversionClass() {
		return CQYes.class;
	}

	@Override
	public ConversionContext convert(CQYes cqYes, ConversionContext context) {

		ColumnConfig primaryColumnConfig = context.getIdColumns().findPrimaryIdColumn();
		Field<Object> primaryColumn = field(name(primaryColumnConfig.getField()));
		SqlIdColumns ids = new SqlIdColumns(primaryColumn);

		Selects selects = Selects.builder().ids(ids)
								 .validityDate(Optional.of(context.getFunctionProvider().emptyColumnDateRange().asValidityDateRange(ALL_IDS_CTE)))
								 .build();
		org.jooq.Table<Record> fromTable = table(name(context.getIdColumns().getTable()));

		QueryStep cqYesTep = QueryStep.builder()
									  .cteName(ALL_IDS_CTE)
									  .selects(selects)
									  .fromTable(fromTable)
									  .build();
		return context.withQueryStep(cqYesTep);
	}
}
