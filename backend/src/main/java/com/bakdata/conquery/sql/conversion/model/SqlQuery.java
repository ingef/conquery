package com.bakdata.conquery.sql.conversion.model;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlQuery {

	private final String sql;
	private final List<ResultInfo> resultInfos;

	public SqlQuery(Select<Record> finalQuery, List<ResultInfo> resultInfos) {
		this.sql = finalQuery.getSQL(ParamType.INLINED);
		this.resultInfos = resultInfos;
	}

	/**
	 * For testing purposes
	 */
	protected SqlQuery(String sql) {
		this.sql = sql;
		this.resultInfos = Collections.emptyList();
	}

	public SqlQuery overwriteResultInfos(List<ResultInfo> resultInfos) {
		return new SqlQuery(sql, resultInfos);
	}

}
