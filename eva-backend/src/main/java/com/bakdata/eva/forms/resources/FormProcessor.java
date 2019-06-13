package com.bakdata.eva.forms.resources;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.apiv1.URLBuilder;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.execution.ExecutionStatus;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.eva.forms.common.Form;
import com.bakdata.eva.forms.common.StatisticForm;
import com.bakdata.eva.forms.managed.ManagedForm;
import com.bakdata.eva.forms.managed.ManagedStatisticForm;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.mashape.unirest.http.exceptions.UnirestException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Getter @Slf4j
public class FormProcessor implements Closeable {

	private final Namespaces namespaces;
	private final MasterMetaStorage storage;
	private final Map<UUID, ManagedForm> formCache = new ConcurrentHashMap<>();
	private final ListeningExecutorService pool = ConqueryConfig.getInstance().getQueries().getExecutionPool().createService("Form Executor %d");

	public ManagedForm postForm(Dataset dataset, Form form, User user) throws JSONException, IOException, UnirestException {
		form.init(namespaces, user);
		ManagedForm query;
		if(form instanceof StatisticForm) {
			query = new ManagedStatisticForm((StatisticForm)form, namespaces.get(dataset.getId()), user.getId());
		}
		else {
			query = new ManagedForm(form, namespaces.get(dataset.getId()), user.getId());
		}
		// Set abilities for submitted query
		user.addPermission(storage, new QueryPermission(AbilitySets.QUERY_CREATOR, query.getId()));
		formCache.put(query.getId().getExecution(), query);
		query.executeForm(pool);
		return query;
	}

	@Override
	public void close() throws IOException {
		pool.shutdown();
		try {
			boolean success = pool.awaitTermination(1, TimeUnit.DAYS);
			if (!success && log.isDebugEnabled()) {
				log.error("Timeout has elapsed before termination completed for executor {}", pool);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public ExecutionStatus getStatus(ManagedExecutionId id, URLBuilder url) {
		return get(id).buildStatus(url);
	}

	public ManagedForm get(ManagedExecutionId id) {
		return formCache.get(id.getExecution());
	}
}
