package com.bakdata.conquery.apiv1.forms;

import java.util.Map;

import com.bakdata.conquery.apiv1.query.Query;

/**
 * Interface for {@link Form}s that are executed as a {@link com.bakdata.conquery.models.forms.managed.ManagedInternalForm}.
 *
 * {@link com.bakdata.conquery.models.forms.managed.ManagedInternalForm}s can consist of multiple sub queries which are generated from the api object.
 */
public interface InternalForm {


	Map<String, Query> createSubQueries();

}
