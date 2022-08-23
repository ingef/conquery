package com.bakdata.conquery.models.forms.mostlyai;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jackson.Jackson;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

/**
 * TODO Generate the actual endpoint request using an OpenAPI codegen
 */
@Slf4j
public class MostlyAiApi {
	private final static String HEADER_API_KEY = "X-Mostly-API-Key";
	private final static String CATALOG_ID = "catalog_id";
	private final static String TABLE_ID = "table_id";
	private final static String JOB_ID = "job_id";
	public static final String FILES_0 = "files-0";
	public static final String TOKEN = "token";
	private final WebTarget uploadTableTarget;
	private final WebTarget tableDetailsTarget;
	private final WebTarget columnIncludeTarget;
	private final WebTarget startJobTarget;
	private final WebTarget jobStatusTarget;
	private final WebTarget downloadTokenTarget;
	private final WebTarget downloadDataTarget;
	private final ObjectMapper objectMapper;
	private final String apiKey;


	public MostlyAiApi(@NonNull Client client, @NonNull URI baseUrl, @NonNull String apiKey) {
		this.apiKey = apiKey;

		client.register(MultiPartFeature.class);
		WebTarget base = client.target(baseUrl);
		uploadTableTarget = base.path("/api/jobs/adhoc/upload");
		tableDetailsTarget = base.path("/api/catalogs/{" + CATALOG_ID + "}/table-details");
		columnIncludeTarget = base.path("/api/catalogs/{" + CATALOG_ID + "}/tables/{" + TABLE_ID + "}/column/include");
		startJobTarget = base.path("/api/jobs/catalog/{" + CATALOG_ID + "}");
		jobStatusTarget = base.path("/api/jobs/{" + JOB_ID + "}/progress");
		downloadTokenTarget = base.path("/api/jobs/{" + JOB_ID + "}/download/SYNTHETIC_DATA/token");
		downloadDataTarget =
				base.path("/api/jobs/{" + JOB_ID + "}/assets/synthetic-data.zip").queryParam("token", "{" + TOKEN + "}").queryParam("user-id", "dummy-user-id");
		objectMapper = Jackson.MAPPER.copy();
	}


	public UUID uploadTable(InputStream csvStream, String fileName) {
		// Mostly seems to require a propper supported file suffix (allows csv or parquet format)
		Preconditions.checkArgument(fileName.endsWith(".csv"), "The file name needs to end on '.csv'");

		// Prepare MultiPart CSV File
		final StreamDataBodyPart csvPart = new StreamDataBodyPart(FILES_0, csvStream, fileName, new MediaType("text", "csv"));

		// Prepare MultiPart Upload Request Definition
		final UploadRequest uploadRequest = new UploadRequest(fileName, List.of(new TableSourceDefinition(fileName, FILES_0)));
		final ByteArrayOutputStream buf = new ByteArrayOutputStream();

		try {
			objectMapper.writer().writeValue(buf, uploadRequest);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		final StreamDataBodyPart
				uploadDescriptionPart =
				new StreamDataBodyPart("file-upload-request", new ByteArrayInputStream(buf.toByteArray()), "upload-request.json", MediaType.APPLICATION_JSON_TYPE);

		// Combine MultiParts
		try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
			formDataMultiPart
					.bodyPart(csvPart)
					.bodyPart(uploadDescriptionPart);

			// Send Request
			try (final Response response = uploadTableTarget.request(MediaType.APPLICATION_JSON)
															.header(HEADER_API_KEY, apiKey)
															.post(Entity.entity(formDataMultiPart, formDataMultiPart.getMediaType()))) {

				if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
					throw new RuntimeException("Table upload failed. Status: " + response.getStatusInfo().getStatusCode());
				}

				final TableUploadResponse tableUploadResponse = response.readEntity(TableUploadResponse.class);
				log.trace("Uploaded table '{}' and retrieved catalog id '{}'", fileName, tableUploadResponse.id);
				return tableUploadResponse.id;
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public List<TableDetails> getTableDetails(UUID catalogId) {
		try (final Response response = tableDetailsTarget.resolveTemplate(CATALOG_ID, catalogId)
														 .request(MediaType.APPLICATION_JSON)
														 .header(HEADER_API_KEY, apiKey)
														 .get()) {
			if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
				throw new RuntimeException("Fetching table details failed. Status: " + response.getStatusInfo().getStatusCode());
			}

			final List<TableDetails> tableDetails = response.readEntity(new GenericType<List<TableDetails>>() {
			});
			log.trace("Fetched table details for catalog id '{}'", catalogId);
			return tableDetails;
		}
	}

	public void disableColumn(UUID catalogId, UUID tableId, UUID columnId) {
		final ColumnDetail columnDetail = new ColumnDetail(columnId, null, false);
		final Invocation.Builder requestBuilder = columnIncludeTarget.resolveTemplate(CATALOG_ID, catalogId).resolveTemplate(TABLE_ID, tableId)
																	 .request(MediaType.APPLICATION_JSON)
																	 .header(HEADER_API_KEY, apiKey);
		try (final Response response = requestBuilder.put(Entity.entity(columnDetail, MediaType.APPLICATION_JSON_TYPE))) {

			if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
				throw new RuntimeException("Fetching table details failed. Status: " + response.getStatusInfo().getStatusCode());
			}

			log.trace("Disabled column in catalog_id/table_id/column_id '{}/{}/{}'", catalogId, tableId, columnId);
		}
	}

