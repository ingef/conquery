package com.bakdata.conquery.sql.conversion.dialect;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQAndConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQDateRestrictionConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQExternalConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQNegationConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQOrConverter;
import com.bakdata.conquery.sql.conversion.cqelement.CQYesConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.CQConceptConverter;
import com.bakdata.conquery.sql.conversion.forms.StratificationFunctions;
import com.bakdata.conquery.sql.conversion.model.QueryStepTransformer;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.query.AbsoluteFormQueryConverter;
import com.bakdata.conquery.sql.conversion.query.CQReusedQueryConverter;
import com.bakdata.conquery.sql.conversion.query.ConceptQueryConverter;
import com.bakdata.conquery.sql.conversion.query.EntityDateQueryConverter;
import com.bakdata.conquery.sql.conversion.query.FormConversionHelper;
import com.bakdata.conquery.sql.conversion.query.RelativFormQueryConverter;
import com.bakdata.conquery.sql.conversion.query.SecondaryIdQueryConverter;
import com.bakdata.conquery.sql.conversion.query.TableExportQueryConverter;
import org.jooq.DSLContext;
import org.jooq.Field;

/**
 * Temporary backend adapter exposing services required by the legacy SQL compiler.
 *
 * <p>The framework-neutral dialect contract lives in {@link CompilerDialect}. This adapter retains dependencies on
 * backend query DTOs, converter registries, and legacy compiler services until those implementations move into the SQL
 * connector. New connector code must not depend on this interface.</p>
 */
public interface LegacyCompilerDialect extends CompilerDialect {

	@Override
	default <T> Field<T> anyValue(Field<T> field) {
		return getFunctionProvider().anyValue(field);
	}

	StratificationFunctions getStratificationFunctions();

	SqlFunctionProvider getFunctionProvider();

	IntervalPacker getIntervalPacker();

	SqlDateAggregator getDateAggregator();

	List<NodeConverter<? extends Visitable>> getNodeConverters(DSLContext context);

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

	default Map<Class<? extends Select>, ? extends SelectConverter<? extends Select>> getSelectConverterOverrides() {
		return Collections.emptyMap();
	}

	default SelectConverter<Select> getSelectConverter(Select select) {
		SelectConverter<Select> maybeOverride = (SelectConverter<Select>) getSelectConverterOverrides().get(select.getClass());

		if (maybeOverride != null) {
			return maybeOverride;
		}

		return select.createConverter();
	}
}
