package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.ConqueryStorage;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadStorageTask extends Task {

	private final MetaStorage metaStorage;
	private final NamespacedStorageProvider namespacedStorageProvider;

	public LoadStorageTask(String node, MetaStorage metaStorage, NamespacedStorageProvider namespacedStorageProvider) {
		super("load-storage-" + node);
		this.metaStorage = metaStorage;
		this.namespacedStorageProvider = namespacedStorageProvider;
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {

		Map<String, ConqueryStorage> storages = new HashMap<>();

		if (metaStorage != null) {
			storages.put("metaStorage", metaStorage);
		}
		Map<String, ConqueryStorage> namespacedStorages = namespacedStorageProvider.getAllDatasetIds().stream().collect(Collectors.toMap(DatasetId::getName, datasetId -> namespacedStorageProvider.getStorage(datasetId)));

		storages.putAll(namespacedStorages);

		Object list = parameters.get("list");
		if (list != null) {
			// Just print out all the storages we have and return
			storages.keySet().forEach(output::println);
			return;
		}

		Collection<String> requestedStorages = parameters.getOrDefault("storage", Collections.emptyList());

		if (requestedStorages == null || requestedStorages.isEmpty()) {
			// load all if request was empty
			requestedStorages = storages.keySet();
		}

		Sets.SetView<String> difference = Sets.difference(Set.copyOf(requestedStorages), storages.keySet());
		if (!difference.isEmpty()) {
			output.println("Abort loading storages. Unknown storages:");
			difference.stream().map("\t%s"::formatted).forEach(output::println);
			return;
		}

		for (String request : requestedStorages) {

			final Stopwatch timer = Stopwatch.createStarted();

			output.println("BEGIN loading %s.".formatted(request));

			storages.get(request).loadData();

			output.println("DONE reloading storage %s within %s.".formatted(request, timer.elapsed()));

		}
	}
}
