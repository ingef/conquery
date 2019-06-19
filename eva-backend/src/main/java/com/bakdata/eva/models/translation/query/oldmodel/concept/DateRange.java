package com.bakdata.eva.models.translation.query.oldmodel.concept;

import java.time.LocalDate;

import com.bakdata.conquery.models.common.Range;

import lombok.Data;

@Data
public class DateRange {
	private LocalDate min;
	private LocalDate max;
	private LocalDate exact;

	public Range<LocalDate> translate() {
		return exact != null ? Range.exactly(exact) : Range.of(min, max);
	}
}
