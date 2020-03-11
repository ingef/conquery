package com.bakdata.conquery.apiv1.forms;

import java.util.ListResourceBundle;

import lombok.Getter;


public class DateContextModeResource extends ListResourceBundle {
	
	@Getter(onMethod = @__(@Override))
	private final Object[][] contents = {
		{DateContextMode.COMPLETE.toString(), "complete"},
		{DateContextMode.YEARS.toString(), "year"},
		{DateContextMode.QUARTERS.toString(), "quarter"},
		{DateContextMode.DAYS.toString(), "day"},
	};

}
