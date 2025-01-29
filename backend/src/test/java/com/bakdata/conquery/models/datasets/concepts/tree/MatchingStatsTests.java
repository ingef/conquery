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

		stats.putEntry(workerId1.getWorker(), new MatchingStats.Entry(5, 5, 10, 20));
		assertThat(stats.countEntities()).isEqualTo(5);

		stats.putEntry(workerId1.getWorker(), new MatchingStats.Entry(5, 8, 10, 20));
		assertThat(stats.countEntities()).isEqualTo(8);

		stats.putEntry(workerId2.getWorker(), new MatchingStats.Entry(5, 2, 10, 20));
		assertThat(stats.countEntities()).isEqualTo(10);


	}

	@Test
	public void addEventTest() {
		MatchingStats stats = new MatchingStats();
		Table table = new Table();
		table.setColumns(new Column[0]);

		assertThat(stats.countEvents()).isEqualTo(0);
		assertThat(stats.countEntities()).isEqualTo(0);


		MatchingStats.Entry entry1 = new MatchingStats.Entry();
		entry1.addEventFromBucket("1", null, 0, table );
		entry1.addEventFromBucket("1", null, 0, table );

		entry1.addEventFromBucket("2", null, 0, table );
		entry1.addEventFromBucket("2", null, 0, table );

		entry1.addEventFromBucket("3", null, 0, table );
		entry1.addEventFromBucket("3", null, 0, table );

		entry1.addEventFromBucket("4", null, 0, table );
		entry1.addEventFromBucket("4", null, 0, table );


		stats.putEntry(workerId1.getWorker(), entry1);
		assertThat(stats.countEvents()).isEqualTo(8);
		assertThat(stats.countEntities()).isEqualTo(4);


		MatchingStats.Entry entry2 = new MatchingStats.Entry();

		entry2.addEventFromBucket("1", null, 0, table );
		entry2.addEventFromBucket("2", null, 0, table );
		entry2.addEventFromBucket("3", null, 0, table );
		entry2.addEventFromBucket("4", null, 0, table );
		entry2.addEventFromBucket("5", null, 0, table );
		entry2.addEventFromBucket("6", null, 0, table );
		entry2.addEventFromBucket("7", null, 0, table );
		entry2.addEventFromBucket("8", null, 0, table );
		entry2.addEventFromBucket("9", null, 0, table );
		entry2.addEventFromBucket("10", null, 0, table );


		stats.putEntry(workerId2.getWorker(), entry2);
		assertThat(stats.countEvents()).isEqualTo(18);
		assertThat(stats.countEntities()).isEqualTo(14);


	}
}
