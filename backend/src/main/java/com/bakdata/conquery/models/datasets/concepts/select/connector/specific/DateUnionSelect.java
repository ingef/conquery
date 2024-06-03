package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelect;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.DateUnionAggregator;
import com.bakdata.conquery.sql.conversion.model.select.DateUnionSelectConverter;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverterHolder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "DATE_UNION", base = Select.class)
@JsonIgnoreProperties("categorical")
public class DateUnionSelect extends Select implements DaterangeSelect {

	@NsIdRef
	@Nullable
	private Column column;
	@NsIdRef
	@Nullable
	private Column startColumn;
	@NsIdRef
	@Nullable
	private Column endColumn;

	@Override
	public List<Column> getRequiredColumns() {
		if (column != null) {
			return List.of(column);
		}
		return List.of(startColumn, endColumn);
	}

	@Override
	public Aggregator<?> createAggregator() {
		if (getColumn() != null) {
			return new DateUnionAggregator(getColumn());
		}
		// only relevant for ResultType in SQL mode
		return new DateUnionAggregator(getStartColumn());
	}

	@Override
	public SelectConverterHolder<?> createConverterHolder() {
		return new SelectConverterHolder<>(this, new DateUnionSelectConverter());
	}
}
