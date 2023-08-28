package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.Converter;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.step.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.cqelement.CQAndConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQDateRestrictionConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQNegationConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQOrConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CQConceptConverter;
import com.bakdata.conquery.sql.conversion.filter.DateDistanceFilterConverter;
import com.bakdata.conquery.sql.conversion.filter.FilterConversions;
import com.bakdata.conquery.sql.conversion.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.filter.MultiSelectConverter;
import com.bakdata.conquery.sql.conversion.filter.NumberFilterConverter;
import com.bakdata.conquery.sql.conversion.filter.SumConverter;
import com.bakdata.conquery.sql.conversion.query.ConceptQueryConverter;
import com.bakdata.conquery.sql.conversion.select.DateDistanceConverter;
import com.bakdata.conquery.sql.conversion.select.ExistsSelectConverter;
import com.bakdata.conquery.sql.conversion.select.FirstValueConverter;
import com.bakdata.conquery.sql.conversion.select.SelectConversions;
import com.bakdata.conquery.sql.conversion.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.supplier.SystemDateNowSupplier;
import org.jooq.DSLContext;

public interface SqlDialect {

	SystemDateNowSupplier DEFAULT_DATE_NOW_SUPPLIER = new SystemDateNowSupplier();

	SqlFunctionProvider getFunction();

	List<NodeConverter<? extends Visitable>> getNodeConverters();

	private static <R, C extends Converter<?, R, ?>> List<C> customize(List<C> defaults, List<C> substitutes) {
		Map<Class<?>, C> substituteMap = getSubstituteMap(substitutes);
		return defaults.stream()
					   .map(converter -> substituteMap.getOrDefault(converter.getConversionClass(), converter))
					   .collect(Collectors.toList());
	}

	List<SelectConverter<? extends Select>> getSelectConverters();

	DSLContext getDSLContext();

	private static <R, C extends Converter<?, R, ?>> Map<Class<?>, C> getSubstituteMap(List<C> substitutes) {
		return substitutes.stream()
						  .collect(Collectors.toMap(
								  Converter::getConversionClass,
								  Function.identity()
						  ));
	}

	default List<NodeConverter<? extends Visitable>> getDefaultNodeConverters() {
		return List.of(
				new CQDateRestrictionConverter(),
				new CQAndConverter(),
				new CQOrConverter(),
				new CQNegationConverter(),
				new CQConceptConverter(new FilterConversions(getFilterConverters()), new SelectConversions(getSelectConverters())),
				new ConceptQueryConverter(new QueryStepTransformer(getDSLContext()))
		);
	}

	default List<SelectConverter<? extends Select>> customizeSelectConverters(List<SelectConverter<?>> substitutes) {
		return customize(getDefaultSelectConverters(), substitutes);
	}

	List<FilterConverter<?, ?>> getFilterConverters();

	default List<FilterConverter<?, ?>> getDefaultFilterConverters() {
		return List.of(
				new DateDistanceFilterConverter(DEFAULT_DATE_NOW_SUPPLIER),
				new MultiSelectConverter(),
				new NumberFilterConverter(),
				new SumConverter()
		);
	}

	default List<SelectConverter<? extends Select>> getDefaultSelectConverters() {
		return List.of(
				new FirstValueConverter(),
				new DateDistanceConverter(DEFAULT_DATE_NOW_SUPPLIER),
				new ExistsSelectConverter()
		);
	}
}
