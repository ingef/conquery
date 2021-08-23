package com.bakdata.conquery.apiv1.query;

import java.util.List;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import lombok.Data;

/**
 * Result Container for responses of {@link com.bakdata.conquery.resources.api.QueryResource#upload(User, ExternalUpload)}.
 */
@Data
public class ExternalUploadResult {
	/**
	 * Id of created execution of uploaded data.
	 */
	private final ManagedExecutionId execution;

	/**
	 * Number of successfully resolved rows.
	 */
	private final int resolved;

	/**
	 * Content of rows where no Id could be resolved.
	 */
	private final List<String[]> unresolvedId;
	/**
	 * Content of rows where dates could not be read.
	 */
	private final List<String[]> unreadableDate;
}
