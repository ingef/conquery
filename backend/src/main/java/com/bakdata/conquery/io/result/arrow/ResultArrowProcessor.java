package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.ResultUtil.makeResponseWithFileName;
import static com.bakdata.conquery.io.result.arrow.ArrowRenderer.renderToStream;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_FILE;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_STREAM;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.io.result.ResultUtil;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ArrowServiceConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.identifiable.mapping.IdPrinter;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.io.IdColumnUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.ipc.ArrowWriter;
import org.apache.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultArrowProcessor {

	// From https://www.iana.org/assignments/media-types/application/vnd.apache.arrow.file
	public static final MediaType FILE_MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.file");
	public static final MediaType STREAM_MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.stream");

	private final DatasetRegistry datasetRegistry;
	private final ConqueryConfig conqueryConfig;

	private final ArrowServiceConfig arrowConfig;


	public Response createResultFile(Subject subject, ManagedExecution<?> exec, boolean pretty) {
		return getArrowResult(
				(output) -> (root) -> new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), Channels.newChannel(output)),
				subject,
				(ManagedExecution<?> & SingleTableResult) exec,
				datasetRegistry,
				pretty,
				FILE_EXTENTION_ARROW_FILE,
				FILE_MEDIA_TYPE,
				conqueryConfig,
				arrowConfig
		);
	}

	public Response createResultStream(Subject subject, ManagedExecution<?> exec, boolean pretty) {
		return getArrowResult(
				(output) -> (root) -> new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), output),
				subject,
				((ManagedExecution<?> & SingleTableResult) exec),
				datasetRegistry,
				pretty,
				FILE_EXTENTION_ARROW_STREAM,
				STREAM_MEDIA_TYPE,
				conqueryConfig,
				arrowConfig
		);
	}

	public static <E extends ManagedExecution<?> & SingleTableResult> Response getArrowResult(
			Function<OutputStream, Function<VectorSchemaRoot, ArrowWriter>> writerProducer,
			Subject subject,
			E exec,
			DatasetRegistry datasetRegistry,
			boolean pretty,
			String fileExtension,
			MediaType mediaType,
			ConqueryConfig config,
			ArrowServiceConfig arrowConfig) {

		ConqueryMDC.setLocation(subject.getName());

		final Dataset dataset = exec.getDataset();

		log.info("Downloading results for {} on dataset {}", exec, dataset);

		ResultUtil.authorizeExecutable(subject, exec, dataset);

		if (!(exec instanceof ManagedQuery || (exec instanceof ManagedForm && ((ManagedForm) exec).getSubQueries().size() == 1))) {
			return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Execution result is not a single Table").build();
		}

		// Get the locale extracted by the LocaleFilter


		final Namespace namespace = datasetRegistry.get(dataset.getId());
		IdPrinter idPrinter = IdColumnUtil.getIdPrinter(subject, exec, namespace, config.getIdColumns().getIds());
		final Locale locale = I18n.LOCALE.get();
		PrintSettings settings = new PrintSettings(
				pretty,
				locale,
				datasetRegistry,
				config,
				idPrinter::createId
		);


		// Collect ResultInfos for id columns and result columns
		final List<ResultInfo> resultInfosId = config.getIdColumns().getIdResultInfos();
		final List<ResultInfo> resultInfosExec = exec.getResultInfos();

		StreamingOutput out = output -> {
			try {
				renderToStream(
						writerProducer.apply(output),
						settings,
						arrowConfig,
						resultInfosId,
						resultInfosExec,
						exec.streamResults()
				);
			}
			finally {
				log.trace("DONE downloading data for `{}`", exec.getId());
			}
		};

		return makeResponseWithFileName(out, exec.getLabelWithoutAutoLabelSuffix(), fileExtension, mediaType, ResultUtil.ContentDispositionOption.ATTACHMENT);
	}


}
