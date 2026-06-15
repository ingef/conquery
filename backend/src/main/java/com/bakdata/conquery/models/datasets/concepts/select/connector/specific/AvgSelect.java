package com.bakdata.conquery.models.datasets.concepts.select.connector.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.DistinctValuesWrapperAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.conversion.model.aggregator.AvgSqlAggregator;
import com.bakdata.conquery.sql.conversion.model.select.SelectConverter;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@CPSType(id = "AVG", base = Select.class)
@NoArgsConstructor
@Data
public class AvgSelect extends Select {

    private boolean distinct = false;

    @NotNull
    private ColumnId column;

    @Override
    public Aggregator<?> createAggregator() {
        final Column resolved = getColumn().resolve();
        if (!isDistinct()) {
            return new CountAggregator(resolved);
        }

        return new DistinctValuesWrapperAggregator(new CountAggregator(resolved), List.of(getColumn().resolve()));
    }

    @Nullable
    @Override
    public List<ColumnId> getRequiredColumns() {
        final List<ColumnId> out = new ArrayList<>();
        out.add(getColumn());

        return out;
    }

    @Override
    public SelectConverter<AvgSelect> createConverter() {
        return new AvgSqlAggregator();
    }

    @Override
    public ResultSetProcessor.Reader<Integer> createResultSetReader(ResultSetProcessor processor) {
        return processor::getInteger;
    }

    @Override
    public ResultType getResultType() {
        return ResultType.resolveResultType(getColumn().resolve().getType());
    }

}
