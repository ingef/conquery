package com.bakdata.eva.models.translation.query.oldmodel.concept;

import java.math.BigDecimal;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.eva.models.translation.IdentifiableMocker;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class QFilter<VALUE> {
	@Valid @NotNull
	private String id;
	@Valid @NotNull
	private VALUE value;

	public abstract FilterValue<?> translate(DatasetId dataset, ConceptId conceptId, ConnectorId connectorId);

	@Data
	private static class RangeContainer<T> {
		@Nullable
		private final T min;
		@Nullable
		private final T max;
		@Nullable
		private final T exact;

	}


	//TODO Proper parsing of range values, respecting the field exact

	private static Filter<?> createFilter(ConnectorId connectorId, String filterId) {
		return IdentifiableMocker.mockAnswer(new FilterId(connectorId, filterId));
	}

	@NoArgsConstructor
	@CPSType(id="MULTI_SELECT", base=QFilter.class)
	@CPSType(id="BIG_MULTI_SELECT", base=QFilter.class)
	public static class QMultiSelectFilter extends QFilter<String[]> {

		public QMultiSelectFilter(String id, String[] value) {
			super(id, value);
		}

		public FilterValue.CQMultiSelectFilter translate(DatasetId dataset, ConceptId conceptId, ConnectorId connectorId) {

			final Filter<?> filter = QFilter.createFilter(connectorId, getId());

			return new FilterValue.CQMultiSelectFilter(filter, getValue());
		}
	}

	@NoArgsConstructor @CPSType(id="SELECT", base=QFilter.class)
	public static class QSelectFilter extends QFilter<String> {
		public QSelectFilter(String id, String value) {super(id, value);}

		public FilterValue.CQSelectFilter translate(DatasetId dataset, ConceptId conceptId, ConnectorId connectorId) {

			final Filter<?> filter = QFilter.createFilter(connectorId, getId());

			return new FilterValue.CQSelectFilter(filter, getValue());
		}

	}
	@NoArgsConstructor @CPSType(id="STRING", base=QFilter.class)
	public static class QStringFilter extends QFilter<String> {

		public QStringFilter(String id, String value) {super(id, value);}

		public FilterValue.CQStringFilter translate(DatasetId dataset, ConceptId conceptId, ConnectorId connectorId) {

			final Filter<?> filter = QFilter.createFilter(connectorId, getId());

			return new FilterValue.CQStringFilter(filter, getValue());
		}
	}

	@NoArgsConstructor @CPSType(id="INTEGER_RANGE", base=QFilter.class)
	public static class QIntegerRangeFilter		extends QFilter<RangeContainer<Long>> {
		public QIntegerRangeFilter(String id, RangeContainer<Long> value) {super(id, value);}

		public FilterValue.CQIntegerRangeFilter translate(DatasetId dataset, ConceptId conceptId, ConnectorId connectorId) {

			final Filter<?> filter = QFilter.createFilter(connectorId, getId());

			Range.LongRange range = new Range.LongRange(getValue().getMin(), getValue().getMax());

			if(getValue().exact != null)
				range = new Range.LongRange(getValue().getExact(), getValue().getExact());

			return new FilterValue.CQIntegerRangeFilter(filter, range);
		}
	}


	@NoArgsConstructor @CPSType(id="MONEY_RANGE", base=QFilter.class)
	public static class QMoneyRangeFilter		extends QFilter<RangeContainer<Long>> {
		public QMoneyRangeFilter(String id, RangeContainer<Long> value) {super(id, value);}

		public FilterValue.CQIntegerRangeFilter translate(DatasetId dataset, ConceptId conceptId, ConnectorId connectorId) {

			final Filter<?> filter = QFilter.createFilter(connectorId, getId());

			Range.LongRange range = new Range.LongRange(getValue().getMin(), getValue().getMax());

			if(getValue().exact != null)
				range = new Range.LongRange(getValue().getExact(), getValue().getExact());

			//TODO Money Range?
			return new FilterValue.CQIntegerRangeFilter(filter, range);
		}
	}
	@NoArgsConstructor @CPSType(id="REAL_RANGE", base=QFilter.class)
	public static class QRealRangeFilter		extends QFilter<RangeContainer<BigDecimal>> {
		public QRealRangeFilter(String id, RangeContainer<BigDecimal> value) {super(id, value);}

		public FilterValue.CQRealRangeFilter translate(DatasetId dataset, ConceptId conceptId, ConnectorId connectorId) {

			final Filter<?> filter = QFilter.createFilter(connectorId, getId());

			Range<BigDecimal> range = Range.of(getValue().getMin(), getValue().getMax());

			if(getValue().exact != null)
				range = Range.exactly(getValue().getExact());

			return new FilterValue.CQRealRangeFilter(filter, range);
		}
	}
}
