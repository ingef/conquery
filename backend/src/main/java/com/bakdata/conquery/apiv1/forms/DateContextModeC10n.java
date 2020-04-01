package com.bakdata.conquery.apiv1.forms;

import c10n.annotations.De;
import c10n.annotations.En;

public interface DateContextModeC10n {
	@En("complete")
	@De("Gesamt")
	String complete();
	
	@En("year")
	@De("Jahr")
	String year();
	
	@En("quarter")
	@De("Quartal")
	String quarter();
	
	@En("day")
	@De("Tag")
	String day();

}
