package com.bakdata.conquery.integration.sql.dialect;

import java.time.LocalDate;

import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;

public class MockDateNowSupplier implements DateNowSupplier {

	@Override
	public LocalDate getLocalDateNow() {
		return LocalDate.parse("2023-03-28");
	}

}
