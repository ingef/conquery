package com.bakdata.eva.forms.common;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.eva.models.forms.EventIndex;
import com.bakdata.eva.models.forms.Resolution;

import lombok.Data;

@Data
public class EventPeriods {
	@NotNull
	private Resolution unit;
	@Min(0)
	private int countBefore;
	@Min(0)
	private int countAfter;
	@NotNull
	private EventIndex indexDate;
}
