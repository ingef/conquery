package com.bakdata.conquery.models.worker;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.specific.ForwardToWorker;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jetbrains.exodus.core.dataStructures.hash.IntHashSet;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class WorkerInformation extends NamedImpl<WorkerId> implements MessageSender.Transforming<WorkerMessage, MessageToShardNode> {
	@NotNull
	private DatasetId dataset;
	@NotNull
	private IntHashSet includedBuckets = new IntHashSet();
	@JsonIgnore
	private transient ShardNodeInformation connectedShardNode;

	@Min(0)
	private int entityBucketSize;

	@Override
	public WorkerId createId() {
		return new WorkerId(dataset, getName());
	}
	
	@JsonIgnore
	public int findLargestEntityId() {
		int max = -1;
		for(Integer bucket : includedBuckets) {			
			if(bucket > max) {
				max = bucket;
			}
		}
		return max;
	}

	@Override
	public ShardNodeInformation getMessageParent() {
		return connectedShardNode;
	}

	@Override
	public MessageToShardNode transform(WorkerMessage message) {
		return new ForwardToWorker(getId(), message);
	}
}
