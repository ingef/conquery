package com.bakdata.conquery.models.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.io.result.excel.ResultExcelProcessor;
import com.bakdata.conquery.models.config.ExcelPluginConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultExcelResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auto.service.AutoService;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
@AutoService(Plugin.class)
public class ExcelResultService implements ResultRendererProvider, Plugin {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");


	@Valid
	@NotNull
	private ExcelPluginConfig config = new ExcelPluginConfig();

	@JsonIgnore
	private int idColumnsCount = 0;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		// We only support/produce xlsx files with one sheet for now
		if (!(exec instanceof SingleTableResult singleExecution)) {
			log.trace("Execution result is not a single table");

			return Collections.emptyList();
		}

		// Check if the url should be hidden by default
		if (config.isHidden() && !allProviders) {
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

		final URL resultUrl = ResultExcelResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec);
		log.trace("Generated URL: {}", resultUrl);

		return List.of(resultUrl);
	}

	@Override
	public int getPriority() {
		return config.getPriority();
	}

	@Override
	public void initialize(ManagerNode managerNode) {
		// Save id column count to later check if xlsx dimensions are feasible
		idColumnsCount = managerNode.getConfig().getIdColumns().getIdResultInfos().size();

		final JerseyEnvironment jersey = managerNode.getEnvironment().jersey();

		// inject required services
		jersey.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(config).to(ExcelPluginConfig.class);
				bindAsContract(ResultExcelProcessor.class);
			}
		});
		jersey.register(ResultExcelResource.class);
	}

	@Override
	public boolean isDefault() {
		return true;
	}

	@Override
	public Optional<Class<? extends PluginConfig>> getPluginConfigClass() {
		return Optional.of(ExcelPluginConfig.class);
	}

	@Override
	public void setConfig(PluginConfig config) {
		if (config instanceof ExcelPluginConfig excelConfig) {
			this.config = excelConfig;
			return;
		}
		throw new IllegalStateException("Incompatible config provided: " + config);
	}
}
