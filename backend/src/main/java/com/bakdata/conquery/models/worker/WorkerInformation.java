package com.bakdata.conquery.models.worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.mina.MessageSender;
import com.bakdata.conquery.models.identifiable.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.network.MessageToShardNode;
import com.bakdata.conquery.models.messages.network.specific.ForwardToWorker;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Slf4j
@JsonIgnoreProperties("entityBucketSize")
public class WorkerInformation extends NamespacedIdentifiable<WorkerId> implements MessageSender.Transforming<WorkerMessage, MessageToShardNode> {
	@NotNull
	private DatasetId dataset;
	@NotNull
	private IntArraySet includedBuckets = new IntArraySet();
	@JsonIgnore
	private transient ShardNodeInformation connectedShardNode;

	@Getter
	@Setter
	@ToString.Include
	@NotBlank
	private String name;


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
