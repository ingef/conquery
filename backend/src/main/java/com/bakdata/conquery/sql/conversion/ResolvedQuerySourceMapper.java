package com.bakdata.conquery.sql.conversion;

import java.util.List;

import com.bakdata.conquery.apiv1.query.ArrayConceptQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.sql.model.ResolvedQuerySource;
import com.bakdata.conquery.sql.model.export.ResolvedTableExportQuery;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import lombok.RequiredArgsConstructor;

/** Recursively resolves every backend query shape that can be used as a SQL prerequisite. */
@RequiredArgsConstructor
public final class ResolvedQuerySourceMapper {

	private final ResolvedQueryMapper queryMapper;
	private final ResolvedFormMapper formMapper;
	private final ResolvedTableExportMapper tableExportMapper;

	public ResolvedQuerySource map(Query query) {
		return switch (query) {
			case ConceptQuery concept -> new ResolvedQuerySource.Standard(queryMapper.map(concept, List.of()));
			case SecondaryIdQuery secondary -> new ResolvedQuerySource.Standard(queryMapper.map(secondary, List.of()));
			case ArrayConceptQuery array -> mapArray(array);
			case TableExportQuery export -> new ResolvedQuerySource.TableExport(mapTableExport(export));
			case AbsoluteFormQuery form -> new ResolvedQuerySource.Form(mapForm(form));
			case RelativeFormQuery form -> new ResolvedQuerySource.Form(mapForm(form));
			case EntityDateQuery form -> new ResolvedQuerySource.Form(mapForm(form));
			default -> throw new UnsupportedOperationException(
					"Resolved SQL query-source mapping is not implemented for " + query.getClass());
		};
	}

	private ResolvedQuerySource.Union mapArray(ArrayConceptQuery array) {
		List<ResolvedQuerySource> children = array.getChildQueries().stream().map(this::map).toList();
		return new ResolvedQuerySource.Union(
				children.getFirst().entitySchema(),
				children,
				array.getResolvedDateAggregationMode() != DateAggregationMode.NONE);
	}

	public ResolvedFormQuery mapForm(AbsoluteFormQuery query) {
		return formMapper.map(query, map(query.getQuery()));
	}

	public ResolvedFormQuery mapForm(RelativeFormQuery query) {
		return formMapper.map(query, map(query.getQuery()));
	}

	public ResolvedFormQuery mapForm(EntityDateQuery query) {
		return formMapper.map(query, map(query.getQuery()));
	}

	public ResolvedTableExportQuery mapTableExport(TableExportQuery query) {
		return tableExportMapper.map(query, map(query.getQuery()));
	}
}
