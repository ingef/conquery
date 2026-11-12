package com.bakdata.conquery.sql.translate.query.concept;


import com.bakdata.conquery.sql.translate.ir.condition.WhereClauses;
import lombok.Value;

/** Carries select expressions and staged where clauses produced while compiling a resolved filter. */
@Value
public class SqlFilters {
	ConnectorSqlSelects selects;
	WhereClauses whereClauses;
}
