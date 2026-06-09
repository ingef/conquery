package com.bakdata.conquery.models.datasets.concepts.filters.specific;

import com.bakdata.conquery.apiv1.frontend.FrontendFilterConfiguration;
import com.bakdata.conquery.apiv1.frontend.FrontendFilterType;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.exceptions.ConceptConfigurationException;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.filter.RangeFilterNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import com.bakdata.conquery.sql.conversion.model.filter.FilterConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@Data
@CPSType(id = "AVG", base = Filter.class)
public class AvgFilter<RANGE extends IRange<? extends Number, ?>> extends Filter<RANGE> {

    private ColumnId column;

    @Override
    public void configureFrontend(final FrontendFilterConfiguration.Top f, final ConqueryConfig conqueryConfig) throws
            ConceptConfigurationException {
        final MajorTypeId typeId = this.getColumn().resolve().getType();
        final String type = switch (typeId) {
            case MONEY -> FrontendFilterType.Fields.MONEY_RANGE;
            case INTEGER -> FrontendFilterType.Fields.INTEGER_RANGE;
            case DECIMAL, REAL -> FrontendFilterType.Fields.REAL_RANGE;
            default -> throw new ConceptConfigurationException(this.getConnector(),
                    "NUMBER filter is incompatible with columns of type " + typeId);
        };

        f.setType(type);
    }

    @Override
    public List<ColumnId> getRequiredColumns() {
        final List<ColumnId> out = new ArrayList<>();
        out.add(this.getColumn());
        return out;
    }

    @Override
    public FilterNode createFilterNode(final RANGE value) {
        IRange<? extends Number, ?> range = value;
        // Real and Decimal share FilterValue
        if (this.getColumn().resolve().getType() == MajorTypeId.REAL) {
            range = Range.DoubleRange.fromNumberRange(value);
        }
        return new RangeFilterNode(range, this.getAggregator());
    }

    @Override
    public FilterConverter<? extends AvgFilter<RANGE>, RANGE> createConverter() {
        // TODO use new AvgSqlAggregator here
        return null;
    }

    @JsonIgnore
    private ColumnAggregator<?> getAggregator() {
        throw new UnsupportedOperationException("Not implemented...");
    }

}
