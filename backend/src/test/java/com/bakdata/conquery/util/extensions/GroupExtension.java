package com.bakdata.conquery.util.extensions;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import lombok.Getter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Getter
public class GroupExtension implements BeforeAllCallback {

	private final Group group;

	public GroupExtension(MetaStorage metaStorage, String name) {
		group = new Group(name, name, metaStorage);

	}
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		group.updateStorage();
	}
}
