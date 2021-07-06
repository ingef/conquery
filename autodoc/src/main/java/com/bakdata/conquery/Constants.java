package com.bakdata.conquery;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.FilterTemplate;
import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.apiv1.MetaDataPatch;
import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRef;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRefCollection;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.jackson.serializer.NsIdRefCollection;
import com.bakdata.conquery.model.Base;
import com.bakdata.conquery.model.Group;
import com.bakdata.conquery.apiv1.frontend.FERoot;
import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.models.config.auth.AuthenticationConfig;
import com.bakdata.conquery.models.config.auth.AuthorizationConfig;
import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.datasets.concepts.conditions.CTCondition;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.datasets.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.config.APIConfig;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.config.ClusterConfig;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.FrontendConfig;
import com.bakdata.conquery.models.config.LocaleConfig;
import com.bakdata.conquery.models.config.MinaConfig;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.config.PreprocessingConfig;
import com.bakdata.conquery.models.config.QueryConfig;
import com.bakdata.conquery.models.config.StandaloneConfig;
import com.bakdata.conquery.models.config.XodusConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.apiv1.ExecutionStatus;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.apiv1.OverviewExecutionStatus;
import com.bakdata.conquery.models.forms.configs.FormConfig;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigFullRepresentation;
import com.bakdata.conquery.models.forms.configs.FormConfig.FormConfigOverviewRepresentation;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.preproc.TableImportDescriptor;
import com.bakdata.conquery.models.preproc.TableInputDescriptor;
import com.bakdata.conquery.models.preproc.outputs.OutputDescription;
import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.filter.ValidityDateContainer;
import com.bakdata.conquery.resources.api.APIResource;
import com.bakdata.conquery.resources.api.ConceptResource;
import com.bakdata.conquery.resources.api.ConceptsProcessor;
import com.bakdata.conquery.resources.api.ConfigResource;
import com.bakdata.conquery.resources.api.DatasetResource;
import com.bakdata.conquery.resources.api.FilterResource;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.api.ResultCsvResource;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import io.dropwizard.util.DataSize;
import io.dropwizard.util.Duration;

