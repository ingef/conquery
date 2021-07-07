package com.bakdata.conquery.api.form.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.apiv1.forms.Form;
import com.bakdata.conquery.apiv1.forms.FormConfigAPI;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.auth.permissions.FormPermission;
import com.bakdata.conquery.models.config.auth.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormScanner;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormType;
import com.bakdata.conquery.models.forms.managed.ManagedInternalForm;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.dropwizard.jersey.validation.Validators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;


/**
 * Tests the operation of the {@link FormConfigProcessor}.
 * Since the storage is mocked, SERDESing of the {@link FormConfig} is not tested.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class FormConfigTest {
	
	private MetaStorage storage;
	private DatasetRegistry namespacesMock;

	private FormConfigProcessor processor;
	private AuthorizationController controller;
	private Validator validator = Validators.newValidatorFactory().getValidator();
	
	private Dataset dataset = new Dataset("test");
	private Dataset dataset1 = new Dataset("test1");
	private DatasetId datasetId;
	private DatasetId datasetId1;
	private ExportForm form;
	
	@BeforeAll
	public void setupTestClass() throws Exception{

		datasetId = dataset.getId();
		datasetId1 = dataset1.getId();

		// Mock DatasetRegistry for translation
		namespacesMock = Mockito.mock(DatasetRegistry.class);
		doAnswer(invocation -> {
			throw new UnsupportedOperationException("Not yet implemented");
		}).when(namespacesMock).getOptional(any());
		doAnswer(invocation -> {
			final DatasetId id = invocation.getArgument(0);
			Namespace namespaceMock = Mockito.mock(Namespace.class);
			if(id.equals(datasetId)) {
				when(namespaceMock.getDataset()).thenReturn(dataset);
			}
			else if (id.equals(datasetId1)) {
				when(namespaceMock.getDataset()).thenReturn(dataset1);
			}
			else {
				throw new IllegalStateException("Unkown dataset id.");
			}
			return namespaceMock;
		}).when(namespacesMock).get(any(DatasetId.class));
		when(namespacesMock.getAllDatasets()).thenReturn(List.of(dataset,dataset1));
		when(namespacesMock.injectInto(any(ObjectMapper.class))).thenCallRealMethod();
		when(namespacesMock.inject(any(MutableInjectableValues.class))).thenCallRealMethod();

		storage = new MetaStorage(null, new NonPersistentStoreFactory(),  namespacesMock);


		((MutableInjectableValues)FormConfigProcessor.getMAPPER().getInjectableValues())
		.add(IdResolveContext.class, namespacesMock);
		processor = new FormConfigProcessor(validator, storage);
		controller = new AuthorizationController(storage, new DevelopmentAuthorizationConfig());
		controller.start();
	}
	
	@BeforeEach
	public void setupTest(){

		final ManagedQuery managedQuery = new ManagedQuery(null, null, dataset);
		managedQuery.setQueryId(UUID.randomUUID());

		form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		form.setQueryGroupId(managedQuery.getId());
		mode.setForm(form);
		mode.setFeatures(List.of(new CQConcept()));
	}

	@AfterEach
	public void cleanupTest() {
		storage.clear();
	}
	
	@Test
	public void addConfigWithoutTranslation() {
		User user = new User("test","test");
		storage.addUser(user);
		user.addPermission(storage, dataset.createPermission(Ability.READ.asSet()));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfigAPI formConfig = FormConfigAPI.builder()
			.formType(form.getFormType())
			.values(mapper.valueToTree(form))
			.build();

		processor.addConfig(user, dataset, formConfig);
		
		assertThat(storage.getAllFormConfigs()).containsExactly(FormConfigAPI.intern(formConfig, user, dataset));
	}

	@Test
	public void deleteConfig() {
		// PREPARE
		User user = new User("test","test");
		storage.addUser(user);
		user.addPermission(storage, DatasetPermission.onInstance(Ability.READ, datasetId));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), mapper.valueToTree(form));
		formConfig.setDataset(dataset);

		user.addPermission(storage, formConfig.createPermission(AbilitySets.FORM_CONFIG_CREATOR));
		storage.addFormConfig(formConfig);
		
		// EXECUTE
		processor.deleteConfig(user, formConfig);
		
		// CHECK
		assertThat(storage.getAllFormConfigs()).doesNotContain(formConfig);
		
		assertThat(storage.getUser(user.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
	}
	
	@Test
	public void getConfig() {
		// PREPARE
		User user = new User("test","test");
		storage.addUser(user);
		user.addPermission(storage, dataset.createPermission(Ability.READ.asSet()));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setDataset(dataset);
		formConfig.setOwner(user);
		user.addPermission(storage, formConfig.createPermission(Ability.READ.asSet()));
		storage.addFormConfig(formConfig);
		
		// EXECUTE
		 FormConfigFullRepresentation response = processor.getConfig(user, formConfig);

		// CHECK
		assertThat(response).usingRecursiveComparison()
				.ignoringFields(FormConfigOverviewRepresentation.Fields.createdAt)
				.isEqualTo(FormConfigFullRepresentation.builder()
						.formType(form.getClass().getAnnotation(CPSType.class).id())
						.id(formConfig.getId())
						.label(formConfig.getLabel())
						.own(true)
						.ownerName(user.getLabel())
						.shared(false)
						.groups(Collections.emptySet())
						.system(false)
						.tags(formConfig.getTags())
						.values(values)
						.build());

	}

	private static class TestForm extends Form {

		@Override
		public String getFormType() {
			return "test-form";
		}

		@Override
		public ManagedExecution<?> toManagedExecution(User user, Dataset submittedDataset) {
			return new ManagedInternalForm(this, user, submittedDataset);
		}

		@Override
		public Set<ManagedExecution<?>> collectRequiredQueries() {
			return Collections.emptySet();
		}

		@Override
		public void resolve(QueryResolveContext context) {

		}

		@Override
		public Map<String, List<ManagedQuery>> createSubQueries(DatasetRegistry datasets, User user, Dataset submittedDataset) {
			return Collections.emptyMap();
		}

		@Override
		public String getLocalizedTypeLabel() {
			return null;
		}

		@Override
		public void visit(Consumer<Visitable> visitor) {
			visitor.accept(this);
		}
	}
	
	@Test
	public void getConfigs() {
		// PREPARE

		User user = new User("test","test");
		storage.addUser(user);
		user.addPermission(storage, DatasetPermission.onInstance(Ability.READ, datasetId));
		user.addPermission(storage, FormPermission.onInstance(Ability.CREATE, form.getFormType()));
		
		ExportForm form2 = new ExportForm();
		RelativeMode mode3 = new RelativeMode();
		form2.setTimeMode(mode3);
		mode3.setForm(form);
		mode3.setFeatures(List.of(new CQConcept()));
		mode3.setOutcomes(List.of(new CQConcept()));

		TestForm form3 = new TestForm();
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();

		FormConfigAPI formConfig = FormConfigAPI.builder()
			.formType(form.getFormType())
			.values(mapper.valueToTree(form))
			.build();
		FormConfigAPI formConfig2 = FormConfigAPI.builder()
			.formType(form2.getFormType())
			.values(mapper.valueToTree(form2))
			.build();
		// This should not be retrieved by the user because it does not hold the Permission to create TestForms
		FormConfigAPI formConfig3 = FormConfigAPI.builder()
				.formType(form3.getFormType())
				.values(mapper.valueToTree(form2))
				.build();
		FormConfigId formId = processor.addConfig(user, dataset, formConfig).getId();
		FormConfigId formId2 = processor.addConfig(user, dataset, formConfig2).getId();
		FormConfigId _formId3 = processor.addConfig(user, dataset, formConfig3).getId();

		FormScanner.FRONTEND_FORM_CONFIGS = Map.of(form.getFormType(), new FormType(form.getFormType(), new TextNode("dummy")));

		// EXECUTE
		 Stream<FormConfigOverviewRepresentation> response = processor.getConfigsByFormType(user, dataset, Collections.emptySet());
		
		// CHECK
		assertThat(response).containsExactlyInAnyOrder(
			FormConfigOverviewRepresentation.builder()
				.formType(form.getClass().getAnnotation(CPSType.class).id())
				.id(formId)
				.label(formConfig.getLabel())
				.own(true)
				.ownerName(user.getLabel())
				.shared(false)
				.system(false)
				.tags(formConfig.getTags())
				.createdAt(formConfig.getCreationTime().atZone(ZoneId.systemDefault()))
				.build(),
			FormConfigOverviewRepresentation.builder()
				.formType(form2.getClass().getAnnotation(CPSType.class).id())
				.id(formId2)
				.label(formConfig2.getLabel())
				.own(true)
				.ownerName(user.getLabel())
				.shared(false)
				.system(false)
				.tags(formConfig2.getTags())
				.createdAt(formConfig2.getCreationTime().atZone(ZoneId.systemDefault()))
				.build());
	}
	
	@Test
	public void patchConfig() {
		// PREPARE
		User user = new User("test","test");
		storage.addUser(user);
		user.addPermission(storage, DatasetPermission.onInstance(Ability.READ, datasetId));
		Group group1 = new Group("test1","test1");
		storage.addGroup(group1);
		Group group2 = new Group("test2","test2");
		storage.addGroup(group2);
		
		group1.addMember(storage, user);
		group2.addMember(storage, user);
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfigAPI formConfig = FormConfigAPI.builder()
			.formType(form.getFormType())
			.values(values)
			.build();
		FormConfig config = processor.addConfig(user, dataset, formConfig);
		
		// EXECUTE PART 1
		processor.patchConfig(
				user,
				config,
				FormConfigPatch.builder()
							   .label("newTestLabel")
							   .tags(new String[]{"tag1", "tag2"})
							   .groups(List.of(group1.getId()))
							   .values(new ObjectNode(mapper.getNodeFactory(), Map.of("test-Node", new TextNode("test-text"))))
							   .build()
		);
		
		// CHECK PART 1
		FormConfig patchedFormExpected = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		patchedFormExpected.setDataset(dataset);
		patchedFormExpected.setFormId(config.getFormId());
		patchedFormExpected.setLabel("newTestLabel");
		patchedFormExpected.setShared(true);
		patchedFormExpected.setTags(new String[] {"tag1", "tag2"});
		patchedFormExpected.setOwner(user);
		patchedFormExpected.setValues(new ObjectNode(mapper.getNodeFactory() , Map.of("test-Node", new TextNode("test-text"))));

		final FormConfigId formId = config.getId();
		assertThat(storage.getFormConfig(formId)).usingRecursiveComparison()
														 .ignoringFields("cachedId", FormConfig.Fields.creationTime)
														 .isEqualTo(patchedFormExpected);

		assertThat(storage.getGroup(group1.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));
		assertThat(storage.getGroup(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));
		
		
		
		// EXECUTE PART 2 (Unshare)
		processor.patchConfig(
			 user,
			 config,
			 FormConfigPatch.builder()
				 .groups(List.of())
			 	.build()
			 );
		
		// CHECK PART 2
		patchedFormExpected.setShared(false);
		
		assertThat(storage.getFormConfig(formId)).usingRecursiveComparison()
														 .ignoringFields("cachedId", FormConfig.Fields.creationTime)
														 .isEqualTo(patchedFormExpected);

		assertThat(storage.getGroup(group1.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));
		assertThat(storage.getGroup(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.SHAREHOLDER, formId));
	}

}
