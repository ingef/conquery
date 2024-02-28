package com.bakdata.conquery.sql.conversion.query;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import lombok.Value;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

@Value
class ConceptSqlQuery implements SqlQuery {

	String query;
	List<ResultInfo> resultInfos;

	public ConceptSqlQuery(Select<Record> query, List<ResultInfo> resultInfos) {
		this.query = query.getSQL(ParamType.INLINED);
		this.resultInfos = resultInfos;
	}

	@Override
	public String getSql() {
		return this.query;
	}

}
