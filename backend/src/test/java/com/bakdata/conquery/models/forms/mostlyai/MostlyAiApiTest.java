package com.bakdata.conquery.models.forms.mostlyai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.client.Client;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "MOSTLY_AI_SERVER", matches = "^https?://.+", disabledReason = "As long as there is no mock server, test manually against a real server")
@EnabledIfEnvironmentVariable(named = "MOSTLY_AI_API_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "MOSTLY_AI_FILE", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MostlyAiApiTest {

	private static Client CLIENT;
	private static MostlyAiApi API;
	private UUID catalogId;
	private List<MostlyAiApi.TableDetails> tableDetails;
	private static File csv;
	private Map<UUID, MostlyAiApi.JobStatusResponse> jobStati;

	@BeforeAll
	public static void beforeAll() {
		String TOKEN = System.getenv("MOSTLY_AI_API_TOKEN");
		URI SERVER = URI.create(System.getenv("MOSTLY_AI_SERVER"));
		csv = new File(System.getenv("MOSTLY_AI_FILE"));

		CLIENT = JerseyClientBuilder.createClient();
		API = new MostlyAiApi(CLIENT, SERVER, TOKEN);
	}

	@AfterAll
	public static void afterAll() {
		CLIENT.close();
	}

	@Test
	@Order(0)
	public void createCatalog() throws IOException {
		try (InputStream csvStream = new FileInputStream(csv)) {
			catalogId = API.uploadTable(csvStream, csv.getName());

		}
	}


	@Test
	@Order(1)
	public void getTableDetails() {
		tableDetails = API.getTableDetails(catalogId);
	}

	@Test
	@Order(2)
	public void disableColumns() {
		for (MostlyAiApi.TableDetails tableDetail : tableDetails) {
			for (MostlyAiApi.ColumnDetail columnDetail : tableDetail.columns()) {
				if (List.of("PID", "eGK Nummer", "KV Nummer").contains(columnDetail.name())) {
					API.disableColumn(catalogId, tableDetail.id(), columnDetail.id());
				}
			}
		}
	}


	@Test
	@Order(3)
	public void startJob() {
		jobStati = new HashMap<>();
		for (MostlyAiApi.TableDetails tableDetail : tableDetails) {
			final MostlyAiApi.JobStatusResponse jobStatusResponse = API.startJob(csv.getName(), catalogId, Set.of(tableDetail.id()));
			jobStati.put(jobStatusResponse.jobId(), jobStatusResponse);
		}
	}

	@Test
	@Order(4)
	public void trackProcess() throws InterruptedException {
		HashSet<UUID> jobsUnfinished = new HashSet<>(jobStati.keySet());

		while (!jobsUnfinished.isEmpty()) {
			for (UUID jobId : jobsUnfinished) {
				final MostlyAiApi.JobStatusResponse jobStatus = API.getJobStatus(jobId);
				if (List.of(MostlyAiApi.JobStatus.DONE, MostlyAiApi.JobStatus.CANCELED, MostlyAiApi.JobStatus.ERROR).contains(jobStatus.status())) {
					jobsUnfinished.remove(jobId);
				}
			}
			Thread.sleep(1000);
		}
	}

	@Test
	@Order(5)
	public void downloadData() {
		for (UUID jobId : jobStati.keySet()) {
			API.downloadSyntheticData(
					jobId,
					inputStream -> {
						final byte[] bytes;
						try {
							bytes = inputStream.readAllBytes();
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
						assertThat(bytes).isNotEmpty();
					}
			);
		}
	}


}
