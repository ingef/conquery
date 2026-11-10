package com.bakdata.conquery.sql.model.form;

import java.util.List;

import com.bakdata.conquery.models.forms.util.ResolutionAndAlignment;

public record RelativeFormSettings(
		FormIndexSelector indexSelector,
		FormIndexPlacement indexPlacement,
		int timeCountBefore,
		int timeCountAfter,
		FormCalendarUnit timeUnit,
		List<ResolutionAndAlignment> resolutions
) {
	public RelativeFormSettings { resolutions = List.copyOf(resolutions); }
}
