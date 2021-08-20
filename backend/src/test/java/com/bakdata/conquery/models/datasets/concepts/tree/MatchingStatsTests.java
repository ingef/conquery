package com.bakdata.conquery.models.datasets.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MatchingStatsTests {
    MatchingStats stats = new MatchingStats();
    public static int NUMBER_OF_ENTRY = 10;
    //public static int NUMBER_OF_ENTITIES = 10;

    @BeforeEach
    private void fillStats() {
        Map<WorkerId, MatchingStats.Entry> entries = new HashMap<>();
        Random random = new Random();
        for (int i = 0; i < NUMBER_OF_ENTRY; i++) {

            entries.put(new WorkerId(new DatasetId("sampleDataset" + i), "worker" + i),
                    new MatchingStats.Entry(5, IntSet.of(1, 3, 4), CDateRange.of(10, 20)));
        }
        stats.setEntries(entries);

    }

    @Test
    public void entitiesCountTest() {

        assertThat(stats.countEntities()).isEqualTo(NUMBER_OF_ENTRY*3);

    }
}
