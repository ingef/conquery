package com.bakdata.conquery.models.datasets.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

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
	public void addEventTest() {
		MatchingStats stats = new MatchingStats();


		assertThat(stats.countEvents()).isEqualTo(0);
		assertThat(stats.countEntities()).isEqualTo(0);


		MatchingStats.Entry entry1 = new MatchingStats.Entry();
		entry1.addEvents("1", 1, null);
		entry1.addEvents("1", 1, null);

		entry1.addEvents("2", 1, null);
		entry1.addEvents("2", 1, null);

		entry1.addEvents("3", 1, null);
		entry1.addEvents("3", 1, null);

		entry1.addEvents("4", 1, null);
		entry1.addEvents("4", 1, null);


		stats.putEntry(workerId1.toString(), entry1);
		assertThat(stats.countEvents()).isEqualTo(8);
		assertThat(stats.countEntities()).isEqualTo(4);


		MatchingStats.Entry entry2 = new MatchingStats.Entry();

		entry2.addEvents("1", 1, null);
		entry2.addEvents("2", 1, null);
		entry2.addEvents("3", 1, null);
		entry2.addEvents("4", 1, null);
		entry2.addEvents("5", 1, null);
		entry2.addEvents("6", 1, null);
		entry2.addEvents("7", 1, null);
		entry2.addEvents("8", 1, null);
		entry2.addEvents("9", 1, null);
		entry2.addEvents("10", 1, null);


		stats.putEntry(workerId2.toString(), entry2);
		assertThat(stats.countEvents()).isEqualTo(18);
		assertThat(stats.countEntities()).isEqualTo(14);


	}
}
