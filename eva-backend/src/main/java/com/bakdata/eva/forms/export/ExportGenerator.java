package com.bakdata.eva.forms.export;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryManager;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.queries.FormQuery;
import com.bakdata.eva.forms.queries.RelativeFormQuery;
import com.bakdata.eva.models.forms.DateContext;
import com.bakdata.eva.models.forms.FeatureGroup;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExportGenerator {

	private Dataset dataset;
	private User user;
	private Namespaces namespaces;
	
	public RelativeFormQuery generate(ExportForm form) throws JSONException {
		QueryManager queryManager = namespaces.get(dataset.getId()).getQueryManager();
		ManagedQuery prerequisite = (ManagedQuery)namespaces.getMetaStorage().getExecution(form.getQueryGroup().getId());
		
		if(prerequisite.getState() != ExecutionState.DONE) {
			queryManager
				.reexecuteQuery(prerequisite)
				.awaitDone(1, TimeUnit.HOURS);
		}
		
		List<FormQuery> queries = new ArrayList<>();
		for(FeatureGroup featureGroup : FeatureGroup.values()) {
			Int2ObjectMap<List<DateContext>> entityContexts = new Int2ObjectOpenHashMap<>();
			prerequisite
				.fetchContainedEntityResult()
				.map(SinglelineContainedEntityResult.class::cast)
				.forEach(cer -> {
					List<DateContext> list = DateContext.generateRelativeContexts(
						form.getQueryGroup().getTimestamp().sample(CDateSet.parse(Objects.toString(cer.getValues()[0]))).getAsInt(), 
						form.getIndexDate(),
						form.getTimeCountBefore(),
						form.getTimeCountAfter(),
						true,
						form.getTimeUnit()
					);
					list.removeIf(dctx -> dctx.getFeatureGroup() != featureGroup);
					entityContexts.put(cer.getEntityId(), list);
				});
	
			FormQuery query = new FormQuery(entityContexts);
			CQOr root = new CQOr();
			if(featureGroup == FeatureGroup.FEATURE)
				root.setChildren(new ArrayList<>(form.getFeatures()));
			else
				root.setChildren(new ArrayList<>(form.getOutcomes()));
			query.setRoot(root);
			queries.add(query);
		}
		return new RelativeFormQuery(queries.get(0), queries.get(1));
	}
	
	public ManagedQuery execute(ExportForm form, boolean printUserFriendly) throws JSONException {
		QueryManager queryManager = namespaces.get(dataset.getId()).getQueryManager();
		return queryManager.createQuery(generate(form), user);
	}
}
