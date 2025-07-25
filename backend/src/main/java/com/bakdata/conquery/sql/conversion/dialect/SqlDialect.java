package com.bakdata.conquery.sql.conversion.dialect;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.Converter;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQAndConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQDateRestrictionConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQExternalConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQNegationConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQOrConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQYesConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CQConceptConverter;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.query.AbsoluteFormQueryConverter;
import com.bakdata.conquery.sql.conversion.query.CQReusedQueryConverter;
import com.bakdata.conquery.sql.conversion.query.ConceptQueryConverter;
import com.bakdata.conquery.sql.conversion.query.EntityDateQueryConverter;
import com.bakdata.conquery.sql.conversion.query.FormConversionHelper;
import com.bakdata.conquery.sql.conversion.query.RelativFormQueryConverter;
import com.bakdata.conquery.sql.conversion.query.SecondaryIdQueryConverter;
import com.bakdata.conquery.sql.conversion.query.TableExportQueryConverter;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import com.bakdata.conquery.sql.conversion.supplier.SystemDateNowSupplier;
import com.bakdata.conquery.sql.execution.SqlCDateSetParser;
import org.jooq.DSLContext;
import org.jooq.Field;

public interface SqlDialect {

	SystemDateNowSupplier SYSTEM_DATE_NOW_SUPPLIER = new SystemDateNowSupplier();

	boolean isTypeCompatible(Field<?> field, MajorTypeId type);

	SqlFunctionProvider getFunctionProvider();

	IntervalPacker getIntervalPacker();

	SqlDateAggregator getDateAggregator();

	List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext context);

	SqlCDateSetParser getCDateSetParser();

	default DateNowSupplier getDateNowSupplier() {
		return SYSTEM_DATE_NOW_SUPPLIER;
	}

	default boolean supportsSingleColumnRanges() {
		return false;
	}

	default List<NodeConverter<? extends Visitable>> getDefaultNodeConverters(DSLContext dslContext) {

		QueryStepTransformer queryStepTransformer = new QueryStepTransformer(dslContext);
		FormConversionHelper formConversionUtil = new FormConversionHelper(queryStepTransformer);

		return List.of(
				new CQDateRestrictionConverter(),
				new CQAndConverter(),
				new CQOrConverter(),
				new CQNegationConverter(),
				new CQYesConverter(),
				new CQConceptConverter(),
				new CQExternalConverter(),
				new CQReusedQueryConverter(),
				new ConceptQueryConverter(queryStepTransformer),
				new SecondaryIdQueryConverter(),
				new AbsoluteFormQueryConverter(formConversionUtil),
				new EntityDateQueryConverter(formConversionUtil),
				new RelativFormQueryConverter(formConversionUtil),
				new TableExportQueryConverter(queryStepTransformer)
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
