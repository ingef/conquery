package com.bakdata.conquery.models.datasets.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MatchingStatsTests {

    private final WorkerId workerId1 = new WorkerId(new DatasetId("sampleDataset"), "sampleWorker");
    private final WorkerId workerId2 = new WorkerId(new DatasetId("sampleDataset2"), "sampleWorker2");

    @Test
    public void entitiesCountTest() {

        MatchingStats stats = new MatchingStats();


        assertThat(stats.countEntities()).isEqualTo(0);

        Map<WorkerId, MatchingStats.Entry> entries = new HashMap<>();
        entries.put(workerId1, new MatchingStats.Entry(5, 5, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(5);

        entries.put(workerId1, new MatchingStats.Entry(5, 8, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(8);


        entries.put(new WorkerId(new DatasetId("sampleDataset2"), "sampleWorker"), new MatchingStats.Entry(5, 10, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(18);


        entries.put(workerId2, new MatchingStats.Entry(5, 2, CDateRange.of(10, 20)));
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
        final int entitiesPerEntry1 = 10;
        for (int i = 0; i< entitiesPerEntry1; i++)
        {
            entry1.addEvent(table, null, 1, 2+i);
        }
        entries.put(workerId1, entry1);
        stats.setEntries(entries);
        assertThat(stats.countEvents()).isEqualTo(entitiesPerEntry1);
        assertThat(stats.countEntities()).isEqualTo(entitiesPerEntry1);


        MatchingStats.Entry entry2 =  new MatchingStats.Entry();
        final int entitiesPerEntry2 = 20;
        for (int i = 0; i< entitiesPerEntry2; i++)
        {
            entry2.addEvent(table, null, 5, 9+i);
        }
        entries.put(workerId2, entry2);
        stats.setEntries(entries);
        assertThat(stats.countEvents()).isEqualTo(entitiesPerEntry1+entitiesPerEntry2);
        assertThat(stats.countEntities()).isEqualTo(entitiesPerEntry1+entitiesPerEntry2);



    }
}
