package com.bakdata.conquery.internationalization;

import c10n.annotations.De;
import c10n.annotations.En;

public interface Results {
	@De("Ja")
	@En("Yes")
	String True();

	@De("Nein")
	@En("No")
	String False();

}
