package com.bakdata.conquery.sql.conversion.model.filter;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.Range.LongRange;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.QuartersInYearFilter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import org.jooq.Condition;

public class QuartersInYearFilterConverter implements FilterConverter<QuartersInYearFilter, Range.LongRange> {
    @Override
    public SqlFilters convertToSqlFilter(QuartersInYearFilter filter, FilterContext<LongRange> filterContext) {
        return null;
    }

    @Override
    public Condition convertForTableExport(QuartersInYearFilter filter, FilterContext<LongRange> filterContext) {
        return null;
    }
}
