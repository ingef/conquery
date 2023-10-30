package com.bakdata.conquery.sql.conversion.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.select.ExplicitSelect;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Value;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Value
@Builder(toBuilder = true)
public class Selects {

	Field<Object> primaryColumn;
	@Builder.Default
	Optional<ColumnDateRange> validityDate = Optional.empty();
	@Builder.Default
	List<ExplicitSelect> explicitSelects = Collections.emptyList();
	@Builder.Default
	List<SqlSelect> sqlSelects = Collections.emptyList();

	public Selects withValidityDate(ColumnDateRange validityDate) {
		return this.toBuilder()
				   .validityDate(Optional.of(validityDate))
				   .build();
	}

	public Selects blockValidityDate() {
		return this.toBuilder()
				   .validityDate(Optional.empty())
				   .build();
	}

	public Selects qualify(String qualifier) {
		Field<Object> qualifiedPrimaryColumn = DSL.field(DSL.name(qualifier, this.primaryColumn.getName()));
		List<SqlSelect> sqlSelects = this.sqlSelects.stream()
													.flatMap(sqlSelect -> sqlSelect.createReferences(qualifier, SqlSelect.class).stream())
													.distinct()
													.collect(Collectors.toList());
		List<ExplicitSelect> explicitSelects = this.explicitSelects.stream()
																   .flatMap(sqlSelect -> sqlSelect.createReferences(qualifier, ExplicitSelect.class).stream())
																   .collect(Collectors.toList());

		SelectsBuilder builder = this.toBuilder()
									 .primaryColumn(qualifiedPrimaryColumn)
									 .sqlSelects(sqlSelects)
									 .explicitSelects(explicitSelects);

		if (this.validityDate.isPresent()) {
			builder = builder.validityDate(this.validityDate.map(_validityDate -> _validityDate.qualify(qualifier)));
		}

		return builder.build();
	}

	public List<Field<?>> all() {
		return Stream.of(
							 Stream.of(this.primaryColumn),
							 this.validityDate.stream().flatMap(range -> range.toFields().stream()),
							 this.sqlSelects.stream().map(SqlSelect::select),
							 this.explicitSelects.stream().map(SqlSelect::select)
					 )
					 .flatMap(Function.identity())
					 .map(select -> (Field<?>) select)
					 .collect(Collectors.toList());
	}

	public List<Field<?>> explicitSelects() {
		return this.explicitSelects.stream()
								   .map(SqlSelect::select)
								   .collect(Collectors.toList());
	}

}
