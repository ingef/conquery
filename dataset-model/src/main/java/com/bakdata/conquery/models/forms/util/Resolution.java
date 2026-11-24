package com.bakdata.conquery.models.forms.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Defines the granularity into which a date range is divided. */
public enum Resolution {
	COMPLETE(null, List.of(Alignment.NO_ALIGN)),
	YEARS(COMPLETE, List.of(Alignment.YEAR, Alignment.QUARTER, Alignment.DAY)),
	QUARTERS(YEARS, List.of(Alignment.QUARTER, Alignment.DAY)),
	DAYS(QUARTERS, List.of(Alignment.DAY));

	@JsonIgnore
	private final Resolution coarser;
	@JsonIgnore
	private final List<Alignment> compatibleAlignments;
	private List<Resolution> thisAndCoarserSubdivisions;

	Resolution(Resolution coarser, List<Alignment> compatibleAlignments) {
		this.coarser = coarser;
		this.compatibleAlignments = compatibleAlignments;
	}

	@JsonIgnore
	public Alignment getDefaultAlignment() {
		return compatibleAlignments.getFirst();
	}

	@JsonIgnore
	public Alignment getFittingAlignment(Alignment alignmentHint) {
		return isAlignmentSupported(alignmentHint) ? alignmentHint : getDefaultAlignment();
	}

	/** Returns the amount of calendar-aligned ranges that fit into this resolution. */
	@JsonIgnore
	public OptionalInt getAmountForAlignment(Alignment alignment) {
		if (!isAlignmentSupported(alignment)) {
			return OptionalInt.empty();
		}
		return alignment.getAmountForResolution(this);
	}

	@JsonIgnore
	public boolean isAlignmentSupported(Alignment alignment) {
		return compatibleAlignments.contains(alignment);
	}

	@JsonIgnore
	public List<Resolution> getThisAndCoarserSubdivisions() {
		if (thisAndCoarserSubdivisions != null) {
			return thisAndCoarserSubdivisions;
		}
		final List<Resolution> thisAndCoarser = new ArrayList<>();
		if (coarser != null) {
			thisAndCoarser.addAll(coarser.getThisAndCoarserSubdivisions());
		}
		thisAndCoarser.add(this);
		return thisAndCoarserSubdivisions = Collections.unmodifiableList(thisAndCoarser);
	}
}
