package com.bakdata.conquery.sql.compiler.dialect;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bakdata.conquery.sql.compiler.dialect.clickhouse.ClickhouseCompilerDialect;
import com.bakdata.conquery.sql.compiler.dialect.hana.HanaCompilerDialect;
import com.bakdata.conquery.sql.model.range.DateRange;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

class VendorCompilerDialectTest {

  private static final Field<Date> START = field(name("events", "start"), Date.class);
  private static final Field<Date> END = field(name("events", "end"), Date.class);

  @Test
  void shouldRenderHanaCapabilitiesWithoutBackendTypes() {
    var dialect = new HanaCompilerDialect();

    assertEquals("127", Integer.toString(dialect.getNameMaxLength()));
    assertTrue(render(dialect.minimumDate(), SQLDialect.DEFAULT).contains("TO_DATE('0001-01-01'"));
    assertTrue(render(dialect.maximumDate(), SQLDialect.DEFAULT).contains("TO_DATE('9999-12-31'"));
    assertTrue(
        render(dialect.dateDistance(ChronoUnit.DAYS, START, END), SQLDialect.DEFAULT)
            .contains("DAYS_BETWEEN"));
    assertTrue(render(dialect.quarterStart(START), SQLDialect.DEFAULT).contains("SERIES_ROUND"));
    assertTrue(render(dialect.nextQuarterStart(START), SQLDialect.DEFAULT).contains("ADD_MONTHS"));
    assertTrue(
        render(dialect.externalStringValues(List.of("a", "b")), SQLDialect.DEFAULT)
            .contains(Character.toString((char) 31)));
    assertEquals("\"DUMMY\"", render(dialect.literalSelectTable(), SQLDialect.DEFAULT));
    assertFalse(dialect.supportsSingleColumnRanges());
  }

  @Test
  void shouldRenderClickhouseCapabilitiesWithoutBackendTypes() {
    var dialect = new ClickhouseCompilerDialect();

    assertEquals(64, dialect.getNameMaxLength());
    assertTrue(render(dialect.minimumDate(), SQLDialect.CLICKHOUSE).contains("toDate32(-25567)"));
    assertTrue(render(dialect.maximumDate(), SQLDialect.CLICKHOUSE).contains("toDate32(24855)"));
    assertTrue(
        render(dialect.dateRange(START, END).getStart(), SQLDialect.CLICKHOUSE)
            .contains("::Nullable(Date32)"));
    assertTrue(
        render(dialect.dateDistance(ChronoUnit.MONTHS, START, END), SQLDialect.CLICKHOUSE)
            .contains("age('months'"));
    assertTrue(
        render(dialect.quarterStart(START), SQLDialect.CLICKHOUSE).contains("toStartOfYear"));
    assertTrue(
        render(dialect.nextQuarterStart(START), SQLDialect.CLICKHOUSE).contains("addMonths"));
    assertTrue(
        render(dialect.externalStringValues(List.of("a", "b")), SQLDialect.CLICKHOUSE)
            .contains("array"));
    assertTrue(
        render(dialect.externalId("id"), SQLDialect.CLICKHOUSE).contains("::Nullable(String)"));
    assertTrue(
        render(dialect.cast(field(name("value"), Integer.class), org.jooq.impl.SQLDataType.INTEGER), SQLDialect.CLICKHOUSE)
            .contains("Nullable(integer)"));
  }

  @Test
  void shouldKeepInclusiveLiteralRangeSemantics() {
    var range =
        new HanaCompilerDialect()
            .dateRangeLiteral(
                new DateRange(
                    java.util.Optional.of(LocalDate.of(2020, 1, 1)),
                    java.util.Optional.of(LocalDate.of(2020, 1, 31))));

    assertTrue(render(range.getStart(), SQLDialect.DEFAULT).contains("2020-01-01"));
    assertTrue(render(range.getEnd(), SQLDialect.DEFAULT).contains("2020-02-01"));
  }

  private static String render(org.jooq.QueryPart part, SQLDialect dialect) {
    return DSL.using(dialect).renderInlined(part).replace('`', '"');
  }
}
