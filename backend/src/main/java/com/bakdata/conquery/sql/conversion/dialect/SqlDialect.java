package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.Converter;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQAndConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQDateRestrictionConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQNegationConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQOrConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CQConceptConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.BigMultiSelectFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.CountFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.DateDistanceFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConversions;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FlagFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.MultiSelectFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.NumberFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.PrefixTextFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.SingleSelectFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.SumFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.CountSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.DateDistanceSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.ExistsSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.FirstValueSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.PrefixSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.FlagSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.LastValueSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.RandomValueSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConversions;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SumSelectConverter;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.query.ConceptQueryConverter;
import com.bakdata.conquery.sql.conversion.supplier.SystemDateNowSupplier;
import com.bakdata.conquery.sql.execution.CDateSetParser;
import org.jooq.DSLContext;

public interface SqlDialect {

	SystemDateNowSupplier DEFAULT_DATE_NOW_SUPPLIER = new SystemDateNowSupplier();

	SqlFunctionProvider getFunctionProvider();

	IntervalPacker getIntervalPacker();

	SqlDateAggregator getDateAggregator();

	List<NodeConverter<? extends Visitable>> getNodeConverters();

	List<SelectConverter<? extends Select>> getSelectConverters();

	List<FilterConverter<?, ?>> getFilterConverters();

	DSLContext getDSLContext();

	CDateSetParser getCDateSetParser();

	default boolean requiresAggregationInFinalStep() {
		return true;
	}

	default List<NodeConverter<? extends Visitable>> getDefaultNodeConverters() {
		return List.of(
				new CQDateRestrictionConverter(),
				new CQAndConverter(),
				new CQOrConverter(),
				new CQNegationConverter(),
				new CQConceptConverter(new FilterConversions(getFilterConverters()), new SelectConversions(getSelectConverters()), getFunctionProvider()),
				new ConceptQueryConverter(new QueryStepTransformer(getDSLContext()))
		);
	}

	default List<SelectConverter<? extends Select>> customizeSelectConverters(List<SelectConverter<?>> substitutes) {
		return customize(getDefaultSelectConverters(), substitutes);
	}

	default List<FilterConverter<?, ?>> getDefaultFilterConverters() {
		return List.of(
				new DateDistanceFilterConverter(DEFAULT_DATE_NOW_SUPPLIER),
				new BigMultiSelectFilterConverter(),
				new MultiSelectFilterConverter(),
				new SingleSelectFilterConverter(),
				new NumberFilterConverter(),
				new SumFilterConverter(),
				new CountFilterConverter(),
				new PrefixTextFilterConverter(),
				new FlagFilterConverter()
		);
	}

	default List<SelectConverter<? extends Select>> getDefaultSelectConverters() {
		return List.of(
				new FirstValueSelectConverter(),
				new LastValueSelectConverter(),
				new RandomValueSelectConverter(),
				new DateDistanceSelectConverter(DEFAULT_DATE_NOW_SUPPLIER),
				new ExistsSelectConverter(),
				new SumSelectConverter(),
				new CountSelectConverter(),
				new PrefixSelectConverter(),
				new FlagSelectConverter()
		);
	}

	private static <R, C extends Converter<?, R, ?>> List<C> customize(List<C> defaults, List<C> substitutes) {
		Map<Class<?>, C> substituteMap = getSubstituteMap(substitutes);
		return defaults.stream()
					   .map(converter -> substituteMap.getOrDefault(converter.getConversionClass(), converter))
					   .toList();
	}

	private static <R, C extends Converter<?, R, ?>> Map<Class<?>, C> getSubstituteMap(List<C> substitutes) {
		return substitutes.stream()
						  .collect(Collectors.toMap(
								  Converter::getConversionClass,
								  Function.identity()
						  ));
	}

}
