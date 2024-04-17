package com.bakdata.conquery.sql.conversion.query;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.Getter;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

@Getter
class ConceptSqlQuery implements SqlQuery {

	String sqlString;
	List<ResultInfo> resultInfos;

	public ConceptSqlQuery(Select<Record> finalQuery, List<ResultInfo> resultInfos) {
		this.sqlString = finalQuery.getSQL(ParamType.INLINED);
		this.resultInfos = resultInfos;
	}

	protected ConceptSqlQuery(String sqlString, List<ResultInfo> resultInfos) {
		this.sqlString = sqlString;
		this.resultInfos = resultInfos;
	}

	@Override
	public String getSql() {
		return this.sqlString;
	}

}
