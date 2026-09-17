package com.bakdata.conquery.mode.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.jobs.Job;
import com.bakdata.conquery.sql.conquery.SqlMatchingStats;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jooq.exception.DataAccessException;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateMatchingStatsSqlJob extends Job {

    @ToString.Exclude
    private final List<Concept<?>> concepts;
    private final Dataset dataset;

    @ToString.Exclude
    private final SqlMatchingStats matchingStats;
    @ToString.Exclude
    private final Map<ConceptId, MatchingStatsJob> jobsByConcept = new HashMap<>();


    @Override
    public void execute() throws Exception {

        log.info("BEGIN collecting SQL matching stats for {}", dataset);

        Stopwatch stopwatch = Stopwatch.createStarted();

        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(getMatchingStats().getMatchingStatsWorkers()));

        try {
            for (Concept<?> concept : concepts) {
                if (isCancelled()) {
                    break;
                }

                if (!(concept instanceof TreeConcept treeConcept)) {
                    continue;
                }

                MatchingStatsJob job = submitMatchingStatsJob(treeConcept, executorService, getMatchingStats().getMatchingStatsRetries());
                jobsByConcept.put(concept.getId(), job);
            }

            while (!jobsByConcept.isEmpty() && !isCancelled()) {
                for (MatchingStatsJob job : new ArrayList<>(jobsByConcept.values())) {
                    if (isCancelled()) {
                        break;
                    }

                    try {
                        job.future().get(30, TimeUnit.SECONDS);

                        jobsByConcept.remove(job.concept().getId());
                    } catch (TimeoutException ignored) {
                        // Check the remaining jobs and cancellation state periodically.
                    } catch (CancellationException e) {
                        jobsByConcept.remove(job.concept().getId());
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();

                        if (cause instanceof DataAccessException && job.remainingRetries() > 0) {
                            log.debug("Failed to connect to database for concept {}. Retrying with {} retries remaining.", job.concept().getId(), job.remainingRetries() - 1, (Exception) (log.isTraceEnabled() ? cause : null));

                            MatchingStatsJob retry = submitMatchingStatsJob(job.concept(), executorService, job.remainingRetries() - 1);
                            jobsByConcept.put(job.concept().getId(), retry);
                        } else {
                            log.warn("FAILED to collect SQL matching stats for {}", job.concept(), cause);
                            jobsByConcept.remove(job.concept().getId(), job);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }

                    log.debug("WAITING for {} matching stats to finish.", jobsByConcept.size());
                }
            }
        } finally {
            jobsByConcept.values().forEach(job -> job.future().cancel(true));
            executorService.shutdownNow();
        }

        log.debug("DONE collecting SQL matching stats for {} within {}", dataset, stopwatch);
    }

    @Override
    public void cancel() {
        jobsByConcept.values().forEach(job -> {
            if (!job.future().isDone()) {
                job.future.cancel(true);
            }
        });
        super.cancel();
    }

    private MatchingStatsJob submitMatchingStatsJob(TreeConcept concept, ListeningExecutorService executorService, int remainingRetries) {
        ListenableFuture<?> future = executorService.submit(() -> matchingStats.collectMatchingStatsForConcept(concept));
        return new MatchingStatsJob(concept, future, remainingRetries);
    }

    @Override
    public String getLabel() {
        return "Collect matching stats for %s (%s concepts)".formatted(dataset.getName(), concepts.size());
    }

    private record MatchingStatsJob(TreeConcept concept, ListenableFuture<?> future, int remainingRetries) {
    }
}
