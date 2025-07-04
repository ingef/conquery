package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.integration.common.RequiredColumn;
import com.bakdata.conquery.integration.common.RequiredTable;
import com.bakdata.conquery.integration.json.SqlTestDataImporter;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.description.LazyTextDescription;

/**
 * We test validation of Tables against SQL schemas. Specifically we're trying to ensure that errors are thrown on invalid inputs.
 */
@Slf4j
public class SqlSchemaValidationTest implements ProgrammaticIntegrationTest {
	@Override
	public Set<StandaloneSupport.Mode> forModes() {
		return Set.of(StandaloneSupport.Mode.SQL);
	}

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		StandaloneSupport support = testConquery.getSupport("dataset");

		CsvTableImporter importer = ((SqlTestDataImporter) testConquery.getTestDataImporter()).getCsvTableImporter();

		String tableName = "test_table";
		importer.createTable(
				RequiredTable.builder()
							 .primaryColumn(new RequiredColumn("pid", MajorTypeId.STRING))
							 .name(tableName)
							 .columns(new RequiredColumn[]{
									 new RequiredColumn("strCol", MajorTypeId.STRING),
									 new RequiredColumn("intCol", MajorTypeId.INTEGER)
							 })
							 .build()
		);

		final Invocation.Builder tableUploadRequest =
				support.getClient()
					   .target(HierarchyHelper.hierarchicalPath(support.defaultAdminURIBuilder(), AdminDatasetResource.class, "addTable")
											  .buildFromMap(Map.of(ResourceConstants.DATASET, support.getDataset())))
					   .request(MediaType.APPLICATION_JSON_TYPE);


		{
			// Insert table with wrong name
			Table table = new Table();
			table.setName(tableName + "_2");
			table.setPrimaryColumn(new Column() {
				{
					setName("pid");
					setType(MajorTypeId.STRING);
				}
			});
			table.setColumns(new Column[]{
					new Column() {
						{
							setName("strCol");
							setType(MajorTypeId.STRING);
						}
					},
					new Column() {
						{
							setName("intCol");
							setType(MajorTypeId.INTEGER);
						}
					},
					});

			try (final Response response = tableUploadRequest.post(Entity.json(table))) {
				assertThat(response.getStatusInfo().getFamily())
						.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
						.isEqualTo(Response.Status.Family.CLIENT_ERROR);

				assertThat(response.readEntity(String.class))
						.contains("name: SQL table test_table_2 does not exist");
			}
		}

		{
			// Insert table with wrong column name
			Table table = new Table();
			table.setName(tableName);
			table.setPrimaryColumn(new Column() {
				{
					setName("pid");
					setType(MajorTypeId.STRING);
				}
			});
			table.setColumns(new Column[]{
					new Column() {
						{
							setName("strCol_2");
							setType(MajorTypeId.STRING);
						}
					},
					new Column() {
						{
							setName("intCol");
							setType(MajorTypeId.INTEGER);
						}
					},
					});

			try (final Response response = tableUploadRequest.post(Entity.json(table))) {
				assertThat(response.getStatusInfo().getFamily())
						.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
						.isEqualTo(Response.Status.Family.CLIENT_ERROR);

				assertThat(response.readEntity(String.class))
						.contains("columns.strCol_2: SQL Column `test_table.strCol_2` does not exist");
			}
		}

		{
			// Insert table with wrong column type
			Table table = new Table();
			table.setName("test_table");
			table.setPrimaryColumn(new Column() {
				{
					setName("pid");
					setType(MajorTypeId.STRING);
				}
			});
			table.setColumns(new Column[]{
					new Column() {
						{
							setName("strCol");
							setType(MajorTypeId.DECIMAL);
						}
					},
					new Column() {
						{
							setName("intCol");
							setType(MajorTypeId.INTEGER);
						}
					},
					});

			try (final Response response = tableUploadRequest.post(Entity.json(table))) {

				assertThat(response.getStatusInfo().getFamily())
						.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
						.isEqualTo(Response.Status.Family.CLIENT_ERROR);

				assertThat(response.readEntity(String.class))
						.contains("test_table.strCol` does not match required type DECIMAL"); // I'm only testing the suffix as the underlying type is not specific.
			}
		}

		{
			// Insert valid and compliant table
			// We insert the valid table last, just so we don't have to delete it.
			Table table = new Table();
			table.setName(tableName);
			table.setPrimaryColumn(new Column() {
				{
					setName("pid");
					setType(MajorTypeId.STRING);
				}
			});

			table.setColumns(new Column[]{
					new Column() {
						{
							setName("strCol");
							setType(MajorTypeId.STRING);
						}
					},
					new Column() {
						{
							setName("intCol");
							setType(MajorTypeId.INTEGER);
						}
					},
					});

			try (final Response response = tableUploadRequest.post(Entity.json(table))) {

				assertThat(response.getStatusInfo().getFamily())
						.describedAs(new LazyTextDescription(() -> response.readEntity(String.class)))
						.isEqualTo(Response.Status.Family.SUCCESSFUL);
			}
		}

	}
}
