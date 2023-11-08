package com.bakdata.conquery.integration.sql;

import javax.validation.Validator;

import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.sql.dialect.TestSqlDialect;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.InternalObjectMapperCreator;
import com.bakdata.conquery.mode.local.LocalNamespaceHandler;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.SqlContext;
import com.bakdata.conquery.sql.conquery.SqlExecutionManager;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.bakdata.conquery.util.support.TestSupport;
import io.dropwizard.jersey.validation.Validators;
import lombok.Value;

@Value
public class SqlStandaloneSupport implements TestSupport {

	private static final Validator VALIDATOR = Validators.newValidator();
	Dataset dataset;
	Namespace namespace;
	ConqueryConfig config;
	MetaStorage metaStorage;
	User testUser;

	CsvTableImporter tableImporter;
	SqlExecutionManager executionManager;

	public SqlStandaloneSupport(final TestSqlDialect sqlDialect, final SqlConnectorConfig sqlConfig) {
		this.dataset = new Dataset("test");
		NamespaceStorage storage = new NamespaceStorage(new NonPersistentStoreFactory(), "", VALIDATOR) {
		};
		storage.openStores(Jackson.MAPPER.copy());
		storage.updateDataset(dataset);
		config = IntegrationTests.DEFAULT_CONFIG;
		config.setSqlConnectorConfig(sqlConfig);
		config.setIdColumns(new IdColumnConfig());
		InternalObjectMapperCreator creator = new InternalObjectMapperCreator(config, getValidator());
		SqlContext context = new SqlContext(sqlConfig, sqlDialect);
		LocalNamespaceHandler localNamespaceHandler = new LocalNamespaceHandler(config, creator, context);
		DatasetRegistry<LocalNamespace> registry = new DatasetRegistry<>(0, config, creator, localNamespaceHandler);

		metaStorage = new MetaStorage(new NonPersistentStoreFactory(), registry);
		metaStorage.openStores(Jackson.MAPPER.copy());
		registry.setMetaStorage(metaStorage);
		creator.init(registry);

		testUser = getConfig().getAuthorizationRealms().getInitialUsers().get(0).createOrOverwriteUser(metaStorage);
		metaStorage.updateUser(testUser);
		namespace = registry.createNamespace(storage);
		tableImporter = new CsvTableImporter(sqlDialect.getDSLContext(), sqlDialect, sqlConfig);
		executionManager = (SqlExecutionManager) namespace.getExecutionManager();
	}

	@Override
	public Namespace getNamespace() {
		return namespace;
	}

	@Override
	public Validator getValidator() {
		return VALIDATOR;
	}

	@Override
	public MetaStorage getMetaStorage() {
		return metaStorage;
	}

	@Override
	public NamespaceStorage getNamespaceStorage() {
		return namespace.getStorage();
	}

	@Override
	public ConqueryConfig getConfig() {
		return config;
	}

	@Override
	public User getTestUser() {
		return testUser;
	}
}
