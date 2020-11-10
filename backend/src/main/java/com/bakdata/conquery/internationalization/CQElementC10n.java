package com.bakdata.conquery.internationalization;

import c10n.annotations.De;
import c10n.annotations.En;

/**
 * Cosmopolitan (C10n) internationalization provider for elements of a query description.
 * These are used to generate expressive default labels for queries. 
 */
public interface CQElementC10n {
	
	@En("Uploaded-List")
	@De("Hochgeladene-Liste")
	String external();
	
	@En("Previous-Query")
	@De("Frühere-Anfrage")
	String reused();

}
