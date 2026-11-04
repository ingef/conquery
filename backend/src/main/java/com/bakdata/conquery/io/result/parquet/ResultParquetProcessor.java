package com.bakdata.conquery.io.result.parquet;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultExportContext;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ResultParquetResource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultParquetProcessor {
	public static final MediaType PARQUET_MEDIA_TYPE = MediaType.valueOf(ResultParquetResource.PARQUET_MEDIA_TYPE_STRING);

	public Response createResultFile(ResultExportContext context) {

		log.info("Downloading results for {}", context.execution().getId());

		final StreamingOutput out = output -> {

			ParquetRenderer.writeToStream(
					output,
					context.idResultInfos(),
					context.resultInfos(),
					context.printSettings(),
					context.streamResults()
			);

		};


		return makeResponseWithFileName(Response.ok(out),
										String.join(".", context.execution().getLabelWithoutAutoLabelSuffix(), ResourceConstants.FILE_EXTENTION_PARQUET),
										PARQUET_MEDIA_TYPE,
										ResultUtil.ContentDispositionOption.ATTACHMENT
		);
	}
}
