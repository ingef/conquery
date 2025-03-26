package com.bakdata.conquery.sql.conversion.model;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.jooq.Field;

@Value
@Builder(toBuilder = true)
public class Selects {

	SqlIdColumns ids;
	@Builder.Default
	Optional<ColumnDateRange> validityDate = Optional.empty();
	@Builder.Default
	Optional<ColumnDateRange> stratificationDate = Optional.empty();
	@Singular
	List<SqlSelect> sqlSelects;

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
		SqlIdColumns ids = this.ids.qualify(qualifier);
		List<SqlSelect> sqlSelects = this.sqlSelects.stream().map(sqlSelect -> sqlSelect.qualify(qualifier)).collect(Collectors.toList());

		SelectsBuilder builder = Selects.builder()
										.ids(ids)
										.sqlSelects(sqlSelects);

		if (this.validityDate.isPresent()) {
			builder = builder.validityDate(this.validityDate.map(_validityDate -> _validityDate.qualify(qualifier)));
		}

		if (this.stratificationDate.isPresent()) {
			builder = builder.stratificationDate(this.stratificationDate.map(_validityDate -> _validityDate.qualify(qualifier)));
		}

		return builder.build();
	}

	public List<Field<?>> all() {
		return Stream.of(
							 this.ids.toFields().stream(),
							 this.stratificationDate.stream().flatMap(range -> range.toFields().stream()),
							 this.validityDate.stream().flatMap(range -> range.toFields().stream()),
							 this.sqlSelects.stream().flatMap(sqlSelect -> sqlSelect.toFields().stream())
					 )
					 .flatMap(Function.identity())
					 .map(select -> (Field<?>) select)
					 .distinct()
					 .collect(Collectors.toList());
	}

	/**
	 * All {@link Select}s that have not been explicitly selected (IDs, validity/stratification dates).
	 */
	public List<Field<?>> nonExplicitSelects() {
		return Stream.of(
							this.ids.toFields().stream(),
							this.stratificationDate.stream().flatMap(range -> range.toFields().stream()),
							this.validityDate.stream().flatMap(range -> range.toFields().stream())
					 )
					 .flatMap(Function.identity())
					 .map(select -> (Field<?>) select)
					 .distinct()
					 .collect(Collectors.toList());
	}

	/**
	 * All explicitly selected and converted {@link Select}s.
	 */
	public List<Field<?>> explicitSelects() {
		return this.sqlSelects.stream()
							  .flatMap(sqlSelect -> sqlSelect.toFields().stream())
							  .distinct()
							  .collect(Collectors.toList());
	}

}