public class Constants {
	public static final Group[] GROUPS = {
			Group.builder().name("Concept JSONs")
				 .description("Each `*.concept.json` has to contain exactly one [Concept](#Base-Concept).")
				 .base(new Base(Concept.class, "A concept is a collection of filters and selects and their connection to tables."))
				 .base(new Base(CTCondition.class, "These represent guard conditions. A value matches a [ConceptElement](#ConceptElement) if it matches its condition and its parent"))
				 .base(new Base(Filter.class, "These are used to define filters, than can be used to reduce the result set."))
				 .base(new Base(Select.class, "These are used to define selects, that can be used to create additional CSV columns."))
				 .otherClass(Connector.class)
				 .otherClass(KeyValue.class)
				 .otherClass(ConceptTreeChild.class)
				 .otherClass(FilterTemplate.class)
				 .otherClass(ValidityDate.class)
				 .markerInterface(UniversalSelect.class)
					.build(),
			Group.builder().name("Import JSONs")
				 .description("Each `*.import.json` has to contain exactly one [ImportDescriptor](#Type-ImportDescriptor).")
				 .base(new Base(OutputDescription.class, ""))
				 .otherClass(TableImportDescriptor.class)
				 .otherClass(TableInputDescriptor.class)
					.build(),
			Group.builder().name("Table JSONs")
				 .description("Each `*.table.json` has to contain exactly one [Tabel](#Type-Tabel).")
				 .otherClass(Table.class)
				 .otherClass(Column.class)
					.build(),
			Group.builder().name("Config JSON")
				 .description("The `config.json` is required for every type of execution. Its root element is a [ConqueryConfig](#Type-ConqueryConfig) object.")
				 .base(new Base(AuthorizationConfig.class, "An `AuthorizationConfig` defines the initial users that are created on application startup and other permission related options."))
				 .base(new Base(AuthenticationConfig.class, "An `AuthenticationConfig` is used to define how specific realms for authentication are configured."))
				 .base(new Base(PluginConfig.class, "A `PluginConfig` is used to define settings for Conquery plugins."))
				 .base(new Base(IdMappingConfig.class, "An `IdMappingConfig` is used to define how multi column entity IDs are printed and parsed"))
				 .otherClass(APIConfig.class)
				 .otherClass(ConqueryConfig.class)
				 .otherClass(ClusterConfig.class)
				 .otherClass(CSVConfig.class)
				 .otherClass(FrontendConfig.class)
				 .otherClass(LocaleConfig.class)
				 .otherClass(PreprocessingConfig.class)
				 .otherClass(QueryConfig.class)
				 .otherClass(StandaloneConfig.class)
				 .otherClass(XodusStoreFactory.class)
				 .otherClass(MinaConfig.class)
				 .otherClass(FrontendConfig.CurrencyConfig.class)
				 .otherClass(XodusConfig.class)
				 .hide(Charset.class)
				 .hide(Currency.class)
				 .hide(InetAddress.class)
				 .hide(Locale.class)
				 .hide(Duration.class)
				 .hide(DataSize.class)
					.build(),
			Group.builder().name("REST API JSONs")
				 .resource(ConfigResource.class)
				 .resource(APIResource.class)
				 .resource(DatasetResource.class)
				 .resource(ConceptResource.class)
				 .resource(FilterResource.class)
				 .resource(QueryResource.class)
				 .resource(ResultCsvResource.class)
				 .base(new Base(QueryDescription.class, ""))
				 .base(new Base(CQElement.class, ""))
				 .base(new Base(FilterValue.class, ""))

				 .hide(Response.class)
				 .hide(ZonedDateTime.class)
				 .hide(Range.class)

				 .otherClass(FullExecutionStatus.class)
				 .otherClass(OverviewExecutionStatus.class)
				 .otherClass(IdLabel.class)
				 .otherClass(FrontendConfig.class)
				 .otherClass(FERoot.class)
				 .otherClass(FEValue.class)
				 .otherClass(FilterResource.FilterValues.class)
				 .otherClass(MetaDataPatch.class)
				 .otherClass(FrontendConfig.CurrencyConfig.class)
				 .otherClass(ConceptsProcessor.ResolvedFilterResult.class)
				 .otherClass(FilterResource.StringContainer.class)
				 .otherClass(ExecutionStatus.class)
				 .otherClass(ConceptsProcessor.ResolvedConceptsResult.class)
				 .otherClass(ConceptResource.ConceptCodeList.class)
				 .otherClass(CQTable.class)
				 .otherClass(ValidityDateContainer.class)
				 .otherClass(FormConfig.class)
				 .otherClass(FormConfigOverviewRepresentation.class)
				 .otherClass(FormConfigFullRepresentation.class)
					.build()
	};

	public static final String JSON_CREATOR = JsonCreator.class.getName();
	public static final String CPS_TYPE = CPSType.class.getName();
	public static final Set<String> ID_REF = Set.of(NsIdRef.class.getName(), MetaIdRef.class.getName());
	public static final Set<String> ID_REF_COL = Set.of(NsIdRefCollection.class.getName(), MetaIdRefCollection.class.getName());
	public static final String JSON_IGNORE = JsonIgnore.class.getName();
	public static final String JSON_BACK_REFERENCE = JsonBackReference.class.getName();
	public static final String PATH = Path.class.getName();
	public static final String LIST_OF = "list of ";
	public static final String ID_OF = "ID of ";
	public static final String GET = GET.class.getName();
	public static final String POST = POST.class.getName();
	public static final String PATCH = PATCH.class.getName();
	public static final String DELETE = DELETE.class.getName();
	public static final String PUT = PUT.class.getName();
	public static final String PATH_PARAM = PathParam.class.getName();
	public static final String AUTH = Auth.class.getName();
	public static final String CONTEXT = Context.class.getName();
	public static final Set<String> RESTS = Set.of(GET, POST, PUT, PATCH, DELETE);
}
