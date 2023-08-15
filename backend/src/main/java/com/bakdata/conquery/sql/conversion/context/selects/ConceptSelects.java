package com.bakdata.conquery.sql.conversion.context.selects;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jooq.Field;

/**
 * {@link ConceptSelects} represent all select fields of a {@link CQConcept}.
 */
@Value
@With
@Builder(toBuilder = true)
public class ConceptSelects implements Selects {

	Field<Object> primaryColumn;
	Optional<Field<Object>> dateRestriction;
	Optional<Field<Object>> validityDate;
	List<Field<Object>> eventSelect;
	List<Field<Object>> eventFilter;
	List<Field<Object>> groupSelect;
	List<Field<Object>> groupFilter;

	@Override
	public ConceptSelects byName(String qualifier) {
		return builder()
				.primaryColumn(this.mapFieldToQualifier(qualifier, this.primaryColumn))
				.dateRestriction(this.mapFieldStreamToQualifier(qualifier, this.dateRestriction.stream()).findFirst())
				.validityDate(this.mapFieldStreamToQualifier(qualifier, this.validityDate.stream()).findFirst())
				.eventSelect(this.mapFieldStreamToQualifier(qualifier, this.eventSelect.stream()).toList())
				.eventFilter(this.mapFieldStreamToQualifier(qualifier, this.eventFilter.stream()).toList())
				.groupSelect(this.mapFieldStreamToQualifier(qualifier, this.groupSelect.stream()).toList())
				.groupFilter(this.mapFieldStreamToQualifier(qualifier, this.groupFilter.stream()).toList())
				.build();
	}

	@Override
	public List<Field<Object>> all() {
		return Stream.concat(
				this.primaryColumnAndValidityDate(),
				this.explicitSelects().stream()
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
		return Stream.of(
				this.dateRestriction.stream(),
				this.eventSelect.stream(),
				this.eventFilter.stream(),
				this.groupSelect.stream(),
				this.groupFilter.stream()
		).flatMap(Function.identity()).toList();
	}

}
