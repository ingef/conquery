package com.bakdata.conquery.util.extentions;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import lombok.Getter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class GroupExtension implements BeforeAllCallback {
	private final MetaStorage metaStorage;

	@Getter
	private final Group group;

	public GroupExtension(MetaStorage metaStorage, String name) {
		this.metaStorage = metaStorage;
		group = new Group(name, name);

	}
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {

		group.setMetaStorage(metaStorage);
		metaStorage.addGroup(group);
	}
}
