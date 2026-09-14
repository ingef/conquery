package com.bakdata.conquery.sql.compiler.ir.concept;

import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import lombok.Value;

/** Carries select expressions and staged where clauses produced while compiling a resolved filter. */
@Value
public class SqlFilters {
	ConnectorSqlSelects selects;
	WhereClauses whereClauses;
}
