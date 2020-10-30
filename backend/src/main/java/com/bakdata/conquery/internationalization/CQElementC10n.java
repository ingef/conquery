package com.bakdata.conquery.internationalization;

import c10n.annotations.De;
import c10n.annotations.En;

public interface CQElementC10n {
	
	@En("Uploaded-List")
	@De("Hochgeladene-Liste")
	String external();
	
	@En("Previous-Query")
	@De("Frühere-Anfrage")
	String reused();

}
