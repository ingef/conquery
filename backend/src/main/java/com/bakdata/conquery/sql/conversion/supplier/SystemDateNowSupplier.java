package com.bakdata.conquery.sql.conversion.supplier;

import java.time.LocalDate;

public class SystemDateNowSupplier implements DateNowSupplier {

	@Override
	public LocalDate getLocalDateNow() {
		return LocalDate.now();
	}

}
