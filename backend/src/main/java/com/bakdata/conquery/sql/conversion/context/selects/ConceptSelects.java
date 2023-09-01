package com.bakdata.conquery.sql.conversion.context.selects;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.ConquerySelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.model.select.ExtractingSelect;
import com.bakdata.conquery.sql.models.ColumnDateRange;
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

	List<ConquerySelect> conquerySelects;

	@Override
	public Selects withValidityDate(ColumnDateRange validityDate) {
		return new ConceptSelects(primaryColumn, Optional.ofNullable(validityDate), conquerySelects);
	}

	@Override
	public Selects qualifiedWith(String qualifier) {
		Field<Object> qualifiedPrimaryColumn = DSL.field(DSL.name(qualifier, this.primaryColumn.getName()));
		Optional<ColumnDateRange> qualifiedValidityDate = validityDate.map(validityDate -> validityDate.qualify(qualifier));
		List<ConquerySelect> qualifiedSelects = this.conquerySelects.stream()
																	.map(select -> new ExtractingSelect<>(qualifier, select.select().getName(), Object.class))
																	.collect(Collectors.toList());
		return new ConceptSelects(qualifiedPrimaryColumn, qualifiedValidityDate, qualifiedSelects);
	}

	@Override
	public List<Field<Object>> all() {
		return Stream.of(
							 Stream.of(this.primaryColumn),
							 this.validityDate.stream().flatMap(range -> range.toFields().stream()),
							 this.conquerySelects.stream().map(ConquerySelect::select)
					 )
					 .flatMap(Function.identity())
					 .map(select -> (Field<Object>) select)
					 .collect(Collectors.toList());
	}

	@Override
	public List<Field<Object>> explicitSelects() {
		return this.conquerySelects.stream()
								   .map(ConquerySelect::select)
								   .map(select -> (Field<Object>) select)
								   .collect(Collectors.toList());
	}

}
