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


    @Test
    public void entitiesCountTest() {

        MatchingStats stats = new MatchingStats();


        assertThat(stats.countEntities()).isEqualTo(0);

        Map<WorkerId, MatchingStats.Entry> entries = new HashMap<>();
        entries.put(new WorkerId(new DatasetId("sampleDataset"), "sampleWorker"), new MatchingStats.Entry(5, 5, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(5);

        entries.put(new WorkerId(new DatasetId("sampleDataset"), "sampleWorker"), new MatchingStats.Entry(5, 8, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(8);



        entries.put(new WorkerId(new DatasetId("sampleDataset2"), "sampleWorker"), new MatchingStats.Entry(5, 10, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(18);


        entries.put(new WorkerId(new DatasetId("sampleDataset2"), "sampleWorker2"), new MatchingStats.Entry(5, 2, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(20);


    }

    @Test
    public void addEventTest(){
        MatchingStats stats = new MatchingStats();
        Table table = new Table();
        table.setColumns(new Column[0]);

        assertThat(stats.countEvents()).isEqualTo(0);
        assertThat(stats.countEntities()).isEqualTo(0);

        Map<WorkerId, MatchingStats.Entry> entries = new HashMap<>();

        MatchingStats.Entry entry1 =  new MatchingStats.Entry();
        for (int i = 0; i< 3; i++)
        {
            entry1.addEvent(table, null, 1, 2+i);
        }
        entries.put(new WorkerId(new DatasetId("sampleDataset"), "sampleWorker"), entry1);
        stats.setEntries(entries);
        assertThat(stats.countEvents()).isEqualTo(3);
        assertThat(stats.countEntities()).isEqualTo(3);


        MatchingStats.Entry entry2 =  new MatchingStats.Entry();
        for (int i = 0; i< 4; i++)
        {
            entry2.addEvent(table, null, 5, 9+i);
        }
        entries.put(new WorkerId(new DatasetId("sampleDataset2"), "sampleWorker2"), entry2);
        stats.setEntries(entries);
        assertThat(stats.countEvents()).isEqualTo(7);
        assertThat(stats.countEntities()).isEqualTo(7);



    }
}
