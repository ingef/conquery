package com.bakdata.conquery.api.form.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.validation.Validator;

import com.bakdata.conquery.apiv1.FormConfigPatch;
import com.bakdata.conquery.apiv1.forms.FormConfig;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.apiv1.forms.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.apiv1.forms.export_form.AbsoluteMode;
import com.bakdata.conquery.apiv1.forms.export_form.ExportForm;
import com.bakdata.conquery.apiv1.forms.export_form.RelativeMode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.develop.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AbilitySets;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormConfigProcessor;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.bakdata.conquery.models.worker.Namespaces;
import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.dropwizard.jersey.validation.Validators;
import org.apache.commons.collections4.map.HashedMap;
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
	
	private MasterMetaStorage storageMock;
	private Namespaces namespacesMock;
	
	private Map<FormConfigId, FormConfig> configs = new ConcurrentHashMap<>();
	private Map<UserId, User> users = new HashedMap<>();
	private Map<GroupId, Group> groups = new HashedMap<>();
	
	private FormConfigProcessor processor;
	private AuthorizationController controller;
	private Validator validator = Validators.newValidatorFactory().getValidator();
	
	private Dataset dataset = new Dataset();
	private Dataset dataset1 = new Dataset();
	private DatasetId datasetId;
	private DatasetId datasetId1;
	private ExportForm form;
	
	@BeforeAll
	public void setupTestClass() throws Exception{
		SharedMetricRegistries.setDefault(FormConfigTest.class.getName());
		storageMock = Mockito.mock(MasterMetaStorage.class);

		dataset.setName("test");
		dataset1.setName("test1");
		datasetId = dataset.getId();
		datasetId1 = dataset1.getId();
		
		// Mock Configs
		doAnswer(invocation -> {
			final FormConfigId id = invocation.getArgument(0);
			return configs.get(id);
		}).when(storageMock).getFormConfig(any());
		doAnswer(invocation -> {
			final FormConfig elem = invocation.getArgument(0);
			if(configs.containsKey(elem.getId())) {
				throw new IllegalStateException("Key already existed");
			}
			configs.put(elem.getId(),elem);
			return null;
		}).when(storageMock).addFormConfig(any());
		doAnswer(invocation -> {
			final FormConfig elem = invocation.getArgument(0);
			configs.put(elem.getId(),elem);
			return null;
		}).when(storageMock).updateFormConfig(any());
		doAnswer(invocation -> {
			final FormConfigId id = invocation.getArgument(0);
			configs.remove(id);
			return null;
		}).when(storageMock).removeFormConfig(any());
		when(storageMock.getAllFormConfigs()).thenReturn(configs.values());
		
		// Mock User
		doAnswer(invocation -> {
			final UserId id = invocation.getArgument(0);
			return users.get(id);
		}).when(storageMock).getUser(any());
		doAnswer(invocation -> {
			final User elem = invocation.getArgument(0);
			users.put(elem.getId(),elem);
			return null;
		}).when(storageMock).updateUser(any());
		doAnswer(invocation -> {
			final User elem = invocation.getArgument(0);
			if(users.containsKey(elem.getId())) {
				throw new IllegalStateException("Key already existed");
			}
			users.put(elem.getId(),elem);
			return null;
		}).when(storageMock).addUser(any());
		doAnswer(invocation -> {
			final UserId id = invocation.getArgument(0);
			users.remove(id);
			return null;
		}).when(storageMock).removeUser(any());
		when(storageMock.getAllUsers()).thenReturn(users.values());
		
		// Mock Groups
		doAnswer(invocation -> {
			final GroupId id = invocation.getArgument(0);
			return groups.get(id);
		}).when(storageMock).getGroup(any());
		doAnswer(invocation -> {
			final Group elem = invocation.getArgument(0);
			groups.put(elem.getId(),elem);
			return null;
		}).when(storageMock).updateGroup(any());
		doAnswer(invocation -> {
			final GroupId id = invocation.getArgument(0);
			groups.remove(id);
			return null;
		}).when(storageMock).removeGroup(any());
		when(storageMock.getAllGroups()).thenReturn(groups.values());
		
		// Mock namespaces for translation
		namespacesMock = Mockito.mock(Namespaces.class);
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
		when(storageMock.getNamespaces()).thenReturn(namespacesMock);
		

		((MutableInjectableValues)FormConfigProcessor.getMAPPER().getInjectableValues())
		.add(NamespaceCollection.class, namespacesMock);
		processor = new FormConfigProcessor(validator, storageMock);
		controller = new AuthorizationController(new DevelopmentAuthorizationConfig(), Collections.emptyList(), storageMock);
		controller.init();
		controller.start();
	}
	
	@BeforeEach
	public void setupTest(){
		configs.clear();
		users.clear();
		groups.clear();

		form = new ExportForm();
		AbsoluteMode mode = new AbsoluteMode();
		form.setTimeMode(mode);
		form.setQueryGroup(new ManagedExecutionId(datasetId, UUID.randomUUID()));
		mode.setForm(form);
		mode.setFeatures(List.of(new CQConcept()));
	}
	
	@Test
	public void addConfigWithoutTranslation() {
		User user = new User("test","test");
		storageMock.addUser(user);
		user.addPermission(storageMock, DatasetPermission.onInstance(Ability.READ, datasetId));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), mapper.valueToTree(form));
		processor.addConfig(user, datasetId, formConfig);
		
		assertThat(configs).containsAllEntriesOf(Map.of(formConfig.getId(),formConfig));
		
		assertThat(users.get(user.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
	}
	
	@Test
	public void addConfigWithTranslation() {
		User user = new User("test","test");
		storageMock.addUser(user);
		user.addPermission(storageMock, DatasetPermission.onInstance(Ability.READ, datasetId));
		user.addPermission(storageMock, DatasetPermission.onInstance(Ability.READ, datasetId1));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), mapper.valueToTree(form));
		processor.addConfig(user, datasetId, formConfig);
		
		FormConfig translatedTestForm = formConfig.tryTranslateToDataset(namespacesMock, datasetId1, mapper).get();
		assertThat(configs).containsAllEntriesOf(Map.of(
			formConfig.getId(), formConfig,
			translatedTestForm.getId(), translatedTestForm));
		
		assertThat(users.get(user.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
		assertThat(users.get(user.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, translatedTestForm.getId()));
	}
	
	@Test
	public void deleteConfig() {
		// PREPARE
		User user = new User("test","test");
		storageMock.addUser(user);
		user.addPermission(storageMock, DatasetPermission.onInstance(Ability.READ, datasetId));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), mapper.valueToTree(form));
		
		user.addPermission(storageMock, FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
		configs.put(formConfig.getId(),formConfig);
		
		// EXECUTE
		processor.deleteConfig(user, formConfig.getId());
		
		// CHECK
		assertThat(configs).doesNotContainEntry(formConfig.getId(),formConfig);
		
		assertThat(users.get(user.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_CREATOR, formConfig.getId()));
	}
	
	@Test
	public void getConfig() {
		// PREPARE
		User user = new User("test","test");
		storageMock.addUser(user);
		user.addPermission(storageMock, DatasetPermission.onInstance(Ability.READ, datasetId));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		formConfig.setOwner(user.getId());
		user.addPermission(storageMock, FormConfigPermission.onInstance(Ability.READ, formConfig.getId()));
		configs.put(formConfig.getId(),formConfig);
		
		// EXECUTE
		 FormConfigFullRepresentation response = processor.getConfig(new DatasetId("testDataset"), user, formConfig.getId());
		
		// CHECK
		assertThat(response).isEqualToIgnoringGivenFields(FormConfigFullRepresentation.builder()
			.formType(form.getClass().getAnnotation(CPSType.class).id())
			.id(formConfig.getId())
			.label(formConfig.getLabel())
			.own(true)
			.ownerName(user.getLabel())
			.shared(false)
			.system(false)
			.tags(formConfig.getTags())
			.values(values)
			.build(),FormConfigOverviewRepresentation.Fields.createdAt);
		
	}
	
	@Test
	public void getConfigs() {
		// PREPARE
		User user = new User("test","test");
		storageMock.addUser(user);
		user.addPermission(storageMock, DatasetPermission.onInstance(Ability.READ, datasetId));
		
		ExportForm form2 = new ExportForm();
		RelativeMode mode3 = new RelativeMode();
		form2.setTimeMode(mode3);
		mode3.setForm(form);
		mode3.setFeatures(List.of(new CQConcept()));
		mode3.setOutcomes(List.of(new CQConcept()));
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		JsonNode values2 = mapper.valueToTree(form2);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		FormConfig formConfig2 = new FormConfig(form2.getClass().getAnnotation(CPSType.class).id(), values2);
		processor.addConfig(user, datasetId, formConfig);
		processor.addConfig(user, datasetId, formConfig2);
		
		// EXECUTE
		 Stream<FormConfigOverviewRepresentation> response = processor.getConfigsByFormType(user, datasetId, Optional.empty());
		
		// CHECK
		assertThat(response).containsExactlyInAnyOrder(
			FormConfigOverviewRepresentation.builder()
				.formType(form.getClass().getAnnotation(CPSType.class).id())
				.id(formConfig.getId())
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
				.id(formConfig2.getId())
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
		storageMock.addUser(user);
		user.addPermission(storageMock, DatasetPermission.onInstance(Ability.READ, datasetId));
		Group group1 = new Group("test1","test1");
		storageMock.addGroup(group1);
		Group group2 = new Group("test2","test2");
		storageMock.addGroup(group2);
		
		group1.addMember(storageMock, user);
		group2.addMember(storageMock, user);
		
		ObjectMapper mapper = FormConfigProcessor.getMAPPER();
		JsonNode values = mapper.valueToTree(form);
		FormConfig formConfig = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		processor.addConfig(user, datasetId, formConfig);
		
		// EXECUTE PART 1
		processor.patchConfig(
			 user,
			 new DatasetId("testDataset"),
			 formConfig.getId(), 
			 FormConfigPatch.builder()
				 .label("newTestLabel")
				 .tags(new String[] {"tag1", "tag2"})
				 .shared(true)
				 .groups(List.of(group1.getId()))
				 .values(new ObjectNode(mapper.getNodeFactory() , Map.of("test-Node", new TextNode("test-text"))))
			 	.build()
			 );
		
		// CHECK PART 1
		FormConfig patchedFormExpected = new FormConfig(form.getClass().getAnnotation(CPSType.class).id(), values);
		patchedFormExpected.setLabel("newTestLabel");
		patchedFormExpected.setShared(true);
		patchedFormExpected.setTags(new String[] {"tag1", "tag2"});
		patchedFormExpected.setValues(new ObjectNode(mapper.getNodeFactory() , Map.of("test-Node", new TextNode("test-text"))));
		
		assertThat(storageMock.getFormConfig(formConfig.getId())).isEqualToComparingOnlyGivenFields(patchedFormExpected,
			FormConfig.Fields.formType,
			FormConfig.Fields.label,
			FormConfig.Fields.shared,
			FormConfig.Fields.tags,
			FormConfig.Fields.values);

		assertThat(groups.get(group1.getId()).getPermissions()).contains(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
		assertThat(groups.get(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
		
		
		
		// EXECUTE PART 2 (Unshare)
		processor.patchConfig(
			 user,
			 new DatasetId("testDataset"),
			 formConfig.getId(), 
			 FormConfigPatch.builder()
				 .shared(false)
				 .groups(List.of(group1.getId(), group2.getId()))
			 	.build()
			 );
		
		// CHECK PART 2
		patchedFormExpected.setShared(false);
		
		assertThat(storageMock.getFormConfig(formConfig.getId())).isEqualToComparingOnlyGivenFields(patchedFormExpected,
			FormConfig.Fields.formType,
			FormConfig.Fields.label,
			FormConfig.Fields.shared,
			FormConfig.Fields.tags,
			FormConfig.Fields.values);

		assertThat(groups.get(group1.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
		assertThat(groups.get(group2.getId()).getPermissions()).doesNotContain(FormConfigPermission.onInstance(AbilitySets.FORM_CONFIG_SHAREHOLDER, formConfig.getId()));
	}

}
