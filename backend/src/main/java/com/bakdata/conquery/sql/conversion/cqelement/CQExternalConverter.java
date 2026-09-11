package com.bakdata.conquery.sql.conversion.cqelement;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.sql.compiler.ir.ExternalQueryStepCompiler;
import com.bakdata.conquery.sql.compiler.ir.ExternalQuerySteps;
import com.bakdata.conquery.sql.conversion.NodeConverter;
import com.bakdata.conquery.sql.model.node.ExternalEntity;
import com.bakdata.conquery.sql.model.node.ExternalNode;
import com.bakdata.conquery.sql.model.range.DateRange;

/**
 * Adapts the resolved legacy external-query payload to connector-owned compilation.
 *
 * <p>TODO Remove this adapter once backend query resolution produces {@link ExternalNode} directly.</p>
 */
public class CQExternalConverter implements NodeConverter<CQExternal> {

	@Override
	public Class<? extends CQExternal> getConversionClass() {
		return CQExternal.class;
	}

	@Override
	public ConversionContext convert(CQExternal external, ConversionContext context) {
		ExternalQuerySteps steps = ExternalQueryStepCompiler.compile(
				toExternalNode(external),
				context.isNegation(),
				Optional.ofNullable(context.getDateRestrictionRange()).map(CQExternalConverter::toDateRange),
				context.getCompilerDialect()
		);
		ConversionContext converted = context.withQueryStep(steps.entities());
		return steps.values().map(converted::withExternalExtras).orElse(converted);
	}

	private static ExternalNode toExternalNode(CQExternal external) {
		List<ExternalEntity> entities = external.getValuesResolved().entrySet().stream()
				.map(entry -> toExternalEntity(external, entry.getKey(), entry.getValue().asRanges()))
				.toList();
		return new ExternalNode(entities, external.getExtraHeaders());
	}

	private static ExternalEntity toExternalEntity(
			CQExternal external,
			String entityId,
			Collection<CDateRange> validityDates
	) {
		Map<String, List<String>> values = new LinkedHashMap<>();
		external.getExtrasForId(entityId).forEach(entry -> values.put(entry.getKey(), entry.getValue()));
		return new ExternalEntity(
				entityId,
				validityDates.stream().map(CQExternalConverter::toDateRange).toList(),
				values
		);
	}

	private static DateRange toDateRange(CDateRange range) {
		return new DateRange(Optional.ofNullable(range.getMin()), Optional.ofNullable(range.getMax()));
	}
}
