package com.bakdata.conquery.sql.conversion.query;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

public class SecondaryIdSqlQuery extends ConceptSqlQuery {

	private SecondaryIdSqlQuery(String sqlString, List<ResultInfo> resultInfos) {
		super(sqlString, resultInfos);
	}

	public static SecondaryIdSqlQuery overwriteResultInfos(SqlQuery query, List<ResultInfo> resultInfos) {
		return new SecondaryIdSqlQuery(query.getSql(), resultInfos);
	}

}
