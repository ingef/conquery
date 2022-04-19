package com.bakdata.conquery.models.config;

import static com.bakdata.conquery.io.result.arrow.ResultArrowProcessor.getArrowResult;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_FILE;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.result.ResultRender.ResultRendererProvider;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.api.ResultArrowFileResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

@Data
@CPSType(base = ResultRendererProvider.class, id = "ARROW_FILE")
public class ArrowFileResultProvider implements ResultRendererProvider {

	// From https://www.iana.org/assignments/media-types/application/vnd.apache.arrow.file
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.file");
	@JsonIgnore
	private DatasetRegistry datasetRegistry;
	@JsonIgnore
	private ConqueryConfig config;

	private boolean hidden = true;

	@Override
	@SneakyThrows(MalformedURLException.class)
	public Collection<URL> generateResultURLs(ManagedExecution<?> exec, UriBuilder uriBuilder, boolean allProviders) {
		if (!(exec instanceof SingleTableResult)) {
			return Collections.emptyList();
		}

		if (hidden && !allProviders) {
			return Collections.emptyList();
		}

		return List.of(ResultArrowFileResource.getDownloadURL(uriBuilder, (ManagedExecution<?> & SingleTableResult) exec));
	}

	@Override
	public Response createResult(Subject subject, ManagedExecution<?> exec, Dataset dataset, boolean pretty, Charset charset, Runnable onClose) {
		return getArrowResult(
				(output) -> (root) -> new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), Channels.newChannel(output)),
				subject,
				((ManagedExecution<?> & SingleTableResult) exec),
				dataset,
				datasetRegistry,
				pretty,
				FILE_EXTENTION_ARROW_FILE,
				MEDIA_TYPE,
				config, onClose
		);
	}

	@Override
	public void registerResultResource(JerseyEnvironment environment, ManagerNode manager) {
		setConfig(manager.getConfig());
		setDatasetRegistry(manager.getDatasetRegistry());

		ArrowFileResultProvider me = this;

		//inject required services
		environment.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(me).to(ArrowFileResultProvider.class);
			}
		});

		environment.register(ResultArrowFileResource.class);
	}
}
