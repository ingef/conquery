package com.bakdata.conquery.sql.conversion.model;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.compiler.CompiledQuery;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SqlQuery {

	private final String sql;
	private final List<ResultInfo> resultInfos;

	public static SqlQuery fromCompiled(CompiledQuery compiledQuery, List<ResultInfo> resultInfos) {
		return new SqlQuery(compiledQuery.sql(), List.copyOf(resultInfos));
	}

}
