package com.bakdata.conquery.mode.local;

import java.time.LocalDate;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlMatchingStats implements MatchingStats {

	private long numberOfEvents;
	private long numberOfEntities;
	private CDateRange span;

	@Override
	public long countEvents() {
		return numberOfEvents;
	}

	@Override
	public long countEntities() {
		return numberOfEntities;
	}

	@Override
	public CDateRange spanEvents() {
		return span;
	}

	public SqlMatchingStats add(SqlMatchingStats other) {

		this.numberOfEvents += other.numberOfEvents;
		this.numberOfEntities += other.numberOfEntities;

		if ((this.span == null && other.span == null) || this.span != null && other.span == null) {
			return this;
		}
		else if (this.span == null) {
			this.span = other.span;
			return this;
		}
		else {

			final LocalDate thisMin = this.span.getMin();
			final LocalDate otherMin = other.getSpan().getMin();
			final LocalDate min;

			if (thisMin == null) {
				min = otherMin;
			}
			else if (otherMin == null) {
				min = thisMin;
			}
			else {
				min = thisMin.isBefore(otherMin) ? thisMin : otherMin;
			}

			final LocalDate thisMax = this.span.getMax();
			final LocalDate otherMax = other.getSpan().getMax();
			final LocalDate max;

			if (thisMax == null) {
				max = otherMax;
			}
			else if (otherMax == null) {
				max = thisMax;
			}
			else {
				max = thisMax.isAfter(otherMax) ? thisMax : otherMax;
			}

			this.span = CDateRange.of(min, max);
			return this;
		}
	}

}
