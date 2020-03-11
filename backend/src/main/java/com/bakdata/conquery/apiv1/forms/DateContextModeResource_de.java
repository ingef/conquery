package com.bakdata.conquery.apiv1.forms;

import java.util.ListResourceBundle;

import lombok.Getter;


public class DateContextModeResource_de extends ListResourceBundle {
	
	@Getter(onMethod = @__(@Override))
	private final Object[][] contents = {
		{DateContextMode.COMPLETE.toString(), "Gesamt"},
		{DateContextMode.YEARS.toString(), "Jahr"},
		{DateContextMode.QUARTERS.toString(), "Quartal"},
		{DateContextMode.DAYS.toString(), "Tag"},
	};

}
