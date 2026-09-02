package com.bakdata.conquery.quarkus.services;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QueryUploadService {

	public UploadResult processUpload(QueryUploadPayload payload) {
		int expectedColumns = payload.format().size();
		List<List<String>> unresolvedId = new ArrayList<>();
		List<List<String>> unreadableDate = new ArrayList<>();
		int resolved = 0;

		for (List<String> row : payload.values()) {
			if (row == null || row.isEmpty()) {
				unresolvedId.add(List.of());
				continue;
			}
			if (row.size() < expectedColumns) {
				unresolvedId.add(List.copyOf(row));
				continue;
			}
			// TODO(quarkus-migration): Implement actual ID resolution against dataset contents.
			//  We currently only validate row shape. We still need to verify that externally
			//  provided IDs are known in the selected dataset and mark unknown ones unresolved.
			resolved++;
		}

		return new UploadResult(resolved, unresolvedId, unreadableDate);
	}

	public record QueryUploadPayload(
			List<String> format,
			List<List<String>> values,
			String label
	) {
	}

	public record UploadResult(
			int resolved,
			List<List<String>> unresolvedId,
			List<List<String>> unreadableDate
	) {
	}
}
