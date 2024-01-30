package com.bakdata.conquery.sql.conversion.model;

import java.util.List;

import com.bakdata.conquery.models.query.resultinfo.ResultInfo;

public interface SqlQuery {

	String getSql();

	List<ResultInfo> getResultInfos();

}
