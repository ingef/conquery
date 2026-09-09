package com.bakdata.conquery.sql.compiler.ir;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import org.junit.jupiter.api.Test;

class QualifyingUtilTest {

	@Test
	void shouldQualifyFieldsWithoutChangingTheirType() {
		var qualified = QualifyingUtil.qualify(field(name("result"), Integer.class), "query_step");

		assertEquals(name("query_step", "result"), qualified.getQualifiedName());
		assertEquals(Integer.class, qualified.getType());
	}

	@Test
	void shouldQualifySelectLists() {
		SqlSelect select = new ExtractingSqlSelect<>("source", "result", Integer.class);

		List<SqlSelect> qualified = QualifyingUtil.qualify(List.of(select), "query_step");

		assertEquals(name("query_step", "result"), qualified.getFirst().toFields().getFirst().getQualifiedName());
	}
}
