package com.bakdata.conquery.sql.conversion.query;

import java.util.List;

import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.sql.SqlQuery;
import com.bakdata.conquery.sql.conversion.model.Selects;
import lombok.Value;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.conf.ParamType;

@Value
class ConceptSqlQuery implements SqlQuery {

	String query;
	List<ResultType> resultTypes;

	public ConceptSqlQuery(Select<Record> query, Selects selects) {
		this.query = query.getSQL(ParamType.INLINED);
		this.resultTypes = selects.getExplicitSelects().stream()
								  .map(explicitSelect -> explicitSelect.getSqlSelectId().getResultType())
								  .toList();
	}

	@Override
	public String toString() {
		return this.query;
	}

}
