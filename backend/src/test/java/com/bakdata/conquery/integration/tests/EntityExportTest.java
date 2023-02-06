package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importInternToExternMappers;
import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.preview.EntityPreviewStatus;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.api.DatasetQueryResource;
import com.bakdata.conquery.resources.api.EntityPreviewRequest;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.description.LazyTextDescription;

/**
 * Adapted from {@link com.bakdata.conquery.integration.tests.deletion.ImportDeletionTest}, tests {@link QueryResource#getEntityData(Subject, QueryResource.EntityPreview, HttpServletRequest)}.
 */
@Slf4j
public class EntityExportTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final String testJson = In.resource("/tests/query/ENTITY_EXPORT_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();

		final QueryTest test = JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importInternToExternMappers(conquery, test.getInternToExternMappings());
			conquery.waitUntilWorkDone();

			final RequiredData content = test.getContent();
			importSecondaryIds(conquery, content.getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, content.getTables(), content.isAutoConcept());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, content.getTables());
			conquery.waitUntilWorkDone();

			LoadingUtil.updateMatchingStats(conquery);
			conquery.waitUntilWorkDone();

			final URI setPreviewConfig = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder(), AdminDatasetResource.class, "setPreviewConfig")
														.buildFromMap(Map.of(ResourceConstants.DATASET, dataset.getId()));

			final PreviewConfig previewConfig = new PreviewConfig();

			previewConfig.setInfoCardSelects(List.of(
					new PreviewConfig.InfoCardSelect("Age", SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree1.connector.age")),
					new PreviewConfig.InfoCardSelect("Values", SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree2.connector.values"))
			));

			previewConfig.setHidden(Set.of(ColumnId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "table1.column")));

			try (Response response = conquery.getClient().target(setPreviewConfig)
											 .request(MediaType.APPLICATION_JSON_TYPE)
											 .header("Accept-Language", "en-Us")
											 .post(Entity.json(previewConfig))) {

				assertThat(response.getStatusInfo().getFamily())
						.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
						.isEqualTo(Response.Status.Family.SUCCESSFUL);
			}
		}

		final URI entityExport = HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), DatasetQueryResource.class, "getEntityData")
												.buildFromMap(Map.of(ResourceConstants.DATASET, conquery.getDataset().getName()));

		// Api uses NsIdRef so we have to use the real objects here.
		final List<Connector> allConnectors = conquery.getNamespaceStorage().getAllConcepts().stream()
													  .map(Concept::getConnectors)
													  .flatMap(List::stream)
													  .collect(Collectors.toList());

		final EntityPreviewStatus result;
		try (Response allEntityDataResponse = conquery.getClient().target(entityExport)
													  .request(MediaType.APPLICATION_JSON_TYPE)
													  .header("Accept-Language", "en-Us")
													  .post(Entity.json(new EntityPreviewRequest("ID", "1", Range.atMost(LocalDate.of(2022, 11, 10)), allConnectors)))) {

			assertThat(allEntityDataResponse.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> allEntityDataResponse.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);

			result = allEntityDataResponse.readEntity(EntityPreviewStatus.class);
		}

		assertThat(result.getInfos()).isEqualTo(List.of(
				new EntityPreviewStatus.Info(
						"Age",
						"9",
						ResultType.IntegerT.INSTANCE.typeInfo(),
						"",
						Set.of(new SemanticType.SelectResultT(conquery.getDatasetRegistry()
																	  .resolve(SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree1.connector.age"))))
				),
				new EntityPreviewStatus.Info(
						"Values",
						"A1 ; B2",
						new ResultType.ListT(ResultType.StringT.INSTANCE).typeInfo(),
						"This is a column",
						Set.of(
								new SemanticType.SelectResultT(conquery.getDatasetRegistry()
																	   .resolve(SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree2.connector.values")))
						)
				)
		));

		assertThat(result.getColumnDescriptions())
				.isNotNull()
				.isNotEmpty();

		final Optional<ColumnDescriptor> t2values = result.getColumnDescriptions().stream()
														  .filter(desc -> "table2 column".equals(desc.getLabel()))
														  .findFirst();

		assertThat(t2values).isPresent();
		assertThat(t2values.get().getDescription()).isEqualTo("This is a column");
		assertThat(t2values.get().getSemantics())
				.contains(
						new SemanticType.ConceptColumnT(conquery.getDatasetRegistry()
																.resolve(ConceptId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree2")))
				);


		final Optional<URL> csvUrl = result.getResultUrls().stream()
										   .filter(url -> url.getFile().endsWith(".csv"))
										   .findFirst();

		assertThat(csvUrl).isPresent();

		try (Response resultLines = conquery.getClient().target(csvUrl.get().toURI())
											.queryParam("pretty", false)
											.request(AdditionalMediaTypes.CSV)
											.header("Accept-Language", "en-Us")
											.get()) {

			assertThat(resultLines.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> resultLines.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);


			assertThat(resultLines.readEntity(String.class).lines().collect(Collectors.toList()))
					.containsExactlyInAnyOrder(
							"result,dates,source,secondaryid,table1 column,table2 column",
							"1,{2013-11-10/2013-11-10},table1,External: oneone,tree1.child_a,",
							"1,{2012-01-01/2012-01-01},table2,2222,,tree2",
							"1,{2010-07-15/2010-07-15},table2,External: threethree,,tree2"

					);
		}


	}

}
