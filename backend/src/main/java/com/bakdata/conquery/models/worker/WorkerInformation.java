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
import com.fasterxml.jackson.databind.ObjectWriter;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class WorkerInformation extends NamedImpl<WorkerId> implements MessageSender.Transforming<WorkerMessage, MessageToShardNode> {
	@NotNull
	private DatasetId dataset;
	@NotNull
	private IntArraySet includedBuckets = new IntArraySet();
	@JsonIgnore
	private transient ShardNodeInformation connectedShardNode;
	@JsonIgnore
	private transient ObjectWriter communicationWriter;

	@Min(0)
	private int entityBucketSize;


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
		return ForwardToWorker.create(getId(), message, communicationWriter);
	}
}
