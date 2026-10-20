package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqlConverter {

	private final ResolvedQueryAdapter resolvedQueryAdapter;
	private final ResolvedTableExportAdapter tableExportAdapter;
	private final ResolvedFormAdapter formAdapter;
	private final TranslatorDialect dialect;

	public SqlQuery convert(QueryDescription queryDescription) {
		if (queryDescription instanceof ConceptQuery conceptQuery) {
			return resolvedQueryAdapter.translate(conceptQuery, dialect);
		}
		if (queryDescription instanceof SecondaryIdQuery secondaryIdQuery) {
			return resolvedQueryAdapter.translate(secondaryIdQuery, dialect);
		}
		if (queryDescription instanceof TableExportQuery tableExportQuery) {
			return tableExportAdapter.translate(tableExportQuery, dialect);
		}
		if (queryDescription instanceof AbsoluteFormQuery absoluteForm) {
			return formAdapter.translate(absoluteForm, dialect);
		}
		if (queryDescription instanceof RelativeFormQuery relativeForm) {
			return formAdapter.translate(relativeForm, dialect);
		}
		if (queryDescription instanceof EntityDateQuery entityDateForm) {
			return formAdapter.translate(entityDateForm, dialect);
		}
		throw new UnsupportedOperationException(
				"SQL compilation is not implemented for " + queryDescription.getClass());
	}
}
