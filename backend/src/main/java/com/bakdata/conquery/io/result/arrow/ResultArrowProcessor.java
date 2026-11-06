package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_FILE;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_STREAM;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.function.Function;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultExportContext;
import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.query.resultinfo.printers.ColumnarResultPrinters;
import com.google.common.io.CountingOutputStream;
import io.dropwizard.util.DataSize;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.ipc.ArrowWriter;

@Slf4j
public class ResultArrowProcessor {

	// From https://www.iana.org/assignments/media-types/application/vnd.apache.arrow.file
	public static final MediaType FILE_MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.file");
	public static final MediaType STREAM_MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.stream");

	private Response getArrowResult(
			Function<OutputStream, Function<VectorSchemaRoot, ArrowWriter>> writerProducer,
			String fileExtension,
			MediaType mediaType,
			ResultExportContext context,
			int batchSize
	) {

		log.info("Downloading results for {}", context.execution().getId());

		StreamingOutput out = output -> {
			CountingOutputStream countingOutputStream = new CountingOutputStream(output);
			try {
				ArrowRenderer.renderToStream(
						writerProducer.apply(countingOutputStream),
						context.printSettings(),
						context.idResultInfos(),
						context.resultInfos(),
						context.streamResults(),
						new ColumnarResultPrinters(),
						batchSize
				);
			} catch (Exception e) {
				throw new IllegalStateException("Failed streaming the result for execution %s requested by %s after %s".formatted(context.execution().getId(),
						context.subject().getId(),
						DataSize.bytes(countingOutputStream.getCount())
				),
						e
				);
			} finally {
				log.trace("DONE downloading data for `{}` ({})", context.execution().getId(), DataSize.bytes(countingOutputStream.getCount()));
			}
		};

		return makeResponseWithFileName(Response.ok(out), String.join(".", context.execution().getLabelWithoutAutoLabelSuffix(), fileExtension), mediaType, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}

	public Response createResultFile(ResultExportContext context, int batchSize) {
		return getArrowResult(
				(output) -> (root) -> new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), Channels.newChannel(output)),
				FILE_EXTENTION_ARROW_FILE,
				FILE_MEDIA_TYPE,
				context,
				batchSize
		);
	}

	public Response createResultStream(ResultExportContext context, int batchSize) {
		return getArrowResult(
				(output) -> (root) -> new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output),
				FILE_EXTENTION_ARROW_STREAM,
				STREAM_MEDIA_TYPE,
				context,
				batchSize
		);
	}


}
