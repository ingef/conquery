package com.bakdata.conquery.sql.conversion.query;

import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.forms.managed.EntityDateQuery;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.forms.FormCteStep;
import com.bakdata.conquery.sql.conversion.forms.FormType;
import com.bakdata.conquery.sql.conversion.forms.StratificationTableFactory;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EntityDateQueryConverter implements NodeConverter<EntityDateQuery> {

	private final FormConversionHelper formHelper;

	@Override
	public Class<? extends EntityDateQuery> getConversionClass() {
		return EntityDateQuery.class;
	}

	@Override
	public ConversionContext convert(EntityDateQuery entityDateQuery, ConversionContext context) {

		QueryStep prerequisite = formHelper.convertPrerequisite(entityDateQuery.getQuery(), context);
		QueryStep withOverwrittenValidityDateBounds = overwriteBounds(prerequisite, entityDateQuery, context);
		StratificationTableFactory tableFactory = new StratificationTableFactory(withOverwrittenValidityDateBounds, context);
		QueryStep stratificationTable = tableFactory.createAbsoluteStratificationTable(entityDateQuery.getResolutionsAndAlignments());

		return formHelper.convertForm(
				FormType.ENTITY_DATE,
				stratificationTable,
				entityDateQuery.getFeatures(),
				entityDateQuery.getResultInfos(context.getSqlPrintSettings()),
				context
		);
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
		String unnestCteName = FormCteStep.UNNEST_ENTITY_DATE_CTE.getSuffix();
		QueryStep unnestedEntityDate = functionProvider.unnestDaterange(prerequisite.getSelects().getValidityDate().get(), prerequisite, unnestCteName);
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
