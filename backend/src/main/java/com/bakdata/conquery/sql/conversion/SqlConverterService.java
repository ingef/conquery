package com.bakdata.conquery.sql.conversion;

import java.util.List;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.context.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.CQAndConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQConceptConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQDateRestrictionConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQOrConverter;
import com.bakdata.conquery.sql.conversion.filter.FilterConverterService;
import com.bakdata.conquery.sql.conversion.query.ConceptQueryConverter;
import com.bakdata.conquery.sql.conversion.select.SelectConverterService;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

/**
 * Entry point for converting {@link QueryDescription} to an SQL query.
 */
public class SqlConverterService extends ConverterService<Visitable, ConversionContext> {

	private static final List<NodeConverter<? extends Visitable>> converters = List.of(
			new CQDateRestrictionConverter(),
			new CQAndConverter(),
			new CQConceptConverter(new FilterConverterService(), new SelectConverterService()),
			new ConceptQueryConverter(),
			new CQOrConverter()
	);

	public SqlConverterService() {
		super(converters);
	}

	public String convert(QueryDescription queryDescription) {
		ConversionContext initialCtx = ConversionContext.builder()
														.dslContext(DSL.using(SQLDialect.POSTGRES))
														.sqlConverterService(this)
														.build();
		ConversionContext resultCtx = convert(queryDescription, initialCtx);
		return resultCtx.getQuery().getSQL(ParamType.INLINED);
	}

}
