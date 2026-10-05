package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.LegacyCompilerDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

public class SqlConverter {

	private final NodeConversions nodeConversions;
	private final ConqueryConfig config;
	private final ResolvedQueryAdapter resolvedQueryAdapter;
	private final ResolvedTableExportAdapter tableExportAdapter;
	private final ResolvedFormAdapter formAdapter;
	private final LegacyCompilerDialect dialect;

	public SqlConverter(
			NodeConversions nodeConversions,
			ConqueryConfig config,
			ResolvedQueryAdapter resolvedQueryAdapter,
			ResolvedTableExportAdapter tableExportAdapter,
			ResolvedFormAdapter formAdapter,
			LegacyCompilerDialect dialect
	) {
		this.nodeConversions = nodeConversions;
		this.config = config;
		this.resolvedQueryAdapter = resolvedQueryAdapter;
		this.tableExportAdapter = tableExportAdapter;
		this.formAdapter = formAdapter;
		this.dialect = dialect;
	}

	public SqlQuery convert(QueryDescription queryDescription) {
		if (queryDescription instanceof ConceptQuery conceptQuery) {
			return resolvedQueryAdapter.compile(conceptQuery, dialect);
		}
		if (queryDescription instanceof SecondaryIdQuery secondaryIdQuery) {
			return resolvedQueryAdapter.compile(secondaryIdQuery, dialect);
		}
		if (queryDescription instanceof TableExportQuery tableExportQuery) {
			return tableExportAdapter.compile(tableExportQuery, dialect);
		}
		if (queryDescription instanceof AbsoluteFormQuery absoluteForm) {
			return formAdapter.compile(absoluteForm, dialect);
		}
		if (queryDescription instanceof RelativeFormQuery relativeForm) {
			return formAdapter.compile(relativeForm, dialect);
		}
		if (queryDescription instanceof EntityDateQuery entityDateForm) {
			return formAdapter.compile(entityDateForm, dialect);
		}
		ConversionContext converted = nodeConversions.convert(queryDescription, config);
		return converted.getFinalQuery();
	}
}
