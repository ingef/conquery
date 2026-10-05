package com.bakdata.conquery.sql.conversion;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.models.forms.managed.RelativeFormQuery;
import com.bakdata.conquery.sql.model.ResolvedQuery;
import com.bakdata.conquery.sql.model.form.FormAlignment;
import com.bakdata.conquery.sql.model.form.FormCalendarUnit;
import com.bakdata.conquery.sql.model.form.FormIndexPlacement;
import com.bakdata.conquery.sql.model.form.FormIndexSelector;
import com.bakdata.conquery.sql.model.form.FormResolution;
import com.bakdata.conquery.sql.model.form.RelativeFormSettings;
import com.bakdata.conquery.sql.model.form.ResolutionAndAlignment;
import com.bakdata.conquery.sql.model.form.ResolvedFormMode;
import com.bakdata.conquery.sql.model.form.ResolvedFormQuery;
import com.bakdata.conquery.sql.model.range.DateRange;

/** Resolves initialized backend form DTOs into the connector-owned form model. */
public final class ResolvedFormMapper {

	private final ResolvedQueryMapper queryMapper;

	public ResolvedFormMapper(ResolvedQueryMapper queryMapper) {
		this.queryMapper = queryMapper;
	}

	public ResolvedFormQuery map(AbsoluteFormQuery form) {
		return create(
				form.getQuery(),
				form.getFeatures().getChildQueries(),
				new ResolvedFormMode.Absolute(
						new DateRange(Optional.ofNullable(form.getDateRange().getMin()),
								Optional.ofNullable(form.getDateRange().getMax())),
						resolutions(form.getResolutionsAndAlignmentMap())),
				form.getResultInfos()
		);
	}

	public ResolvedFormQuery map(RelativeFormQuery form) {
		RelativeFormSettings settings = new RelativeFormSettings(
				FormIndexSelector.valueOf(form.getIndexSelector().name()),
				FormIndexPlacement.valueOf(form.getIndexPlacement().name()),
				form.getTimeCountBefore(),
				form.getTimeCountAfter(),
				FormCalendarUnit.valueOf(form.getTimeUnit().name()),
				resolutions(form.getResolutionsAndAlignmentMap())
		);
		return create(form.getQuery(), form.getFeatures().getChildQueries(),
				new ResolvedFormMode.Relative(settings), form.getResultInfos());
	}

	public ResolvedFormQuery map(EntityDateQuery form) {
		Optional<DateRange> bounds = Optional.ofNullable(form.getDateRange()).map(ResolvedFormMapper::dateRange);
		return create(form.getQuery(), form.getFeatures().getChildQueries(),
				new ResolvedFormMode.EntityDate(bounds, resolutions(form.getResolutionsAndAlignments())),
				form.getResultInfos());
	}

	private ResolvedFormQuery create(
			com.bakdata.conquery.apiv1.query.Query prerequisite,
			List<ConceptQuery> featureQueries,
			ResolvedFormMode mode,
			List<com.bakdata.conquery.models.query.resultinfo.ResultInfo> resultInfos
	) {
		ResolvedQuery resolvedPrerequisite = queryMapper.map(prerequisite, List.of());
		List<ResolvedQuery> features = featureQueries.stream()
				.map(feature -> queryMapper.map(feature, Optional.empty(), feature.getResultInfos()))
				.toList();
		return new ResolvedFormQuery(
				resolvedPrerequisite,
				features,
				mode,
				ResolvedOperationAdapter.resultColumns(resultInfos)
		);
	}

	private static List<ResolutionAndAlignment> resolutions(
			List<ExportForm.ResolutionAndAlignment> values
	) {
		return values.stream().map(value -> new ResolutionAndAlignment(
				FormResolution.valueOf(value.getResolution().name()),
				FormAlignment.valueOf(value.getAlignment().name()))).toList();
	}

	private static DateRange dateRange(CDateRange value) {
		return new DateRange(Optional.ofNullable(value.getMin()), Optional.ofNullable(value.getMax()));
	}
}
