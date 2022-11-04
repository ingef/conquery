package com.bakdata.conquery.models.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultExcelResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Data
@CPSType(base = ResultRendererProvider.class, id = "XLSX")
@Slf4j
public class ExcelResultProvider implements ResultRendererProvider {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private boolean hidden = false;

	@JsonIgnore
	private int idColumnsCount = 0;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		// We only support/produce xlsx files with one sheet for now
		if (!(exec instanceof SingleTableResult)) {
			log.trace("Execution result is not a single table");
			return Collections.emptyList();
		}

		// Check if the url should be hidden by default
		if (hidden && !allProviders) {
			log.trace("XLSX result urls are hidden");
			return Collections.emptyList();
		}

		// Check if resulting dimensions are possible for the xlsx format
		if (exec instanceof final ManagedQuery managedQuery) {
			if (managedQuery.getLastResultCount() + 1 /* Header */ > SpreadsheetVersion.EXCEL2007.getLastRowIndex()) {
				log.trace("Row count is too high for XLSX format. Not producing a result URL");
				return Collections.emptyList();
			}

			final int resultColumnCount = managedQuery.getResultInfos().size();
			if (resultColumnCount + idColumnsCount > SpreadsheetVersion.EXCEL2007.getMaxColumns()) {
				log.trace("Column count is too high for XLSX format. Not producing a result URL");
				return Collections.emptyList();

			}
		}

		final URL resultUrl = ResultExcelResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec);
		log.trace("Generated URL: {}", resultUrl);

		return List.of(resultUrl);
	}

	@Override
	public void registerResultResource(DropwizardResourceConfig environment, ManagerNode manager) {

		// Save id column count to later check if xlsx dimensions are feasible
		idColumnsCount = manager.getConfig().getFrontend().getQueryUpload().getIdResultInfos().size();

		// inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bindAsContract(ResultExcelProcessor.class);
			}
		});
		environment.register(ResultExcelResource.class);
	}

}
