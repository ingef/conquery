package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.Converter;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.context.step.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.cqelement.CQAndConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQConceptConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQDateRestrictionConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQOrConverter;
import com.bakdata.conquery.sql.conversion.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.filter.FilterConverterService;
import com.bakdata.conquery.sql.conversion.filter.MultiSelectConverter;
import com.bakdata.conquery.sql.conversion.filter.RealRangeConverter;
import com.bakdata.conquery.sql.conversion.query.ConceptQueryConverter;
import com.bakdata.conquery.sql.conversion.select.DateDistanceConverter;
import com.bakdata.conquery.sql.conversion.select.FirstValueConverter;
import com.bakdata.conquery.sql.conversion.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.select.SelectConverterService;
import com.bakdata.conquery.sql.conversion.supplier.SystemDateNowSupplier;
import org.jooq.DSLContext;

public interface SqlDialect {

	SqlFunctionProvider getFunction();

	List<NodeConverter<? extends Visitable>> getNodeConverters();

	List<FilterConverter<? extends FilterValue<?>>> getFilterConverters();

	List<SelectConverter<? extends Select>> getSelectConverters();

	DSLContext getDSLContext();

	default List<NodeConverter<? extends Visitable>> customizeNodeConverters(List<NodeConverter<?>> substitutes) {

		Map<Class<?>, NodeConverter<?>> nodeClassSubstituteMap = getNodeClassSubstituteMap(substitutes);

		return getDefaultNodeConverters(getDSLContext()).stream()
														.map(nodeConverter -> nodeClassSubstituteMap.getOrDefault(
																nodeConverter.getConversionClass(),
																nodeConverter
														))
														.collect(Collectors.toList());
	}

	private static Map<Class<?>, NodeConverter<?>> getNodeClassSubstituteMap(List<NodeConverter<?>> substitutes) {
		return substitutes.stream()
						  .collect(Collectors.toMap(
								  NodeConverter::getConversionClass,
								  Function.identity()
						  ));
	}

	default List<NodeConverter<? extends Visitable>> getDefaultNodeConverters(DSLContext dslContext) {
		return List.of(
				new CQDateRestrictionConverter(),
				new CQAndConverter(),
				new CQOrConverter(),
				new CQConceptConverter(new FilterConverterService(getFilterConverters()), new SelectConverterService(getSelectConverters())),
				new ConceptQueryConverter(new QueryStepTransformer(dslContext))
		);
	}


	default List<FilterConverter<? extends FilterValue<?>>> customizeFilterConverters(List<FilterConverter<?>> substitutes) {
		Map<Class<?>, FilterConverter<?>> filterClassSubstituteMap = getFilterClassSubstituteMap(substitutes);

		return getDefaultFilterConverters().stream()
										   .map(filterConverter -> filterClassSubstituteMap.getOrDefault(
												   filterConverter.getConversionClass(),
												   filterConverter
										   ))
										   .collect(Collectors.toList());
	}

	private static Map<Class<?>, FilterConverter<?>> getFilterClassSubstituteMap(List<FilterConverter<?>> substitutes) {
		return substitutes.stream()
						  .collect(Collectors.toMap(
								  FilterConverter::getConversionClass,
								  Function.identity()
						  ));
	}

	static List<FilterConverter<? extends FilterValue<?>>> getDefaultFilterConverters() {
		return List.of(
				new MultiSelectConverter(),
				new RealRangeConverter()
		);
	}

	default List<SelectConverter<? extends Select>> customizeSelectConverters(List<SelectConverter<?>> substitutes) {
		Map<Class<?>, SelectConverter<?>> selectClassSubstituteMap = getSelectClassSubstituteMap(substitutes);

		return getDefaultSelectConverters().stream()
										   .map(selectConverter -> selectClassSubstituteMap.getOrDefault(
												   selectConverter.getConversionClass(),
												   selectConverter
										   ))
										   .collect(Collectors.toList());
	}

	private static Map<Class<?>, SelectConverter<?>> getSelectClassSubstituteMap(List<SelectConverter<?>> substitutes) {
		return substitutes.stream()
						  .collect(Collectors.toMap(
								  SelectConverter::getConversionClass,
								  Function.identity()
						  ));
	}

	static List<SelectConverter<? extends Select>> getDefaultSelectConverters() {
		return List.of(
				new FirstValueConverter(),
				new DateDistanceConverter(new SystemDateNowSupplier())
		);
	}


	static <T, R, C extends Converter<T, R>> List<C> customize(List<C> defaults, List<C> substitutes) {
		Map<Class<?>, C> substituteMap = getSubstituteMap(substitutes);
		return defaults.stream()
					   .map(converter -> substituteMap.getOrDefault(converter.getConversionClass(), converter))
					   .collect(Collectors.toList());
	}

	static <T, R, C extends Converter<T, R>> Map<Class<?>, C> getSubstituteMap(List<? extends C> substitutes) {
		return substitutes.stream()
						  .collect(Collectors.toMap(
								  Converter::getConversionClass,
								  Function.identity()
						  ));
	}
}
