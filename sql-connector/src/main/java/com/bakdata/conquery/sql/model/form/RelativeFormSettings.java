package com.bakdata.conquery.sql.model.form;

import java.util.List;

import com.bakdata.conquery.models.forms.util.CalendarUnit;
import com.bakdata.conquery.models.forms.util.IndexSelector;
import com.bakdata.conquery.models.forms.util.IndexPlacement;
import com.bakdata.conquery.models.forms.util.ResolutionAndAlignment;

public record RelativeFormSettings(
		IndexSelector indexSelector,
		IndexPlacement indexPlacement,
		int timeCountBefore,
		int timeCountAfter,
		CalendarUnit timeUnit,
		List<ResolutionAndAlignment> resolutions
) {
	public RelativeFormSettings { resolutions = List.copyOf(resolutions); }
}
