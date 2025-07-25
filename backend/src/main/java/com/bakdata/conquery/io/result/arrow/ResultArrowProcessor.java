package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_FILE;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_STREAM;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.function.Function;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ArrowConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.printers.ArrowResultPrinters;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.IdColumnUtil;
import com.google.common.io.CountingOutputStream;
import io.dropwizard.util.DataSize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.ipc.ArrowWriter;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultArrowProcessor {

	// From https://www.iana.org/assignments/media-types/application/vnd.apache.arrow.file
	public static final MediaType FILE_MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.file");
	public static final MediaType STREAM_MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.stream");

	private final DatasetRegistry<?> datasetRegistry;
	private final ConqueryConfig conqueryConfig;

	private final ArrowConfig arrowConfig;


	public Response createResultFile(Subject subject, ManagedExecutionId exec, boolean pretty, OptionalLong limit) {
		return getArrowResult(
				(output) -> (root) -> new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), Channels.newChannel(output)),
				subject,
				(ManagedExecution & SingleTableResult) exec.resolve(),
				datasetRegistry,
				pretty,
				FILE_EXTENTION_ARROW_FILE,
				FILE_MEDIA_TYPE,
				conqueryConfig,
				arrowConfig,
				limit
		);
	}

	public static <E extends ManagedExecution & SingleTableResult> Response getArrowResult(
			Function<OutputStream, Function<VectorSchemaRoot, ArrowWriter>> writerProducer,
			Subject subject,
			E exec,
			DatasetRegistry<?> datasetRegistry,
			boolean pretty,
			String fileExtension,
			MediaType mediaType,
			ConqueryConfig config,
			ArrowConfig arrowConfig,
			OptionalLong limit
	) {

		ConqueryMDC.setLocation(subject.getName());

		final DatasetId datasetId = exec.getDataset();

		log.info("Downloading results for {}", datasetId);

		ResultUtil.authorizeExecutable(subject, exec);

		// Get the locale extracted by the LocaleFilter

		final Namespace namespace = datasetRegistry.get(datasetId);
		IdPrinter idPrinter = IdColumnUtil.getIdPrinter(subject, exec, namespace, config.getIdColumns().getIds());
		final Locale locale = I18n.LOCALE.get();

		PrintSettings settings = new PrintSettings(pretty, locale, namespace, config, idPrinter::createId, null);


		// Collect ResultInfos for id columns and result columns
		final List<ResultInfo> resultInfosId = config.getIdColumns().getIdResultInfos();
		final List<ResultInfo> resultInfosExec = exec.getResultInfos();

		StreamingOutput out = output -> {
			CountingOutputStream countingOutputStream = new CountingOutputStream(output);
			try {
				ArrowRenderer.renderToStream(
						writerProducer.apply(countingOutputStream),
						settings,
						arrowConfig,
						resultInfosId,
						resultInfosExec,
						exec.streamResults(limit),
						new ArrowResultPrinters()
				);
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed streaming the result for execution %s requested by %s after %s".formatted(exec.getId(),
																																  subject.getId(),
																																  DataSize.bytes(countingOutputStream.getCount())
				),
												e
				);
			}
			finally {
				log.trace("DONE downloading data for `{}` ({})", exec.getId(), DataSize.bytes(countingOutputStream.getCount()));
			}
		};

		return makeResponseWithFileName(Response.ok(out), String.join(".", exec.getLabelWithoutAutoLabelSuffix(), fileExtension), mediaType, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}

	public Response createResultStream(Subject subject, ManagedExecutionId exec, boolean pretty, OptionalLong limit) {
		return getArrowResult(
				(output) -> (root) -> new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output),
				subject,
				((ManagedExecution & SingleTableResult) exec.resolve()),
				datasetRegistry,
				pretty,
				FILE_EXTENTION_ARROW_STREAM,
				STREAM_MEDIA_TYPE,
				conqueryConfig,
				arrowConfig,
				limit
		);
	}


}
