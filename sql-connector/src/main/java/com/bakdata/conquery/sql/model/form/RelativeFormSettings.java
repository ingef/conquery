package com.bakdata.conquery.sql.model.form;

import java.util.List;

public record RelativeFormSettings(
		FormIndexSelector indexSelector,
		FormIndexPlacement indexPlacement,
		int timeCountBefore,
		int timeCountAfter,
		FormCalendarUnit timeUnit,
		List<ResolutionAndAlignment> resolutions
) {
	public RelativeFormSettings { resolutions = List.copyOf(resolutions); }
	public FormIndexSelector getIndexSelector() { return indexSelector; }
	public FormIndexPlacement getIndexPlacement() { return indexPlacement; }
	public int getTimeCountBefore() { return timeCountBefore; }
	public int getTimeCountAfter() { return timeCountAfter; }
	public FormCalendarUnit getTimeUnit() { return timeUnit; }
	public List<ResolutionAndAlignment> getResolutionsAndAlignmentMap() { return resolutions; }
}
