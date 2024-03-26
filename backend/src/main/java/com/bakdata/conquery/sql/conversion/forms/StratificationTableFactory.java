package com.bakdata.conquery.sql.conversion.forms;

import java.time.LocalDate;
import java.time.Month;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.sql.conversion.SharedAliases;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Getter(AccessLevel.PROTECTED)
@RequiredArgsConstructor
public abstract class StratificationTableFactory {

	private final QueryStep baseStep;
	private final SqlFunctionProvider functionProvider;
	private final NameGenerator nameGenerator;

	protected StratificationTableFactory(QueryStep baseStep, ConversionContext context) {
		this.baseStep = baseStep;
		this.functionProvider = context.getSqlDialect().getFunctionProvider();
		this.nameGenerator = context.getNameGenerator();
	}

	public static StratificationTableFactory create(Dialect dialect, QueryStep baseStep, ConversionContext context) {
		return switch (dialect) {
			case POSTGRESQL -> new PostgresStratificationTableFactory(baseStep, context);
			case HANA -> new HanaStratificationTableFactory(baseStep, context);
		};
	}

	public QueryStep createStratificationTable(AbsoluteFormQuery form) {
		List<QueryStep> tables = form.getResolutionsAndAlignmentMap().stream()
									 .map(ExportForm.ResolutionAndAlignment::getResolution)
									 .map(resolution -> createResolutionTable(form.getDateRange(), resolution))
									 .toList();
		return unionResolutionTables(tables, getBaseStep());
	}

	/**
	 * True if there is an entity date in the base step. Compared to absolute mode, stratification window is bound by the entity date of each subject.
	 */
	protected boolean isEntityDateStratification() {
		return baseStep.getSelects().getValidityDate().isPresent();
	}

	protected QueryStep unionResolutionTables(List<QueryStep> unionSteps, QueryStep baseStep) {

		Preconditions.checkArgument(!unionSteps.isEmpty(), "Expecting at least 1 resolution table");

		Iterator<QueryStep> iterator = unionSteps.iterator();
		QueryStep lastResolutionTable = iterator.next();
		while (iterator.hasNext()) {
			lastResolutionTable = iterator.next()
										  .toBuilder()
										  .predecessor(lastResolutionTable)
										  .build();
		}

		List<QueryStep> withQualifiedSelects = unionSteps.stream()
														 .map(queryStep -> QueryStep.builder()
																					.selects(queryStep.getQualifiedSelects())
																					.fromTable(QueryStep.toTableLike(queryStep.getCteName()))
																					.build())
														 .toList();

		return QueryStep.createUnionStep(withQualifiedSelects, FormCteStep.FULL_STRATIFICATION.getSuffix(), List.of(baseStep, lastResolutionTable));
	}

	protected QueryStep createResolutionTable(Range<LocalDate> formDateRestriction, Resolution resolution) {
		return switch (resolution) {
			case COMPLETE -> createCompleteTable(formDateRestriction);
			case YEARS, QUARTERS -> createIntervalTable(formDateRestriction, resolution);
			case DAYS -> throw new UnsupportedOperationException("Resolution days not supported yet");
		};
	}

	protected QueryStep createCompleteTable(Range<LocalDate> formDateRestriction) {

		Selects baseStepSelects = baseStep.getQualifiedSelects();

		// complete range shall have a null index because it spans the complete range, but we set it to 1 to ensure we can join tables on index,
		// because a condition involving null in a join (e.g., null = some_value or null = null) always evaluates to false
		Field<Integer> index = DSL.field(DSL.val(1, Integer.class)).as(SharedAliases.INDEX.getAlias());
		SqlIdColumns ids = baseStepSelects.getIds().withAbsoluteStratification(Resolution.COMPLETE, index);

		ColumnDateRange completeRange;
		if (isEntityDateStratification()) {
			completeRange = baseStepSelects.getValidityDate().get();
		}
		// otherwise the complete range is the form's date range (absolute form)
		else {
			completeRange = functionProvider.forCDateRange(CDateRange.of(formDateRestriction)).as(SharedAliases.STRATIFICATION_RANGE.getAlias());
		}

		Selects selects = Selects.builder()
								 .ids(ids)
								 .stratificationDate(Optional.of(completeRange))
								 .build();

		return QueryStep.builder()
						.cteName(FormCteStep.COMPLETE.getSuffix())
						.selects(selects)
						.fromTable(QueryStep.toTableLike(baseStep.getCteName()))
						.build();
	}

