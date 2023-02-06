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
	
	@En("Query")
	@De("Anfrage")
	String reused();

	@En("and further")
	@De("und weitere")
	String furtherConcepts();

	@En("and")
	@De("und")
	String and();

	@En("or")
	@De("oder")
	String or();

}
