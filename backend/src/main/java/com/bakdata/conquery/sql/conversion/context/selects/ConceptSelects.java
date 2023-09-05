package com.bakdata.conquery.sql.conversion.context.selects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.models.ColumnDateRange;
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
	Optional<ColumnDateRange> dateRestrictionRange;
	Optional<ColumnDateRange> validityDate;
	List<Field<Object>> eventSelect;
	List<Field<Object>> eventFilter;
	List<Field<Object>> groupSelect;
	List<Field<Object>> groupFilter;

	@Override
	public Selects withValidityDate(ColumnDateRange validityDate) {
		return this.toBuilder()
				   .validityDate(Optional.of(validityDate))
				   .build();
	}

	@Override
	public ConceptSelects qualifiedWith(String qualifier) {
		return builder()
				.primaryColumn(this.mapFieldToQualifier(qualifier, this.primaryColumn))
				.dateRestrictionRange(this.dateRestrictionRange.map(dateRestriction -> dateRestriction.qualify(qualifier)))
				.validityDate(this.validityDate.map(validityDate -> validityDate.qualify(qualifier)))
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
				this.validityDate.map(ColumnDateRange::toFields).stream().flatMap(Collection::stream)
		);
	}

	@Override
	public List<Field<Object>> explicitSelects() {

		List<Field<Object>> explicitSelects = new ArrayList<>();

		dateRestrictionRange.ifPresent(columnDateRange -> explicitSelects.addAll(columnDateRange.toFields()));
		explicitSelects.addAll(eventSelect);
		explicitSelects.addAll(eventFilter);
		explicitSelects.addAll(groupSelect);
		explicitSelects.addAll(groupFilter);

		return explicitSelects;
	}

}
