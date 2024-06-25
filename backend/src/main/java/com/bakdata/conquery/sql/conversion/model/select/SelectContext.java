package com.bakdata.conquery.sql.conversion.model.select;

import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.SelectHolder;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SelectContext<S extends SelectHolder<?>, T extends SqlTables> implements Context {

	S selectHolder;
	SqlIdColumns ids;
	Optional<ColumnDateRange> validityDate;
	T tables;
	ConversionContext conversionContext;

	public static SelectContext<Connector, ConnectorSqlTables> create(
			CQTable cqTable,
			SqlIdColumns ids,
			Optional<ColumnDateRange> validityDate,
			ConnectorSqlTables tables,
			ConversionContext conversionContext
	) {
		return new SelectContext<>(cqTable.getConnector().resolve(), ids, validityDate, tables, conversionContext);
	}

	public static SelectContext<TreeConcept, ConceptSqlTables> create(
			CQConcept cqConcept,
			SqlIdColumns ids,
			Optional<ColumnDateRange> validityDate,
			ConceptSqlTables tables,
			ConversionContext conversionContext
	) {
		if (!(cqConcept.getConcept() instanceof TreeConcept treeConcept)) {
			throw new IllegalArgumentException("Cannot create a select context for a concept that is not a TreeConcept");
		}
		return new SelectContext<>(treeConcept, ids, validityDate, tables, conversionContext);
	}

	public SqlFunctionProvider getFunctionProvider() {
		return getSqlDialect().getFunctionProvider();
	}

}
