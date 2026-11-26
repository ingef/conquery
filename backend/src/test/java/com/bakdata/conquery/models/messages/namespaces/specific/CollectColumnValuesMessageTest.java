package com.bakdata.conquery.models.messages.namespaces.specific;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.bakdata.conquery.io.AbstractSerializationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.messages.network.specific.ForwardToWorker;
import org.junit.jupiter.api.Test;

class CollectColumnValuesMessageTest extends AbstractSerializationTest {

	@Test
	void internalCommunicationRoundTripExcludesManagerNamespace() throws Exception {
		final DatasetId datasetId = new DatasetId("dataset");
		final ColumnId columnId = new ColumnId(new TableId(datasetId, "table"), "column");
		final int chunkSize = 42;
		final CollectColumnValuesMessage message = new CollectColumnValuesMessage(chunkSize, Set.of(columnId), null);
		final ForwardToWorker input = ForwardToWorker.create(new WorkerId(datasetId, "worker"), message);

		final byte[] serialized = getInternalCommunicationMapper().writeValueAsBytes(input);
		final NetworkMessage<?> deserialized = getInternalCommunicationMapper().readValue(serialized, NetworkMessage.class);

		assertThat(deserialized).isInstanceOf(ForwardToWorker.class);
		final ForwardToWorker forwarded = (ForwardToWorker) deserialized;
		assertThat(forwarded.getMessage()).isInstanceOf(CollectColumnValuesMessage.class);
		final CollectColumnValuesMessage copy = (CollectColumnValuesMessage) forwarded.getMessage();
		assertThat(copy.columValueChunkSize).isEqualTo(chunkSize);
		assertThat(copy.getColumns()).containsExactly(columnId);
	}
}
