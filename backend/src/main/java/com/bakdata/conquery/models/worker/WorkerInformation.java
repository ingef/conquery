package com.bakdata.conquery.models.worker;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.specific.ForwardToWorker;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Slf4j
public class WorkerInformation extends NamedImpl<WorkerId> implements MessageSender.Transforming<WorkerMessage, MessageToShardNode> {
	@NotNull
	private DatasetId dataset;
	@NotNull
	private IntArraySet includedBuckets = new IntArraySet();
	@JsonIgnore
	private transient ShardNodeInformation connectedShardNode;
	@JsonIgnore

	@Min(0)
	private int entityBucketSize;

	public void awaitFreeJobQueue() {
		try {
			getConnectedShardNode().waitForFreeJobQueue();
		}
		catch (InterruptedException e) {
			log.error("Interrupted while waiting for worker[{}] to have free space in queue", this, e);
		}
	}


	@Override
	public WorkerId createId() {
		return new WorkerId(dataset, getName());
	}

	@Override
	public ShardNodeInformation getMessageParent() {
		return connectedShardNode;
	}

	@Override
	public MessageToShardNode transform(WorkerMessage message) {
		return ForwardToWorker.create(getId(), message);
	}
}
