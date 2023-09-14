package com.bakdata.conquery.sql.conversion.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.model.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * {@link ConceptSelects} represent all select fields of a {@link CQConcept}.
 */
@Value
public class ConceptSelects implements Selects {

	Field<Object> primaryColumn;

	Optional<ColumnDateRange> validityDate;

	List<SqlSelect> sqlSelects;

	@Override
	public Selects withValidityDate(ColumnDateRange validityDate) {
		return new ConceptSelects(primaryColumn, Optional.ofNullable(validityDate), sqlSelects);
	}

	@Override
	public Selects qualifiedWith(String qualifier) {
		Field<Object> qualifiedPrimaryColumn = DSL.field(DSL.name(qualifier, this.primaryColumn.getName()));
		Optional<ColumnDateRange> qualifiedValidityDate = validityDate.map(validityDate -> validityDate.qualify(qualifier));
		List<SqlSelect> qualifiedSelects = this.sqlSelects.stream()
														  .map(select -> new ExtractingSqlSelect<>(qualifier, select.select().getName(), Object.class))
														  .collect(Collectors.toList());
		return new ConceptSelects(qualifiedPrimaryColumn, qualifiedValidityDate, qualifiedSelects);
	}

	@Override
	public List<Field<?>> all() {
		return Stream.of(
							 Stream.of(this.primaryColumn),
							 this.validityDate.stream().flatMap(range -> range.toFields().stream()),
							 this.sqlSelects.stream().map(SqlSelect::select)
					 )
					 .flatMap(Function.identity())
					 .map(select -> (Field<Object>) select)
					 .collect(Collectors.toList());
	}

	@Override
	public List<Field<Object>> explicitSelects() {
		return this.sqlSelects.stream()
							  .map(SqlSelect::select)
							  .map(select -> (Field<Object>) select)
							  .collect(Collectors.toList());
	}

}
