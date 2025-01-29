package com.bakdata.conquery.models.datasets.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import org.junit.jupiter.api.Test;

public class MatchingStatsTests {

    private final WorkerId workerId1 = new WorkerId(new DatasetId("sampleDataset"), "sampleWorker");
    private final WorkerId workerId2 = new WorkerId(new DatasetId("sampleDataset2"), "sampleWorker2");

    @Test
    public void entitiesCountTest() {

        MatchingStats stats = new MatchingStats();

        assertThat(stats.countEntities()).isEqualTo(0);

        stats.putEntry(workerId1.toString(), new MatchingStats.Entry(5, 5, 10, 20));
        assertThat(stats.countEntities()).isEqualTo(5);

        stats.putEntry(workerId1.toString(), new MatchingStats.Entry(5, 8, 10, 20));
        assertThat(stats.countEntities()).isEqualTo(8);

        stats.putEntry(workerId2.toString(), new MatchingStats.Entry(5, 2, 10, 20));
        assertThat(stats.countEntities()).isEqualTo(10);


    }

    @Test
    public void addEventTest(){
        MatchingStats stats = new MatchingStats();
        Table table = new Table();
        table.setColumns(new Column[0]);

        assertThat(stats.countEvents()).isEqualTo(0);
        assertThat(stats.countEntities()).isEqualTo(0);


        MatchingStats.Entry entry1 =  new MatchingStats.Entry();
        entry1.addEventFromBucket("1", null, 1);
        entry1.addEventFromBucket("1", null, 2);

        entry1.addEventFromBucket("2", null, 3);
        entry1.addEventFromBucket("2", null, 4);

        entry1.addEventFromBucket("3", null, 5);
        entry1.addEventFromBucket("3", null, 6);

        entry1.addEventFromBucket("4", null, 7);
        entry1.addEventFromBucket("4", null, 8);



        stats.putEntry(workerId1.toString(), entry1);
        assertThat(stats.countEvents()).isEqualTo(8);
        assertThat(stats.countEntities()).isEqualTo(4);


        MatchingStats.Entry entry2 =  new MatchingStats.Entry();

        entry2.addEventFromBucket("1", null, 1);
        entry2.addEventFromBucket("2", null, 2);

        entry2.addEventFromBucket("3", null, 3);
        entry2.addEventFromBucket("4", null, 4);

        entry2.addEventFromBucket("5", null, 5);
        entry2.addEventFromBucket("6", null, 6);

        entry2.addEventFromBucket("7", null, 7);
        entry2.addEventFromBucket("8", null, 8);

        entry2.addEventFromBucket("9", null, 9);
        entry2.addEventFromBucket("10", null, 10);

        stats.putEntry(workerId2.toString(), entry2);
        assertThat(stats.countEvents()).isEqualTo(18);
        assertThat(stats.countEntities()).isEqualTo(14);



    }
}
