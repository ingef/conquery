package com.bakdata.conquery.sql.conversion.context.selects;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
import com.bakdata.conquery.sql.models.ColumnDateRange;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * {@link MergedSelects} represent the combination of multiple {@link Selects}.
 * Default selects fields of multiple {@link Selects} will be merged and special select fields like the primary column
 * or validity dates will be unified or aggregated due to defined policies.
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MergedSelects implements Selects {

	public static final String PRIMARY_COLUMN_NAME = "primary_column";
	Field<Object> primaryColumn;

	/**
	 * An aggregated validity date of all validity dates of each {@link QueryStep} passed to the {@link MergedSelects} constructor.
	 */
	Optional<ColumnDateRange> validityDate;

	/**
	 * A merged list of all select fields, except the primary column and validity date,
	 * of the {@link QueryStep}'s passed to the {@link MergedSelects} constructor.
	 * Each field name is qualified with its associated CTE name.
	 */
	List<Field<Object>> mergedSelects;

	public MergedSelects(List<QueryStep> querySteps) {
		this.primaryColumn = this.coalescePrimaryColumns(querySteps);
		this.validityDate = this.extractValidityDates(querySteps);
		this.mergedSelects = this.mergeSelects(querySteps);
	}

	@Override
	public Selects withValidityDate(ColumnDateRange validityDate) {
		return new MergedSelects(
				this.primaryColumn,
				Optional.of(validityDate),
				this.mergedSelects
		);
	}

	@Override
	public MergedSelects qualifiedWith(String cteName) {
		return new MergedSelects(
				this.mapFieldToQualifier(cteName, this.primaryColumn),
				this.validityDate.map(columnDateRange -> columnDateRange.qualify(cteName)),
				this.mapFieldStreamToQualifier(cteName, this.mergedSelects.stream()).toList()
		);
	}

	@Override
	public List<Field<?>> all() {
		return Stream.concat(
				this.primaryColumnAndValidityDate(),
				this.mergedSelects.stream()
		).toList();
	}

	@Override
	public List<Field<Object>> explicitSelects() {
		return this.mergedSelects;
	}

	private Field<Object> coalescePrimaryColumns(List<QueryStep> querySteps) {
		List<Field<Object>> primaryColumns = querySteps.stream()
													   .map(queryStep -> queryStep.getQualifiedSelects().getPrimaryColumn())
													   .toList();
		return DSL.coalesce((Object) primaryColumns.get(0), primaryColumns.subList(1, primaryColumns.size()).toArray())
				  .as(PRIMARY_COLUMN_NAME);
	}

	private Optional<ColumnDateRange> extractValidityDates(List<QueryStep> querySteps) {
		// TODO: date aggregation...
		return querySteps.stream()
						 .filter(queryStep -> queryStep.getSelects().getValidityDate().isPresent())
						 .map(queryStep -> queryStep.getQualifiedSelects().getValidityDate().get())
						 .findFirst();
	}

	private List<Field<Object>> mergeSelects(List<QueryStep> queriesToJoin) {
		return queriesToJoin.stream()
							.flatMap(queryStep -> queryStep.getQualifiedSelects().explicitSelects().stream())
							// prevents naming collisions of alike-named selects in final select statement
							.map(field -> field.as("%s-%s".formatted(field.getName(), field.hashCode())))
							.toList();
	}

	private Stream<Field<?>> primaryColumnAndValidityDate() {
		return Stream.concat(
				Stream.of(this.primaryColumn),
				this.validityDate.map(ColumnDateRange::toFields).stream().flatMap(Collection::stream)
		);
	}

}
