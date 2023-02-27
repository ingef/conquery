package com.bakdata.conquery.models.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultExcelResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Data
@CPSType(base = ResultRendererProvider.class, id = "XLSX")
@Slf4j
public class ExcelResultProvider implements ResultRendererProvider {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private boolean hidden = false;

	@Valid
	@NotNull
	private ExcelConfig config = new ExcelConfig();

	@JsonIgnore
	private int idColumnsCount = 0;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<ResultAsset> generateResultURLs(ManagedExecution exec, UriBuilder uriBuilder, boolean allProviders) {
		// We only support/produce xlsx files with one sheet for now
		if (!(exec instanceof SingleTableResult singleExecution)) {
			log.trace("Execution result is not a single table");

			return Collections.emptyList();
		}

		// Check if the url should be hidden by default
		if (hidden && !allProviders) {
			log.trace("XLSX result urls are hidden");

			return Collections.emptyList();
		}

		// Check if resulting dimensions are possible for the xlsx format
		final long rowCount = singleExecution.resultRowCount();
		final int maxRowCount = SpreadsheetVersion.EXCEL2007.getMaxRows();
		if (rowCount + 1 /* header row*/ > maxRowCount) {

			log.trace("Row count is too high for XLSX format (is: {}, max: {}). Not producing a result URL", rowCount, maxRowCount);

			return Collections.emptyList();
		}

		final int columnCount = singleExecution.getResultInfos().size() + idColumnsCount;
		final int maxColumnCount = SpreadsheetVersion.EXCEL2007.getMaxColumns();
		if (columnCount > maxColumnCount) {

			log.trace("Column count is too high for XLSX format (is: {}, max: {}). Not producing a result URL", columnCount, maxColumnCount);

			return Collections.emptyList();
		}

		final URL resultUrl = ResultExcelResource.getDownloadURL(uriBuilder, (ManagedExecution & SingleTableResult) exec);
		log.trace("Generated URL: {}", resultUrl);

		return List.of(new ResultAsset("XLSX", resultUrl));
	}

	@Override
	public void registerResultResource(DropwizardResourceConfig environment, ManagerNode manager) {

		// Save id column count to later check if xlsx dimensions are feasible
		idColumnsCount = manager.getConfig().getIdColumns().getIdResultInfos().size();

		// inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(config).to(ExcelConfig.class);
				bindAsContract(ResultExcelProcessor.class);
			}
		});
		environment.register(ResultExcelResource.class);
	}

}
