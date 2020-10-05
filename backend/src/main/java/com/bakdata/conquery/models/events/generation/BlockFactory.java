package com.bakdata.conquery.models.events.generation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import it.unimi.dsi.fastutil.ints.IntList;

public class BlockFactory {

	private static Object[][] transpose(List<Object[]> rows) {
		final Object[][] transposed = new Object[rows.get(0).length][rows.size()];

		for (int x = 0; x < rows.size(); x++) {
			for (int y = 0; y < rows.get(x).length; y++) {
				transposed[y][x] = rows.get(x)[y];
			}
		}

		return transposed;
	}

	public Bucket create(Import imp, List<Object[]> rows) {
		// TODO: 01.10.2020 FK, this is obviously not optimal, data should be collected by row already
		final Object[][] byColumns = transpose(rows);

		ImportColumn[] columns = imp.getColumns();

		ColumnStore[] stores = new ColumnStore[columns.length];

		for (int colIndex = 0; colIndex < columns.length; colIndex++) {
			ImportColumn column = columns[colIndex];

			stores[colIndex] = column.getType().createStore(byColumns[colIndex]);
		}

		return Bucket.create(0, imp, stores, new int[] {0}, new int[] {rows.get(0).length });
	}

	public Bucket readSingleValue(NamespaceCollection namespaceCollection, Dataset dataset, int bucketNumber, Import imp, InputStream inputStream) throws IOException {

		final Bucket bucket = dataset.injectInto(namespaceCollection.injectInto(Jackson.BINARY_MAPPER)).readValue(inputStream, Bucket.class);
		bucket.setImp(imp);
		bucket.setBucket(bucketNumber);

		return bucket;
	}
	
	public Bucket adaptValuesFrom(int bucketNumber, Import outImport, Bucket value, PreprocessedHeader header){
		//TODO what does this do?
		return value;
	}

	public Bucket combine(IntList includedEntities, Bucket[] buckets){

		ColumnStore<?>[] newStores = new ColumnStore[buckets[0].getStores().length];

		// the order is important!

		final List<ColumnStore>[] storesByColumn = new ArrayList[buckets[0].getStores().length];

		for (int i = 0; i < storesByColumn.length; i++) {
			storesByColumn[i] = new ArrayList<>(buckets.length);
		}

		for (Bucket bucket : buckets) {
			ColumnStore[] stores = bucket.getStores();
			for (int i = 0; i < stores.length; i++) {
				storesByColumn[i].add(stores[i]);
			}
		}

		for (int column = 0; column < storesByColumn.length; column++) {
			newStores[column] = storesByColumn[column].get(0).merge(storesByColumn[column]);
		}

		final int[] ends = new int[buckets.length];

		for (int index = 0; index < buckets.length; index++) {
			final Bucket bucket = buckets[index];
			ends[index] = bucket.getBucketSize();
		}


		return Bucket.create(buckets[0].getBucket(), buckets[0].getImp(), newStores, includedEntities.toArray(new int[0]), ends);
	}
}
