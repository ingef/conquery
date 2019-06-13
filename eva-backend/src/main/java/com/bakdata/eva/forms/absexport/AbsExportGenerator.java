package com.bakdata.eva.forms.absexport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryManager;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.queries.FormQuery;
import com.bakdata.eva.models.forms.DateContext;
import com.bakdata.eva.models.forms.DateContextMode;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class AbsExportGenerator {

	private Dataset dataset;
	private User user;
	private Namespaces namespaces;
	
	public ManagedQuery executeQuery(AbsExportForm form, DateContextMode resultMode, boolean printUserFriendly) throws JSONException {
		QueryManager queryManager = namespaces.get(dataset.getId()).getQueryManager();
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(form.getQueryGroup());
		
		if(prerequisite.getState() != ExecutionState.DONE) {
			queryManager
				.reexecuteQuery(prerequisite)
				.awaitDone(1, TimeUnit.HOURS);
		}
		
		CDateRange rangeMask = CDateRange.of(form.getDateRange());
		
		Int2ObjectMap<List<DateContext>> entityContexts = new Int2ObjectOpenHashMap<>();
		List<DateContext> dateContexts = DateContext.generateAbsoluteContexts(rangeMask,resultMode);
		prerequisite
			.fetchContainedEntityResult()
			.forEach(cer -> entityContexts.put(
				cer.getEntityId(), 
				dateContexts
			));

		//now lets build the result
		FormQuery query = new FormQuery(entityContexts);
		CQOr root = new CQOr();
		root.setChildren(new ArrayList<>(form.getFeatures()));
		query.setRoot(root);
		
		return queryManager.createQuery(query, user);
	}
}
