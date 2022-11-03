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
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.resources.api.ResultExcelResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Data
@CPSType(base = ResultRendererProvider.class, id = "XLSX")
@Slf4j
public class ExcelResultProvider implements ResultRendererProvider {

	// Media type according to https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private boolean hidden = false;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(ResultExcelResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec));
	}

	@Override
	public void registerResultResource(DropwizardResourceConfig environment, ManagerNode manager) {

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
