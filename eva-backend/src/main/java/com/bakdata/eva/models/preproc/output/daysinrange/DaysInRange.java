package com.bakdata.eva.models.preproc.output.daysinrange;

import java.time.LocalDate;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.daterange.CDateRange;

import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class DaysInRange {

	@NotNull @Getter
	private final CDateRange range;
	@Min(0)
	@Getter
	private final long days;

	public LocalDate getStart() {
		return range.getMin();
	}

	public LocalDate getEnd() {
		return range.getMax();
	}

	public boolean immediatelyPrecedes(DaysInRange other) {
		return range.getMaxValue() + 1 == other.range.getMinValue();
	}

	public LocalDate fromStart() {
		return range.getMin().plusDays(days - 1);
	}

	public LocalDate fromEnd() {
		return range.getMax().minusDays(days - 1);
	}

	public CDateRange rangeFromStart() {
		return CDateRange.of(getStart(), fromStart());
	}

	public CDateRange rangeFromEnd() {
		return CDateRange.of(fromEnd(), getEnd());
	}

	public boolean isEmpty() {
		return days == 0;
	}

	@ValidationMethod(message = "DaysInRange contains more days than possible")
	private boolean isDaysBelowMaximum() {
		long maxDays = range.getNumberOfDays();
		return maxDays >= days;
	}

	@Override
	public String toString() {
		return days + " days between " + getStart() + " and " + getEnd();
	}
}
