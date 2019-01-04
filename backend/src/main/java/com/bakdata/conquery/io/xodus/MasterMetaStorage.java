package com.bakdata.conquery.io.xodus;

import java.util.Collection;

import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.ManagedQuery;

public interface MasterMetaStorage extends ConqueryStorage {

	void addQuery(ManagedQuery query) throws JSONException;
	ManagedQuery getQuery(ManagedQueryId id);
	Collection<ManagedQuery> getAllQueries();
	void updateQuery(ManagedQuery query) throws JSONException;
	void removeQuery(ManagedQueryId id);
}