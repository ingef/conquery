package com.bakdata.conquery.sql;

import java.util.List;

import com.bakdata.conquery.models.types.ResultType;

public interface SqlQuery {

	String toString();

	List<ResultType> getResultTypes();

}
