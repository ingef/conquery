package com.bakdata.conquery.sql.model.schema;

import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertInvalid;
import static com.bakdata.conquery.sql.model.ValidationTestSupport.assertValid;

import org.junit.jupiter.api.Test;

class SqlTableTest {

	@Test
	void shouldAcceptQualifiedPhysicalName() {
		assertValid(SqlTable.of("events", "analytics", "events"));
	}

	@Test
	void shouldRejectMissingPhysicalName() {
		assertInvalid(SqlTable.of("events"));
		assertInvalid(SqlTable.of("events", (String[]) null));
	}

	@Test
	void shouldRejectBlankPhysicalNameParts() {
		assertInvalid(SqlTable.of("events", "analytics", " "));
		assertInvalid(SqlTable.of("events", "analytics", ""));
		assertInvalid(SqlTable.of("events", "analytics", null));
	}
}
