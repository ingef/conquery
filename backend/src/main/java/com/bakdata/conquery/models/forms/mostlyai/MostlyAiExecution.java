package com.bakdata.conquery.models.forms.mostlyai;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.forms.MostlyAiForm;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.csv.CsvRenderer;
import com.bakdata.conquery.io.result.external.ExternalResult;
import com.bakdata.conquery.io.result.external.ExternalResultProcessor;
import com.bakdata.conquery.io.result.external.ExternalResultProvider;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.mapping.EntityPrintId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteForm;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.UniqueNamer;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
import it.unimi.dsi.fastutil.Pair;
import kotlin.text.Charsets;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

/**
 * An execution that submits the internal results to the Mostly-AI API and provides the
 * synthesized result as a download zip-archive.
 * <br/>
 * The internal result is intercepted in the {@link MostlyAiExecution#finish(MetaStorage, ExecutionState)}-Hook and
 * forwarded to the service.
 * <p>
 * The progress of the synthesis is tracked on every status request through a call of {@link MostlyAiExecution#setStatusBase(Subject, ExecutionStatus)}.
 * When the service signals successful completion, the execution is finalized by registering a download origin.
 * The payload of that origin is transferred to a {@link StreamingOutput} for a user to download in {@link MostlyAiExecution#getExternalResult(ExternalResultProcessor.ResultFileReference)}.
 * <p>
 * If the synthesis fails this execution is marked as failed.
 */
