package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.util.SimpleObservable;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ImportsManager implements SimpleObservable.Observer<UUID> {


	private static ImportsManager instance;

	private ConcurrentHashMap<String, UUID> runningImportJobs;

	//will be called when an ImportJob with the given jobId terminated
	@Override
	public void onObservableChanged(UUID jobId) {
		Optional<String> importId = runningImportJobs.entrySet().stream()
				.filter(e -> e.getValue().equals(jobId))
				.map(Map.Entry::getKey)
				.findFirst();
		importId.ifPresent(s -> runningImportJobs.remove(s));
	}

	public boolean isJobRunning(String importId) {
		return runningImportJobs.containsKey(importId);
	}

	public boolean addStartedImportJob(String importId, UUID jobId) {
		if (isJobRunning(importId)) {
			return false;
		}
		runningImportJobs.put(importId, jobId);
		return true;
	}

	private ImportsManager() {
		runningImportJobs = new ConcurrentHashMap<>();
	}

	public static ImportsManager getInstance() {
		if (instance == null) {
			//synchronized block to remove overhead
			synchronized (ImportsManager.class) {
				if (instance == null) {
					// if instance is null, initialize
					instance = new ImportsManager();
				}

			}
		}
		return instance;
	}
}
