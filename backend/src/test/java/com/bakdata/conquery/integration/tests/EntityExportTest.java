package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importInternToExternMappers;
import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.common.RequiredData;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.preview.EntityPreviewStatus;
import com.bakdata.conquery.models.types.ResultType;
import com.bakdata.conquery.models.types.SemanticType;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.api.DatasetQueryResource;
import com.bakdata.conquery.resources.api.EntityPreviewRequest;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.description.LazyTextDescription;

@Slf4j
public class EntityExportTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final String testJson = In.resource("/tests/query/ENTITY_EXPORT_TESTS/SIMPLE_TREECONCEPT_Query.json").withUTF8().readAll();

		final DatasetId dataset = conquery.getDataset();

		final QueryTest test = JsonIntegrationTest.readJson(dataset, testJson);

		final Range<LocalDate> dateRange = Range.of(LocalDate.of(2010, 1, 11), LocalDate.of(2022, 12, 31));


		// Manually import data, so we can do our own work.
		final SelectId valuesSelectId = SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree2.connector.values");
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
														.buildFromMap(Map.of(ResourceConstants.DATASET, dataset));

			final PreviewConfig previewConfig = new PreviewConfig();

			previewConfig.setInfoCardSelects(List.of(
					new PreviewConfig.InfoCardSelect("Age", SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree1.connector.age"), null),
					new PreviewConfig.InfoCardSelect("Values", valuesSelectId, null)
			));

			previewConfig.setTimeStratifiedSelects(List.of(new PreviewConfig.TimeStratifiedSelects(
					"Values in Time", "Description",
					List.of(new PreviewConfig.InfoCardSelect(
							"Values",
							valuesSelectId,
							"Description"
					))
			)));

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

		final EntityPreviewStatus result;
		try (Response allEntityDataResponse = conquery.getClient().target(entityExport)
													  .request(MediaType.APPLICATION_JSON_TYPE)
													  .header("Accept-Language", "en-Us")
													  .post(Entity.json(new EntityPreviewRequest("ID", "1", dateRange,
																								 List.of(
																										 ConnectorId.Parser.INSTANCE.parse(dataset.getName() + ".tree1.connector"),
																										 ConnectorId.Parser.INSTANCE.parse(dataset.getName() + ".tree2.connector")
																								 )
													  )))) {

			assertThat(allEntityDataResponse.getStatusInfo().getFamily())
					.describedAs(new LazyTextDescription(() -> allEntityDataResponse.readEntity(String.class)))
					.isEqualTo(Response.Status.Family.SUCCESSFUL);

			result = allEntityDataResponse.readEntity(EntityPreviewStatus.class);
		}

		final EntityPreviewStatus.TimeStratifiedInfos infos = result.getTimeStratifiedInfos().get(0);

		assertThat(infos.description()).isEqualTo("Description");
		assertThat(infos.label()).isEqualTo("Values in Time");

		assertThat(infos.years()).hasSize(13);

		assertThat(infos.columns()).containsExactly(
				new ColumnDescriptor(
						"Values", "Values", "Description", "LIST[STRING]",
						Set.of(new SemanticType.SelectResultT(
								valuesSelectId
						))
				)
		);


		// assert only 2010 as the other years are empty
		assertThat(infos.years().get(0))
				.isEqualTo(new EntityPreviewStatus.YearEntry(
						2010, Map.of("Values", List.of("B2")),
						List.of(
								new EntityPreviewStatus.QuarterEntry(1, Collections.emptyMap()),
								new EntityPreviewStatus.QuarterEntry(2, Collections.emptyMap()),
								new EntityPreviewStatus.QuarterEntry(3, Map.of("Values", List.of("B2"))),
								new EntityPreviewStatus.QuarterEntry(4, Collections.emptyMap())
						)
				));

		assertThat(result.getTimeStratifiedInfos().get(0).totals()).isEqualTo(
				Map.of("Values", List.of("A1", "B2"))
		);


		assertThat(result.getInfos()).containsExactly(
				new EntityPreviewStatus.Info(
						"Age",
						9,
						ResultType.Primitive.INTEGER.typeInfo(),
						null,
						Set.of(new SemanticType.SelectResultT(SelectId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree1.connector.age")))
				),
				new EntityPreviewStatus.Info(
						"Values",
						List.of("A1", "B2"),
						new ResultType.ListT(ResultType.Primitive.STRING).typeInfo(),
						null,
						Set.of(
								new SemanticType.SelectResultT(valuesSelectId)
						)
				)
		);


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
						new SemanticType.ConceptColumnT(ConceptId.Parser.INSTANCE.parsePrefixed(dataset.getName(), "tree2"))
				);


		final Optional<URI> csvUrl = result.getResultUrls().stream()
										   .map(ResultAsset::url)
										   .filter(url -> url.getPath().endsWith(".csv"))
										   .findFirst();

		assertThat(csvUrl).isPresent();

		try (Response resultLines = conquery.getClient().target(csvUrl.get())
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
							"1,{2013-11-10/2013-11-10},table1,External: oneone,EntityExportTest.tree1.child_a,",
							"1,{2012-01-01/2012-01-01},table2,2222,,EntityExportTest.tree2",
							"1,{2010-07-15/2010-07-15},table2,External: threethree,,EntityExportTest.tree2"

					);
		}
	}

}
