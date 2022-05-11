package com.bakdata.conquery.io.result.arrow;

import static com.bakdata.conquery.io.result.arrow.ResultArrowProcessor.getArrowResult;
import static com.bakdata.conquery.resources.ResourceConstants.FILE_EXTENTION_ARROW_FILE;

import java.nio.channels.Channels;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.SingleTableResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileWriter;

@NoArgsConstructor
@AllArgsConstructor
public class ResultArrowFileProcessor {

	// From https://www.iana.org/assignments/media-types/application/vnd.apache.arrow.file
	public static final MediaType MEDIA_TYPE = new MediaType("application", "vnd.apache.arrow.file");
	@Inject
	private DatasetRegistry datasetRegistry;
	@Inject
	private ConqueryConfig config;

	public <E extends ManagedExecution<?> & SingleTableResult> Response getArrowFileResult(Subject subject, E exec, Dataset dataset, boolean pretty) {
		return getArrowResult(
				(output) -> (root) -> new ArrowFileWriter(root, new DictionaryProvider.MapDictionaryProvider(), Channels.newChannel(output)),
				subject,
				exec,
				dataset,
				datasetRegistry,
				pretty,
				FILE_EXTENTION_ARROW_FILE,
				MEDIA_TYPE,
				config);
	}
}