@CPSType(id = "MOSTLY_AI_EXECUTION", base = ManagedExecution.class)
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class MostlyAiExecution extends ManagedForm implements ExternalResult {

	final static String EXECUTION_ID = "EXECUTION_ID";
	final static String JOB_ID = "JOB_ID";
	final static String CAUSE = "CAUSE";
	final static ExternalResultProcessor.ResultFileReference RESULT_FILE_EXTENTION = new ExternalResultProcessor.ResultFileReference("synthetic-data", "zip");

	@JsonIgnore
	private UUID jobId;

	@JsonIgnore
	private PrintSettings printSettings;

	@JsonIgnore
	private MostlyAiApi api;

	@JsonIgnore
	private CSVConfig csvConfig;
	@JsonIgnore
	private List<ResultInfo> idResultInfos;

	/**
	 * Mapping of different result types (file type suffixes) to their download URLs
	 */
	@JsonIgnore
	private final Map<ExternalResultProcessor.ResultFileReference, BiConsumer<UUID, Consumer<InputStream>>> resultUrls = new HashMap<>();

	public MostlyAiExecution(MostlyAiForm form, User user, Dataset submittedDataset) {
		super(form, user, submittedDataset);
	}

	@Override
	public void doInitExecutable(@NonNull DatasetRegistry datasetRegistry, ConqueryConfig config) {
		super.doInitExecutable(datasetRegistry, config);

		// Check that an ExternalResultProvider is configured
		config.getResultProviders()
			  .stream()
			  .filter(ExternalResultProvider.class::isInstance)
			  .collect(MoreCollectors.toOptional())
			  .orElseThrow(() -> new IllegalStateException("This execution requires an ExternalResultProvider to be configured."));

		// Check that the api is available
		api =
				config.getPluginConfig(MostlyAiAPIFactory.class)
					  .orElseThrow(() -> new IllegalStateException("Cannot initialize MostlyAI execution, because not API factory was configured"))
					  .createAPI();


		csvConfig = config.getCsv();
		idResultInfos = config.getIdColumns().getIdResultInfos();

		final String[] idSubstitution = new String[idResultInfos.size()];
		Arrays.fill(idSubstitution, "");
		final EntityPrintId entityPrintId = EntityPrintId.from(idSubstitution);
		// Use US locale so numeric data is presented as stated in https://mostly.ai/synthetic-data-generator-docs/resources/csv-file-requirements/
		printSettings = new PrintSettings(true, Locale.US, datasetRegistry, config, (er) -> entityPrintId);
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public void setStatusBase(@NonNull Subject subject, @NonNull ExecutionStatus status) {
		if (getState() != ExecutionState.RUNNING || jobId == null) {
			// Either the execution finished or it is running but the synthetization part has not started yet
			super.setStatusBase(subject, status);
			return;
		}

		final MostlyAiApi.JobStatusResponse jobStatus = api.getJobStatus(jobId);

		switch (jobStatus.status()) {

			case NEW, QUEUED, IN_PROGRESS, CANCELING -> super.setStatusBase(subject, status);
			case DONE -> {
				super.finish(getStorage(), ExecutionState.DONE);
				// Register download provider(s)
				resultUrls.put(RESULT_FILE_EXTENTION, api::downloadSyntheticData);
				super.setStatusBase(subject, status);

			}
			case ERROR, CANCELED -> {
				final ConqueryError.ContextError contextError = new ConqueryError.ContextError(
						"Failed to execute synthetization. Execution Id '{" + EXECUTION_ID + "}', Job Id '{" + JOB_ID + "}', Cause '{" + CAUSE + "}'",
						Map.of(
								EXECUTION_ID, getId().toString(),
								JOB_ID, jobStatus.jobId().toString(),
								CAUSE, jobStatus.status().toString()
						),
						null
				);
				setError(contextError);
				finish(getStorage(), ExecutionState.FAILED);
				super.setStatusBase(subject, status);
			}
		}
	}

	@Override
	public WorkerMessage createExecutionMessage() {
		return new ExecuteForm(getId(), getFlatSubQueries().entrySet().stream()
														   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getQuery())));
	}

	@Override
	protected void finish(MetaStorage storage, ExecutionState executionState) {
		/*
			This method is called after all shard results have been collected (or an error occurred),
			We intercept the finishing here and send the result to the mostly platform.
			After that we mark the execution as finished.
		 */

		if (executionState != ExecutionState.DONE) {
			// Internal execution failed end execution immediately
			super.finish(storage, executionState);
			return;
		}

		try {
			startSynthetization();
		}
		catch (Exception e) {
			log.warn("Could not start synthetization job", e);
			setError(ConqueryError.asConqueryError(e));
			super.finish(getStorage(), ExecutionState.FAILED);
		}

	}

	private void startSynthetization() throws IOException {
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			try (OutputStreamWriter writer = new OutputStreamWriter(baos, Charsets.UTF_8)) {
				CsvRenderer renderer = new CsvRenderer(csvConfig.createWriter(writer), printSettings);

				final Stream<EntityResult> resultStream = subQueries.values().iterator().next().stream().flatMap(ManagedQuery::streamResults);

				renderer.toCSV(idResultInfos, ((MostlyAiForm) getSubmittedForm()).getQueryGroup().getResultInfos(), resultStream);
			}
			catch (Exception e) {
				log.debug("Could not write csv", e);
				setError(ConqueryError.asConqueryError(e));
				super.finish(getStorage(), ExecutionState.FAILED);
				return;
			}

			log.debug("Uploading table data");
			final UUID
					catalogId =
					api.uploadTable(new ByteArrayInputStream(baos.toByteArray()), ((MostlyAiForm) getSubmittedForm()).getQueryGroup()
																													 .getLabelWithoutAutoLabelSuffix()
																				  + ".csv");

			log.debug("Fetching table details");
			final List<MostlyAiApi.TableDetails> tableDetails = api.getTableDetails(catalogId);

			log.debug("Disabling identifying columns");
			UniqueNamer uniqNamer = new UniqueNamer(printSettings);
			final List<String> identifyingColumns = idResultInfos.stream().map(uniqNamer::getUniqueName).toList();
			for (MostlyAiApi.TableDetails tableDetail : tableDetails) {
				for (MostlyAiApi.ColumnDetail columnDetail : tableDetail.columns()) {
					if (identifyingColumns.contains(columnDetail.name())) {
						api.disableColumn(catalogId, tableDetail.id(), columnDetail.id());
					}
				}
			}

			log.debug("Starting job");
			jobId = api.startJob("conquery_" + getId(), catalogId, tableDetails.stream().map(MostlyAiApi.TableDetails::id).collect(Collectors.toSet())).jobId();

			log.debug("Started job with id '{}'", jobId);
		}
	}


	@Override
	public List<ExternalResultProcessor.ResultFileReference> getResultFileExtensions() {
		return List.of(RESULT_FILE_EXTENTION);
	}

	/**
	 * Returns the result file of the specified type extensions AFTER the execution was successful.
	 */
	@Override
	@JsonIgnore
	public Pair<Response.ResponseBuilder, MediaType> getExternalResult(ExternalResultProcessor.ResultFileReference resultReference) {
		final BiConsumer<UUID, Consumer<InputStream>> resultStreamProvider = resultUrls.get(resultReference);
		if (resultStreamProvider == null) {
			throw new BadRequestException("Unsupported result reference '" + resultReference + "'. The execution only supports: " + resultUrls.keySet());
		}

		final StreamingOutput streamingOutput = output -> resultStreamProvider.accept(jobId, (in) -> {
			log.debug("BEGIN downloading data for {}", jobId);
			final long bytesTransferred;
			try {
				bytesTransferred = in.transferTo(output);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			log.debug("FINISHED downloading result from Mostly AI. Bytes transferred {}", FileUtils.byteCountToDisplaySize(bytesTransferred));
		});
		return Pair.of(
				Response.ok(streamingOutput),
				new MediaType("application", "zip")
		);
	}
}