	protected abstract QueryStep createIntervalTable(Range<LocalDate> formDateRestriction, Resolution resolution);

	protected Field<Integer> indexField(SqlIdColumns ids) {

		List<Field<?>> partitioningFields =
				Stream.concat(
							  ids.toFields().stream(),
							  getBaseStep().getSelects().getValidityDate().stream().flatMap(validityDate -> validityDate.toFields().stream())
					  )
					  .collect(Collectors.toList());

		return DSL.rowNumber()
				  .over(DSL.partitionBy(partitioningFields))
				  .as(SharedAliases.INDEX.getAlias());
	}

	protected static String toResolutionExpression(Resolution resolution) {
		return switch (resolution) {
			case QUARTERS -> "3 month";
			case YEARS -> "1 year";
			case COMPLETE -> throw new UnsupportedOperationException("Generating series for a complete stratification range is not necessary");
			default -> throw new UnsupportedOperationException("Resolution %s currently not supported".formatted(resolution));
		};
	}

	/**
	 * Converts the given date range according to the given resolution to a range that is suited for a dateset-generating SQL function.
	 * <p>
	 * Example: Given  the date range [2012-06-16,2013-01-17] and the resolution YEARS, we cant pipe this directly into SAP HANA's SERIES_GENERATE_DATE command.
	 * Because it does not span the range of a whole year from start and end, the SERIES_GENERATE_DATE command would create an empty set. Instead, we have to
	 * jump to the start of the year of the start date and to the year + 1 of the end date. This will generate the expected series of
	 * <pre>
	 * <table>
	 *   <tr>
	 *     <th>GENERATED_PERIOD_START</th>
	 *     <th>GENERATED_PERIOD_END</th>
	 *   </tr>
	 *   <tr>
	 *     <td>2012-01-01</td>
	 *     <td>2013-01-01</td>
	 *   </tr>
	 *   <tr>
	 *     <td>2013-01-01</td>
	 *     <td>2014-01-01</td>
	 *   </tr>
	 * </table>
	 * </pre>
	 */
	protected Range<LocalDate> toGenerateSeriesBounds(Range<LocalDate> formDateRestriction, Resolution resolution) {
		return switch (resolution) {
			case COMPLETE -> formDateRestriction; // no adjustment necessary
			case YEARS -> Range.of(getYearStart(formDateRestriction.getMin()), getYearEnd(formDateRestriction.getMax()));
			case QUARTERS -> Range.of(getQuarterStart(formDateRestriction.getMin()), getQuarterEnd(formDateRestriction.getMax()));
			case DAYS -> throw new UnsupportedOperationException("DAYS resolution not supported yet");
		};
	}

	private static LocalDate getYearStart(LocalDate date) {
		return LocalDate.of(date.getYear(), Month.JANUARY, 1);
	}

	private static LocalDate getYearEnd(LocalDate date) {
		return LocalDate.of(date.getYear() + 1, Month.JANUARY, 1);
	}

	private static LocalDate getQuarterStart(LocalDate date) {

		Month startMonth = switch (date.getMonthValue()) {
			case 1, 2, 3 -> Month.JANUARY;
			case 4, 5, 6 -> Month.APRIL;
			case 7, 8, 9 -> Month.JULY;
			default -> Month.OCTOBER;
		};

		return LocalDate.of(date.getYear(), startMonth, 1);
	}

	private static LocalDate getQuarterEnd(LocalDate date) {

		int year = date.getYear();
		Month startMonth;

		switch (date.getMonthValue()) {
			case 1, 2, 3:
				startMonth = Month.APRIL; // Start of Q2
				break;
			case 4, 5, 6:
				startMonth = Month.JULY; // Start of Q3
				break;
			case 7, 8, 9:
				startMonth = Month.OCTOBER; // Start of Q4
				break;
			default:
				// For Q4, increment the year and set month to January
				startMonth = Month.JANUARY;
				year++;
				break;
		}

		return LocalDate.of(year, startMonth, 1);
	}

}
