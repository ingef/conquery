package com.bakdata.conquery.models.worker;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.query.FilterSearch;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.util.ConcurrentHashSet;

@Slf4j
@Data
public class MatchingStatsManager {

	private final Supplier<Collection<WorkerId>> allWorkers;
	private final FilterSearch filterSearch;

	@Getter(AccessLevel.NONE)
	private final Set<WorkerId> matchingStatsOpenWorkers = new ConcurrentHashSet<>();
	@Setter(AccessLevel.PRIVATE)
	private boolean managerMatchingStatsDone = false;

	private UUID currentTx = null;

	public void updateMatchingStats() {
		currentTx = UUID.randomUUID();
		resetMatchingStats();
		// TODO send matching stats messages
	}

	public void resetMatchingStats() {
		matchingStatsOpenWorkers.clear();
		matchingStatsOpenWorkers.addAll(allWorkers.get());
		managerMatchingStatsDone = false;
	}

	public void matchingStatsManagerFinished(UUID tx) {
		if (ensureLatestTx(tx)) {
			return;
		}

		managerMatchingStatsDone = true;

		tryFinaliseMatchingStats();
	}

	private boolean ensureLatestTx(UUID tx) {
		if(!Objects.equals(currentTx, tx)){
			log.warn("Outdated MatchingStats message received");
			return true;
		}
		return false;
	}

	public void matchingStatsWorkerFinished(WorkerId workerId, UUID tx) {
		if (ensureLatestTx(tx)) {
			return;
		}


		matchingStatsOpenWorkers.remove(workerId);

		tryFinaliseMatchingStats();
	}

	private synchronized boolean matchingStatsFinished() {
		return matchingStatsOpenWorkers.isEmpty() && managerMatchingStatsDone;
	}

	private void tryFinaliseMatchingStats() {
		if (!matchingStatsFinished()) {
			return;
		}

		getFilterSearch().shrinkSearches();

		log.debug("BEGIN counting Search totals.");

		// Precompute totals as that can be slow when doing it on-demand.

		getFilterSearch().calculateTotals();
	}
}
