package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqlConverter {

	private final ResolvedQueryAdapter resolvedQueryAdapter;
	private final ResolvedTableExportAdapter tableExportAdapter;
	private final ResolvedFormAdapter formAdapter;
	private final TranslatorDialect dialect;

	public SqlQuery convert(QueryDescription queryDescription) {
		return switch (queryDescription) {
			case ConceptQuery conceptQuery -> resolvedQueryAdapter.translate(conceptQuery, dialect);
			case SecondaryIdQuery secondaryIdQuery -> resolvedQueryAdapter.translate(secondaryIdQuery, dialect);
			case TableExportQuery tableExportQuery -> tableExportAdapter.translate(tableExportQuery, dialect);
			case AbsoluteFormQuery absoluteForm -> formAdapter.translate(absoluteForm, dialect);
			case RelativeFormQuery relativeForm -> formAdapter.translate(relativeForm, dialect);
			case EntityDateQuery entityDateForm -> formAdapter.translate(entityDateForm, dialect);
			default -> throw new UnsupportedOperationException(
					"SQL compilation is not implemented for " + queryDescription.getClass());
		};
	}
}
