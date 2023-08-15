package com.bakdata.conquery.integration.sql;

import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.conversion.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.select.DateDistanceConverter;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlDialect;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import org.jooq.DSLContext;

import java.time.LocalDate;
import java.util.List;

public class TestPostgreSqlDialect extends PostgreSqlDialect {

	public TestPostgreSqlDialect(DSLContext dslContext) {
		super(dslContext);
	}

	@Override
	public List<SelectConverter<? extends Select>> getSelectConverters() {
		return this.customizeSelectConverters(List.of(
				new DateDistanceConverter(new MockDateNowSupplier())
		));
	}

	private class MockDateNowSupplier implements DateNowSupplier {

		@Override
		public LocalDate getLocalDateNow() {
			return LocalDate.parse("2023-03-28");
		}

	}

}
