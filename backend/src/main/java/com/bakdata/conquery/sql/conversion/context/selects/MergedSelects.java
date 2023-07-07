package com.bakdata.conquery.sql.conversion.context.selects;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.context.step.QueryStep;
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

	Field<Object> primaryColumn;

	Optional<Field<Object>> validityDate;

	/**
	 * A merged list of all select fields, except the primary column and validity date,
	 * of the {@link QueryStep}'s passed to the {@link MergedSelects} constructor.
	 * Each field name is qualified with its associated CTE name.
	 */
	List<Field<Object>> mergedSelects;

	public MergedSelects(List<QueryStep> querySteps) {
		this.primaryColumn = this.coalescePrimaryColumns(querySteps);
		this.validityDate = this.extractValidityDate(querySteps);
		this.mergedSelects = this.mergeSelects(querySteps);
	}

	private Field<Object> coalescePrimaryColumns(List<QueryStep> querySteps) {
		List<Field<Object>> primaryColumns = querySteps.stream()
													   .map(queryStep -> this.mapFieldToQualifier(queryStep.getCteName(), queryStep.getSelects().getPrimaryColumn()))
													   .toList();
		return DSL.coalesce((Object) primaryColumns.get(0), primaryColumns.subList(1, primaryColumns.size()).toArray())
				  .as("primary_column");
	}

	private Optional<Field<Object>> extractValidityDate(List<QueryStep> querySteps) {
		// TODO: date aggregation...
		if (querySteps.isEmpty()) {
			return Optional.empty();
		}
		QueryStep firstQueryStep = querySteps.get(0);
		return this.mapFieldStreamToQualifier(firstQueryStep.getCteName(), firstQueryStep.getSelects().getValidityDate().stream())
				   .findFirst();
	}

	private List<Field<Object>> mergeSelects(List<QueryStep> queriesToJoin) {
		return queriesToJoin.stream()
							.flatMap(queryStep -> queryStep.getSelects().explicitSelects().stream()
														   .map(field -> this.mapFieldToQualifier(queryStep.getCteName(), field)))
							.toList();
	}

	@Override
	public MergedSelects byName(String qualifier) {
		return new MergedSelects(
				this.mapFieldToQualifier(qualifier, this.primaryColumn),
				this.mapFieldStreamToQualifier(qualifier, this.validityDate.stream()).findFirst(),
				this.mergedSelects.stream()
								  .map(field -> this.mapFieldToQualifier(qualifier, field))
								  .toList()
		);
	}

	@Override
	public List<Field<Object>> all() {
		return Stream.concat(
				this.primaryColumnAndValidityDate(),
				this.mergedSelects.stream()
		).toList();
	}

	private Stream<Field<Object>> primaryColumnAndValidityDate() {
		return Stream.concat(
				Stream.of(this.primaryColumn),
				this.validityDate.stream()
		);
	}

	@Override
	public List<Field<Object>> explicitSelects() {
		return this.mergedSelects;
	}

}
