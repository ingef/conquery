package com.bakdata.conquery.models.forms.util;

import java.util.List;
import java.util.Locale;

import com.bakdata.conquery.models.common.daterange.CDateRange;

//TODO this still used?
public interface DateRangeSubDivider {

    List<CDateRange> subdivideRange(CDateRange range);

    String toString(Locale locale);
}
