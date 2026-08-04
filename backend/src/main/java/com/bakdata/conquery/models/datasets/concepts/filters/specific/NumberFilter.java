package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.filters.EventFilter;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.event.number.DecimalFilterNode;
import com.bakdata.conquery.models.query.filter.event.number.IntegerFilterNode;
import com.bakdata.conquery.models.query.filter.event.number.MoneyFilterNode;
import com.bakdata.conquery.models.query.filter.event.number.RealFilterNode;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.model.filter.NumberFilterConverter;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.dropwizard.validation.ValidationMethod;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This filter represents a filter on an integer columnof each event.
 */
@Getter
@Setter
@Slf4j
@CPSType(id = "NUMBER", base = Filter.class)
public class NumberFilter<RANGE extends IRange<? extends Number, ?>> extends EventFilter<RANGE> {

	@Valid
	@NotNull
	private ColumnId column;

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@EqualsAndHashCode.Exclude
	private ConqueryConfig config;

	@Override
	public List<ColumnId> getRequiredColumns() {
		return List.of(getColumn());
	}

	@JsonIgnore
	@ValidationMethod(message = "Columns do not match required Type.")
	public boolean isValidColumnType() {
		final Column resolved = getColumn().resolve();
		final boolean acceptable = getAcceptedColumnTypes().contains(resolved.getType());

		if (!acceptable) {
			log.error("Column[{}] is of Type[{}]. Not one of [{}]", resolved.getId(), resolved.getType(), getAcceptedColumnTypes());
		}

		return acceptable;
	}

	@JsonIgnore
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.MONEY, MajorTypeId.INTEGER, MajorTypeId.DECIMAL, MajorTypeId.REAL);
	}

	@Override
	public void configureFrontend(FrontendFilterConfiguration.Top f, ConqueryConfig conqueryConfig) throws ConceptConfigurationException {
		final MajorTypeId typeId = getColumn().resolve().getType();
		final String type = switch (typeId) {
			case MONEY -> FrontendFilterType.Fields.MONEY_RANGE;
			case INTEGER -> FrontendFilterType.Fields.INTEGER_RANGE;
			case DECIMAL, REAL -> FrontendFilterType.Fields.REAL_RANGE;
			default -> throw new ConceptConfigurationException(getConnector(), "NUMBER filter is incompatible with columns of type " + typeId);
		};

		f.setType(type);
	}

	@Override
	public EventFilterNode<?> createFilterNode(RANGE value) {
		final Column column = getColumn().resolve();
		final MajorTypeId typeId = column.getType();

		return switch (typeId) {
			case MONEY -> new MoneyFilterNode(column, (Range.MoneyRange) value);
			case INTEGER -> new IntegerFilterNode(column, (Range.LongRange) value);
			case DECIMAL -> new DecimalFilterNode(column, (Range<BigDecimal>) value);
			case REAL -> new RealFilterNode(column, Range.DoubleRange.fromNumberRange(value));
			default -> throw new IllegalStateException(String.format("Column type %s may not be used (Assignment should not have been possible)", column));
		};
	}

	@Override
	public FilterConverter<? extends NumberFilter<RANGE>, RANGE> createConverter() {
		return new NumberFilterConverter<>();
	}
}
