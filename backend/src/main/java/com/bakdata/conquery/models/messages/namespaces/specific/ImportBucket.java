package com.bakdata.conquery.models.messages.namespaces.specific;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jakewharton.byteunits.BinaryByteUnit;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@CPSType(id="IMPORT_BIT", base=NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator) @Getter @Setter
public class ImportBucket extends WorkerMessage.Slow {
	
	@Nonnull @NotNull
	private final BucketId bucket;
	@NotEmpty
	private IntArrayList includedEntities = new IntArrayList();
	@NotNull
	private byte[][] bytes;
	

	@Override
	public void react(Worker context) throws Exception {
		getProgressReporter().setMax(includedEntities.size());
		Import imp = context.getStorage().getImport(bucket.getImp());
		
		int bytePos = 0;
		
		BlockFactory factory = imp.getBlockFactory();
		
		Bucket bucket = new Bucket();
		bucket.setBucket(this.bucket.getBucket());
		bucket.setImp(imp);
		bucket.setBlocks(new Block[ConqueryConfig.getInstance().getCluster().getEntityBucketSize()]);
		
		for(int index=0;index<includedEntities.size();index++) {
			int entity = includedEntities.getInt(index);
			
			try(ByteArrayInputStream input = new ByteArrayInputStream(bytes[index])) {
				Block block = factory.readBlock(imp, input);
				block.setBucket(bucket);
				bucket.getBlocks()[bucket.getPosition(entity)] = block;
				if(input.available() > 0) {
					throw new IllegalStateException("After reading the block of "+entity+" there are still "+input.available()+" bytes remaining in its content");
				}
			}
			getProgressReporter().report(1);
		}
		context.getStorage().addBucket(bucket);
	}
	
	@Override
	public String toString() {
		return
			"Importing "
			+ bytes.length
			+ "entities from "
			+ BinaryByteUnit.format(Arrays.stream(bytes).mapToInt(v->v.length).sum())
			+ " as "
			+ bucket.getImp().getTag()
			+ " into "
			+ bucket.getImp().getTable();
	}
}
