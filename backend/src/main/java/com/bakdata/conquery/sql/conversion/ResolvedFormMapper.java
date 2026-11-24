package com.bakdata.conquery.sql.conversion;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.ResolvedQuerySource;
import com.bakdata.conquery.sql.model.form.*;
import com.bakdata.conquery.sql.model.range.DateRange;
import lombok.RequiredArgsConstructor;

/** Resolves initialized backend form DTOs into the connector-owned form model. */
@RequiredArgsConstructor
public final class ResolvedFormMapper {

	private final ResolvedQueryMapper queryMapper;

	public ResolvedFormQuery map(AbsoluteFormQuery form, ResolvedQuerySource prerequisite) {
		return create(
				prerequisite,
				form.getFeatures().getChildQueries(),
				new ResolvedFormMode.Absolute(
						new DateRange(Optional.ofNullable(form.getDateRange().getMin()),
								Optional.ofNullable(form.getDateRange().getMax())),
						form.getResolutionsAndAlignmentMap()),
				form.getResultInfos()
		);
	}

	public ResolvedFormQuery map(RelativeFormQuery form, ResolvedQuerySource prerequisite) {
		RelativeFormSettings settings = new RelativeFormSettings(
				form.getIndexSelector(),
				form.getIndexPlacement(),
				form.getTimeCountBefore(),
				form.getTimeCountAfter(),
				form.getTimeUnit(),
				form.getResolutionsAndAlignmentMap()
		);
		return create(prerequisite, form.getFeatures().getChildQueries(),
				new ResolvedFormMode.Relative(settings), form.getResultInfos());
	}

	public ResolvedFormQuery map(EntityDateQuery form, ResolvedQuerySource prerequisite) {
		Optional<DateRange> bounds = Optional.ofNullable(form.getDateRange()).map(ResolvedFormMapper::dateRange);
		return create(prerequisite, form.getFeatures().getChildQueries(),
				new ResolvedFormMode.EntityDate(bounds, form.getResolutionsAndAlignments()),
				form.getResultInfos());
	}

	private ResolvedFormQuery create(
			ResolvedQuerySource prerequisite,
			List<ConceptQuery> featureQueries,
			ResolvedFormMode mode,
			List<ResultInfo> resultInfos
	) {
		List<ResolvedQuery> features = featureQueries.stream()
				.map(feature -> queryMapper.map(feature, Optional.empty(), feature.getResultInfos()))
				.toList();
		return new ResolvedFormQuery(
				prerequisite,
				features,
				mode,
				ResolvedOperationAdapter.resultColumns(resultInfos)
		);
	}

	private static DateRange dateRange(CDateRange value) {
		return new DateRange(Optional.ofNullable(value.getMin()), Optional.ofNullable(value.getMax()));
	}
}
