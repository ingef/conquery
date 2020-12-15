package com.bakdata.conquery.models.forms.util;

import com.bakdata.conquery.models.common.daterange.CDateRange;

import java.util.List;
import java.util.Locale;

public interface DateRangeSubDivider {

    List<CDateRange> subdivideRange(CDateRange range);

    String toString(Locale locale);
}
