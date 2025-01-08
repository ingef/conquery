package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.ConqueryStorage;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;

/**
 * Tasks to list, load and invalidate (parameter = "action") all or specific storages (parameter= "storage")
 */
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

		final Map<String, ConqueryStorage> storages = new HashMap<>();

		if (metaStorage != null) {
			storages.put("metaStorage", metaStorage);
		}
		Map<String, ConqueryStorage> namespacedStorages = namespacedStorageProvider.getAllDatasetIds().stream()
																				   .collect(Collectors.toMap(DatasetId::getName, namespacedStorageProvider::getStorage));

		storages.putAll(namespacedStorages);

		// Parse actions (this might throw an IllegalArgumentException)
		List<String> rawActions = parameters.get("action");
		List<Action> actions = rawActions.stream().map(String::toUpperCase).distinct().map(Action::valueOf).toList();

		if (actions.contains(Action.LIST)) {
			// Just print out all the storages we have and return
			storages.keySet().forEach(output::println);
			if (actions.size() > 1 ) {
				log.debug("Encountered action {}. Ignoring other actions: {}", Action.LIST, actions);
			}
			return;
		}

		// Extract the requested storages
		Map<String, ConqueryStorage> requestedStorages = parameters.getOrDefault("storage", Collections.emptyList())
																   .stream()
																   .collect(Collectors.toMap(Function.identity(), storages::get));

		if (requestedStorages.isEmpty()) {
			// load all if request was empty
			requestedStorages = storages;
		}

		// Fail on unknown storages
		Sets.SetView<String> difference = Sets.difference(Set.copyOf(requestedStorages.keySet()), storages.keySet());
		if (!difference.isEmpty()) {
			output.println("Abort loading storages. Unknown storages:");
			difference.stream().map("\t%s"::formatted).forEach(output::println);
			return;
		}

		for (Map.Entry<String, ConqueryStorage> storageKV : requestedStorages.entrySet()) {

			final String storageName = storageKV.getKey();
			final ConqueryStorage conqueryStorage = storageKV.getValue();

			for (Action action : actions) {
				switch (action) {
					case LOAD -> loadStorage(output, storageName, conqueryStorage);
					case INVALIDATE_CACHE -> {
						log.info("Invalidating cache of storage {}", storageName);
						conqueryStorage.invalidateCache();
					}
					case LIST -> {
						// Do nothing (should never be reached)
					}
				}
			}

		}
	}

	private static void loadStorage(PrintWriter output, String storageName, ConqueryStorage conqueryStorage) {
		final Stopwatch timer = Stopwatch.createStarted();

		output.println("BEGIN loading %s.".formatted(storageName));

		conqueryStorage.loadData();

		output.println("DONE reloading storage %s within %s.".formatted(storageName, timer.elapsed()));
	}

	enum Action {
		LOAD,
		INVALIDATE_CACHE,
		LIST
	}
}
