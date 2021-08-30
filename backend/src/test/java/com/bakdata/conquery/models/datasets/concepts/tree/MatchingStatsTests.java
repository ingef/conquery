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

    WorkerId workerId1 = new WorkerId(new DatasetId("sampleDataset"), "sampleWorker");
    WorkerId workerId2 = new WorkerId(new DatasetId("sampleDataset2"), "sampleWorker2");
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

        entries.put(workerId2, new MatchingStats.Entry(5, 2, CDateRange.of(10, 20)));
        stats.setEntries(entries);
        assertThat(stats.countEntities()).isEqualTo(10);


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
        entry1.addEvent(table, null, 1, 1);
        entry1.addEvent(table, null, 2, 1);

        entry1.addEvent(table, null, 3, 2);
        entry1.addEvent(table, null, 4, 2);

        entry1.addEvent(table, null, 5, 3);
        entry1.addEvent(table, null, 6, 3);

        entry1.addEvent(table, null, 7, 4);
        entry1.addEvent(table, null, 8, 4);



        entries.put(workerId1, entry1);
        stats.setEntries(entries);
        assertThat(stats.countEvents()).isEqualTo(8);
        assertThat(stats.countEntities()).isEqualTo(4);


        MatchingStats.Entry entry2 =  new MatchingStats.Entry();

        entry2.addEvent(table, null, 1, 1);
        entry2.addEvent(table, null, 2, 2);

        entry2.addEvent(table, null, 3, 3);
        entry2.addEvent(table, null, 4, 4);

        entry2.addEvent(table, null, 5, 5);
        entry2.addEvent(table, null, 6, 6);

        entry2.addEvent(table, null, 7, 7);
        entry2.addEvent(table, null, 8, 8);

        entry2.addEvent(table, null, 9, 9);
        entry2.addEvent(table, null, 10, 10);

        entries.put(workerId2, entry2);
        stats.setEntries(entries);
        assertThat(stats.countEvents()).isEqualTo(18);
        assertThat(stats.countEntities()).isEqualTo(14);



    }
}
