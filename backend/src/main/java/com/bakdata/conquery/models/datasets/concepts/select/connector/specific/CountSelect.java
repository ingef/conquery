package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@CPSType(id = "COUNT", base = Select.class)
@NoArgsConstructor
@Data
public class CountSelect extends Select {

	private boolean distinct = false;

	@NsIdRefCollection
	@NotNull
	private List<Column> column;


	@Override
	public Aggregator<?> createAggregator() {
		if (distinct) {
			return new DistinctValuesWrapperAggregator<>(new CountAggregator(getColumn().get(0)), getColumn());
		}
		return new CountAggregator(getColumn().get(0));
	}

	@Nullable
	@Override
	public Column[] getRequiredColumns() {
		return getColumn().toArray(Column[]::new);
	}


	@JsonIgnore
	@ValidationMethod(message = "Cannot use multiple columns, when distinct is not set.")
	public boolean isMultiOnlyWhenDistinct() {
		if(!isDistinct()){
			return getColumn().size() == 1;
		}

		return true;
	}
}