	public JobStatusResponse startJob(String jobName, UUID catalogId, Set<UUID> tableIds) {

		// For now: default settings for all tables
		final Map<UUID, DataCatalogTableGeneralSettings>
				tableSettings =
				tableIds.stream().collect(Collectors.toMap(Function.identity(), (i) -> new DataCatalogTableGeneralSettings(null, null)));

		final JobDescription jobDescription = new JobDescription(jobName, tableSettings);

		final Invocation.Builder builder = startJobTarget.resolveTemplate(CATALOG_ID, catalogId)
														 .request(MediaType.APPLICATION_JSON)
														 .header(HEADER_API_KEY, apiKey);
		try (Response response = builder.post(Entity.entity(jobDescription, MediaType.APPLICATION_JSON_TYPE))) {

			if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
				throw new RuntimeException("Starting job failed. Status: " + response.getStatusInfo().getStatusCode());
			}

			final JobStatusResponse jobStatusResponse = response.readEntity(JobStatusResponse.class);

			log.trace("Started job with id {}", jobStatusResponse.jobId);
			return jobStatusResponse;
		}
	}

	public JobStatusResponse getJobStatus(UUID jobId) {
		final Invocation.Builder builder = jobStatusTarget.resolveTemplate(JOB_ID, jobId)
														  .request(MediaType.APPLICATION_JSON)
														  .header(HEADER_API_KEY, apiKey);
		try (Response response = builder.get()) {

			if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
				throw new RuntimeException("Fetching job status failed. Status: " + response.getStatusInfo().getStatusCode());
			}

			final JobStatusResponse jobStatusResponse = response.readEntity(JobStatusResponse.class);

			log.trace("Got job progress for {}", jobStatusResponse.jobId);
			return jobStatusResponse;
		}
	}

	public void downloadSyntheticData(UUID jobId, Consumer<InputStream> inputStreamConsumer) {
		final Invocation.Builder builder = downloadTokenTarget.resolveTemplate(JOB_ID, jobId)
															  .request(MediaType.APPLICATION_JSON)
															  .header(HEADER_API_KEY, apiKey);

		// Get download token
		DownloadToken downloadToken;
		try (Response response = builder.get()) {

			if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
				throw new RuntimeException("Download token retrieval failed for job '" + jobId + "'. Status: " + response.getStatusInfo().getStatusCode());
			}

			downloadToken = response.readEntity(DownloadToken.class);
		}

		final Invocation.Builder
				request = downloadDataTarget.resolveTemplate(JOB_ID, jobId).resolveTemplate(TOKEN, downloadToken.token)
											.request(new MediaType("application", "zip"));


		try (Response response = request.get()) {

			if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
				throw new RuntimeException("Downloading synthetic data failed for job '" + jobId + "'. Status: " + response.getStatusInfo().getStatusCode());
			}

			final InputStream inputStream = response.readEntity(InputStream.class);

			inputStreamConsumer.accept(inputStream);
		}
	}

	@Data
	@RequiredArgsConstructor
	private static class UploadRequest {
		private final String name;
		private final String dataSourceId = "UPLOAD";
		private final List<TableSourceDefinition> tableSourceDefinitions;
	}

	private record TableSourceDefinition(String name, String filesParamName) {
	}

	private record TableUploadResponse(@NotNull UUID id) {
	}


	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TableDetails(
			@NotNull UUID id,
			@NotEmpty List<ColumnDetail> columns
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ColumnDetail(
			@NotNull UUID id,
			@NotEmpty String name,
			boolean include
	) {
	}

	@RequiredArgsConstructor
	@Data
	private static class JobDescription {
		private final String dataTargetId = "DOWNLOAD";
		private final String jobName;
		private final String jobType = "AD_HOC_JOB";
		private final Map<UUID, DataCatalogTableGeneralSettings> dataCatalogTableGeneralSettings;
	}

	private record DataCatalogTableGeneralSettings(@Nullable Long maxSampleSize, @Nullable Long generationSize) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record JobStatusResponse(
			@NotNull UUID jobId,
			@NotNull JobStatus status,
			@NotNull UUID catalogId,
			@NotNull String catalogName,
			boolean hasQaReport) {
	}

	;

	public enum JobStatus {NEW, QUEUED, IN_PROGRESS, DONE, CANCELING, CANCELED, ERROR}

	private record DownloadToken(@NotEmpty String token) {
	}
}
