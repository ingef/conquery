package com.bakdata.conquery.models.datasets.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

public class MatchingStatsTests {
    MatchingStats stats = new MatchingStats();

    @BeforeEach
    private void fillStats()
    {
        stats.setEntries(Map.of(new WorkerId(new DatasetId("sampleDataset1"),"worker1"),
                new MatchingStats.Entry()
                {{
                    this.setNumberOfEvents(5);
                    this.getFoundEntities().addAll(Set.of(1, 3, 4));
                    this.setSpan(CDateRange.of(10, 20));
                }}
        ));
    }

    @Test
    public void entitiesCountTest()
    {
        assertThat(stats.countEntities()).isEqualTo(3);
    }
}
