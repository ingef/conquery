package com.bakdata.conquery.models.events.generation;

import java.util.List;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;

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

	public Bucket adaptValuesFrom(int bucketNumber, Import outImport, Bucket value, PreprocessedHeader header){
		//TODO what does this do?
		return value;
	}

}
