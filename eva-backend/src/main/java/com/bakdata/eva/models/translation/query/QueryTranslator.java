package com.bakdata.eva.models.translation.query;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.admin.DatasetsProcessor;
import com.bakdata.eva.models.translation.IdentifiableMocker;
import com.bakdata.eva.models.translation.query.oldmodel.OIQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("query")
@Produces(AdditionalMediaTypes.JSON)
@PermitAll
@AllArgsConstructor
public class QueryTranslator {

	public static final String NULL_LINE = "null";
	public static final String EMPTY_OBJECT = "{}";

	private static final ObjectReader reader = Jackson.MAPPER.readerFor(OIQuery.class);
	private static final ObjectWriter writer = Jackson.MAPPER.writerFor(IQuery.class);

	private final Namespaces namespaces;
	private final MasterMetaStorage metaStorage;
	private final DatasetsProcessor datasetsProcessor;

	//TODO Read File, Parse every single line as json, transform into new format, skipping null lines
	//TODO dateranges are not proper: right side -1
	//TODO ObjectReaderInjector.set(new Modifier(requestContext.getUriInfo().getPathParameters()));

	public static void main(String[] args) throws IOException {
		final File inputFile = new File(args[0]);

		final CsvParserSettings settings = new CsvParserSettings();
		final CsvFormat csvFormat = new CsvFormat();

		csvFormat.setDelimiter(';');
		csvFormat.setCharToEscapeQuoteEscaping('\\');

		settings.setFormat(csvFormat);
		settings.setMaxCharsPerColumn(-1);

		final CsvParser parser = new CsvParser(settings);
		// QueryManager.createQuery(com.bakdata.conquery.models.query.IQuery, java.util.UUID, com.bakdata.conquery.models.auth.subjects.User)
		//TODO queryId and owner are input columns

		for (String[] row : parser.iterate(inputFile)) {
			final String dataset1 = "DATASET";
			final String owner = null;
			final String id = null;
			final String queryString = row[0];

			final IQuery query = transformQuery(dataset1, queryString);

			log.trace("{}", query);
		}

	}

	//TODO read csv from http POST request
	//TODO new export of queries with dataset, id and user
	public void insertQueries(Iterable<String[]> rows) throws JSONException {
		for (String[] row : rows) {
			final String dataset = "DATASET";
			//			final User owner = metaStorage.getUser(UserId.Parser.INSTANCE.parse(""));
			final UUID id = UUID.fromString("");
			final String queryString = row[0];

			final IQuery query = transformQuery(dataset, queryString);

			namespaces.get(DatasetId.Parser.INSTANCE.parse(dataset)).getQueryManager().createQuery(query, id, null);
		}

	}

	private static IQuery transformQuery(final String dataset, final String oldQuery) {

		if (NULL_LINE.equals(oldQuery) || EMPTY_OBJECT.equals(oldQuery))
			return null;

		final OIQuery oiQuery = readOldQueryJson(oldQuery.replace("\\\\\"", ""));

		if (oiQuery == null)
			return null;

		final DatasetId datasetId = new DatasetId(dataset);
		return translateQuery(oiQuery, datasetId);
	}

	private static IQuery translateQuery(OIQuery iQuery, DatasetId datasetId) {
		try {
			return iQuery.translate(datasetId);
		}
		catch (Exception e) {
			log.error("Error translating query {}", iQuery, e);
			return null;
		}
	}

	private static String getNewQueryJsonString(IQuery value) {
		try {
			return writer.writeValueAsString(value);
		}
		catch (JsonProcessingException e) {
			log.error("Error translating query {}", value, e);
			return null;
		}
	}

	private static OIQuery readOldQueryJson(String json) {
		try {
			return reader.<OIQuery>readValue(json);
		}
		catch (IOException e) {
			log.error(json, e);
			return null;
		}
	}

	@GET
	@Path("echo")
	public LocalDate echo() {
		return LocalDate.now();
	}

	@POST
	@Path("translate")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	public void uploadQuery(@FormDataParam("file") InputStream fileInputStream, @FormDataParam("file") FormDataContentDisposition fileMetaData) {

		final CsvParserSettings settings = new CsvParserSettings();
		final CsvFormat csvFormat = new CsvFormat();

		csvFormat.setDelimiter(',');
		csvFormat.setCharToEscapeQuoteEscaping('\\');

		settings.setFormat(csvFormat);
		settings.setMaxCharsPerColumn(-1);

		final CsvParser parser = new CsvParser(settings);
		// QueryManager.createQuery(com.bakdata.conquery.models.query.IQuery, java.util.UUID, com.bakdata.conquery.models.auth.subjects.User)
		//TODO queryId and owner are input columns

		int rowNumber = 0;
		int error = 0;

		for (String[] row : parser.iterate(fileInputStream)) {
			rowNumber++;
			// 0 id
			// 1 label
			// 2 created_at
			// 3 tags
			// 4 last_used
			// 5 owner
			// 6 company
			// 7 shared
			// 8 system
			// 9 query
			// 10 original_version

			final String id = row[0];
			final String label = row[1];

			final String tags = row[3];
			final boolean shared = Objects.equals(row[7], "f");

			final String dataset = row[6];
			final String owner = row[5];
			final String queryString = row[9];

			final IQuery query = transformQuery(dataset, queryString);

			if (query == null) {
				log.warn("Query {}@{} is defective or empty: {}", id, rowNumber, queryString);
				continue;
			}

			try {
				Namespace namespace = namespaces.get(new DatasetId(dataset));

				if (namespace == null) {
					log.warn("Namespace {} does not exist, skipping it", dataset);
					continue;
				}

				final ManagedQuery managedQuery = namespace.getQueryManager()
					.createQuery(query, UUID.fromString(id), IdentifiableMocker.mockAnswer(new UserId(owner)));

				managedQuery.setLabel(label);
				managedQuery.setShared(shared);
				//TODO Wie parsed man die Tags, wenn welche vorhanden sind?
			}
			catch (Exception e) {
				log.error("Error creating query {}@{}", id, rowNumber, e);
				error++;
			}
		}

		if(error > 0)
			log.error("Had {} faulty JSONS", error);
	}
}
