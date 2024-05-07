package com.bakdata.conquery.sql.conversion.query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.FormCteStep;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@RequiredArgsConstructor
public class EntityDateQueryConverter implements NodeConverter<EntityDateQuery> {

	private final FormConversionHelper formHelper;

	@Override
	public Class<? extends EntityDateQuery> getConversionClass() {
		return EntityDateQuery.class;
	}

	@Override
	public ConversionContext convert(EntityDateQuery entityDateQuery, ConversionContext context) {

		QueryStep prerequisite = convertPrerequisite(entityDateQuery.getQuery(), context);
		QueryStep withOverwrittenValidityDateBounds = overwriteBounds(prerequisite, entityDateQuery, context);

		return formHelper.convertForm(
				withOverwrittenValidityDateBounds,
				entityDateQuery.getResolutionsAndAlignments(),
				entityDateQuery.getFeatures(),
				entityDateQuery.getResultInfos(),
				context
		);
	}

	private static QueryStep convertPrerequisite(Query query, ConversionContext context) {

		ConversionContext withConvertedPrerequisite = context.getNodeConversions().convert(query, context);
		Preconditions.checkArgument(withConvertedPrerequisite.getQuerySteps().size() == 1, "Base query conversion should produce exactly 1 QueryStep");
		QueryStep convertedPrerequisite = withConvertedPrerequisite.getQuerySteps().get(0);

		Selects prerequisiteSelects = convertedPrerequisite.getQualifiedSelects();
		// we keep the primary column and the validity date
		Selects selects = Selects.builder()
								 .ids(new SqlIdColumns(prerequisiteSelects.getIds().getPrimaryColumn()))
								 .validityDate(prerequisiteSelects.getValidityDate())
								 .build();

		// we want to keep each primary column and the corresponding distinct validity date ranges
		List<Field<?>> groupByFields = Stream.concat(
													 Stream.of(prerequisiteSelects.getIds().getPrimaryColumn()),
													 prerequisiteSelects.getValidityDate().stream().flatMap(validityDate -> validityDate.toFields().stream())
											 )
											 .collect(Collectors.toList());

		// filter out entries with a null validity date
		Condition dateNotNullCondition = prerequisiteSelects.getValidityDate().get().isNotNull();

		return QueryStep.builder()
						.cteName(FormCteStep.EXTRACT_IDS.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(convertedPrerequisite.getCteName()))
						.conditions(List.of(dateNotNullCondition))
						.groupBy(groupByFields)
						.predecessors(List.of(convertedPrerequisite))
						.build();
	}

	/**
	 * Computes the intersection of the entity date and the entity date query's daterange.
	 */
	private static QueryStep overwriteBounds(QueryStep prerequisite, EntityDateQuery entityDateQuery, ConversionContext context) {

		Preconditions.checkArgument(
				prerequisite.getQualifiedSelects().getValidityDate().isPresent(),
				"Expecting the prerequisite step's Selects to contain a validity date"
		);

		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();
		// we want to create a stratification for each distinct validity date range of an entity,
		// so we first need to unnest the validity date in case it is a multirange
		QueryStep unnestedEntityDate = functionProvider.unnestValidityDate(prerequisite, FormCteStep.UNNEST_ENTITY_DATE_CTE.getSuffix());
		Selects unnestedSelects = unnestedEntityDate.getQualifiedSelects();

		ColumnDateRange withOverwrittenBounds;
		if (entityDateQuery.getDateRange() == null) {
			withOverwrittenBounds = unnestedSelects.getValidityDate().get();
		}
		else {
			ColumnDateRange formDateRange = functionProvider.forCDateRange(entityDateQuery.getDateRange());
			ColumnDateRange entityDate = unnestedSelects.getValidityDate().get();
			withOverwrittenBounds = functionProvider.intersection(formDateRange, entityDate)
													.as(SharedAliases.STRATIFICATION_BOUNDS.getAlias());
		}

		Selects selects = Selects.builder()
								 .ids(unnestedSelects.getIds())
								 .stratificationDate(Optional.of(withOverwrittenBounds))
								 .build();

		List<QueryStep> predecessors;
		// unnest step is optional depending on the dialect
		if (unnestedEntityDate == prerequisite) {
			predecessors = List.of(prerequisite);
		}
		else {
			predecessors = List.of(prerequisite, unnestedEntityDate);
		}

		return QueryStep.builder()
						.cteName(FormCteStep.OVERWRITE_BOUNDS.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(unnestedEntityDate.getCteName()))
						.predecessors(predecessors)
						.build();
	}

}
