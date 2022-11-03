package com.bakdata.conquery.io.result.parquet;

import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.api.ReadSupport;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.io.InputFile;

public class ConqueryParquetReaderBuilder extends ParquetReader.Builder<Group> {
	protected ConqueryParquetReaderBuilder(InputFile file) {
		super(file);
	}

	@Override
	protected ReadSupport<Group> getReadSupport() {
		return new GroupReadSupport();
	}
}
