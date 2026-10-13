package com.bakdata.conquery.sql.conversion.model;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.translate.TranslatedQuery;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SqlQuery {

	private final String sql;
	private final List<ResultInfo> resultInfos;

	public static SqlQuery fromCompiled(TranslatedQuery translatedQuery, List<ResultInfo> resultInfos) {
		return new SqlQuery(translatedQuery.sql(), List.copyOf(resultInfos));
	}

}
