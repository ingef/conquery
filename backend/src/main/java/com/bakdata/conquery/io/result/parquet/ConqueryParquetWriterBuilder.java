package com.bakdata.conquery.io.result.parquet;

import java.util.List;

import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.OutputFile;

/**
 * Builder class for the {@link ParquetWriter} with {@link WriteSupport} for {@link EntityResult}s included.
 */
@Setter
@Accessors(chain = true)
public class ConqueryParquetWriterBuilder extends ParquetWriter.Builder<EntityResult, ConqueryParquetWriterBuilder> {
	private List<ResultInfo> idHeaders;
	private List<ResultInfo> resultInfo;
	private PrintSettings printSettings;

	public ConqueryParquetWriterBuilder(OutputFile file) {
		super(file);
	}

	@Override
	protected ConqueryParquetWriterBuilder self() {
		return this;
	}

	@Override
	protected WriteSupport<EntityResult> getWriteSupport(Configuration conf) {
		return new EntityResultWriteSupport(idHeaders, resultInfo, printSettings);
	}
}
