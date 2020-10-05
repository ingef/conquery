package com.bakdata.conquery.models.messages.namespaces.specific;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.jakewharton.byteunits.BinaryByteUnit;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

@CPSType(id = "IMPORT_BIT", base = NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Setter
public class ImportBucket extends WorkerMessage.Slow {

	@Nonnull
	@NotNull
	private final BucketId bucket;
	@NotEmpty
	private IntArrayList includedEntities = new IntArrayList();
	@NotNull
	private byte[][] bytes;


	@Override
	public void react(Worker context) throws Exception {
		getProgressReporter().setMax(includedEntities.size());
		Import imp = context.getStorage().getImport(bucket.getImp());

		BlockFactory factory = imp.getBlockFactory();
		Bucket[] buckets = new Bucket[includedEntities.size()];

		for (int index = 0; index < includedEntities.size(); index++) {
			int entity = includedEntities.getInt(index);

			try (ByteArrayInputStream input = new ByteArrayInputStream(bytes[index])) {
				// TODO: 02.10.2020 fix this
				buckets[index] = factory.readSingleValue(new NamespaceCollection() {
					@Override
					public CentralRegistry findRegistry(DatasetId dataset) throws NoSuchElementException {
						return context.getStorage().getCentralRegistry();
					}

					@Override
					public CentralRegistry getMetaRegistry() {
						return null;
					}
				},
														 context.getStorage().getDataset(), bucket.getBucket(), imp, input);

				if (input.available() > 0) {
					throw new IllegalStateException("After reading the block of "
													+ entity
													+ " there are still "
													+ input.available()
													+ " bytes remaining in its content");
				}
			}
			getProgressReporter().report(1);
		}

		context.getStorage().addBucket(factory.combine(includedEntities, buckets));
	}

	@Override
	public String toString() {
		return
				"Importing "
				+ bytes.length
				+ " entities from "
				+ BinaryByteUnit.format(Arrays.stream(bytes).mapToInt(v -> v.length).sum())
				+ " as "
				+ bucket.getImp().getTag()
				+ " into "
				+ bucket.getImp().getTable();
	}
}
