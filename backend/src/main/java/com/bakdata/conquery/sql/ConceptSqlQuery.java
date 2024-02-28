package com.bakdata.conquery.sql;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.Value;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

@Value
public class ConceptSqlQuery implements SqlQuery {

	String sqlString;
	List<ResultInfo> resultInfos;

	public ConceptSqlQuery(Select<Record> finalQuery, List<ResultInfo> resultInfos) {
		this.sqlString = finalQuery.getSQL(ParamType.INLINED);
		this.resultInfos = resultInfos;
	}

	@Override
	public String getSql() {
		return this.sqlString;
	}

}
