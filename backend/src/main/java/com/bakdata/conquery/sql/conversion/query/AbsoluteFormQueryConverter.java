package com.bakdata.conquery.sql.conversion.query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.forms.FormCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AbsoluteFormQueryConverter implements NodeConverter<AbsoluteFormQuery> {

	private final FormConversionHelper formHelper;

	@Override
	public Class<? extends AbsoluteFormQuery> getConversionClass() {
		return AbsoluteFormQuery.class;
	}

	@Override
	public ConversionContext convert(AbsoluteFormQuery form, ConversionContext context) {
		QueryStep prerequisite = convertPrerequisite(form.getQuery(), form.getDateRange(), context);
		return formHelper.convertForm(
				prerequisite,
				form.getResolutionsAndAlignmentMap(),
				form.getFeatures(),
				form.getResultInfos(),
				context
		);
	}

	private static QueryStep convertPrerequisite(Query query, Range<LocalDate> formDateRange, ConversionContext context) {

		ConversionContext withConvertedPrerequisite = context.getNodeConversions().convert(query, context);
		Preconditions.checkArgument(withConvertedPrerequisite.getQuerySteps().size() == 1, "Base query conversion should produce exactly 1 QueryStep");
		QueryStep convertedPrerequisite = withConvertedPrerequisite.getQuerySteps().get(0);

		ColumnDateRange bounds = context.getSqlDialect()
										.getFunctionProvider()
										.forCDateRange(CDateRange.of(formDateRange)).as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());

		Selects prerequisiteSelects = convertedPrerequisite.getQualifiedSelects();
		// we only keep the primary column for the upcoming form
		Selects selects = Selects.builder()
								 .ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
								 .stratificationDate(Optional.of(bounds))
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.EXTRACT_IDS.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(convertedPrerequisite.getCteName()))
						.groupBy(selects.getIds().toFields()) // group by primary column to ensure max. 1 entry per subject
						.predecessors(List.of(convertedPrerequisite))
						.build();
	}

}
