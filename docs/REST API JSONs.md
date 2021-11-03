
# REST API JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


# REST endpoints

### GET /datasets<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/APIResource.java#L26)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.APIResource`

Method: `getDatasets`

Returns: list of [IdLabel](#Type-IdLabel)

</p></details>

### GET config/frontend<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConfigResource.java#L23)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConfigResource`

Method: `getFrontendConfig`

Returns: [FrontendConfig](#Type-FrontendConfig)

</p></details>

### GET datasets/{dataset}/concepts<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/DatasetResource.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.DatasetResource`

Method: `getRoot`

Returns: [FERoot](#Type-FERoot)

</p></details>

### GET datasets/{dataset}/concepts/{concept}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L40)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource`

Method: `getNode`

Returns: `Response`

</p></details>

### POST datasets/{dataset}/concepts/{concept}/resolve<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L53)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource`

Method: `resolve`

Expects: [ConceptCodeList](#Type-ConceptCodeList)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### POST datasets/{dataset}/concepts/{concept}/tables/{table}/filters/{filter}/autocomplete<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L46)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `autocompleteTextFilter`

Expects: [StringContainer](#Type-StringContainer)
Expects: `OptionalInt`
Expects: `OptionalInt`
Returns: list of [FEValue](#Type-FEValue)

</p></details>

### POST datasets/{dataset}/concepts/{concept}/tables/{table}/filters/{filter}/resolve<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L40)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `resolveFilterValues`

Expects: [FilterValues](#Type-FilterValues)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### GET datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L61)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getAllQueries`

Expects: `Optional<Boolean>`
Returns: list of [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### POST datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L70)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `postQuery`

Expects: `Optional<Boolean>`
Expects: [@NotNull(message="{javax.validation.constraints.NotNull.message}", groups={}, payload={}) @Valid QueryDescription](#Base-QueryDescription)
Returns: `Response`

</p></details>

### POST datasets/{dataset}/queries/upload<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L136)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `upload`

Expects: `@Valid ExternalUpload`
Returns: `ExternalUploadResult`

</p></details>

### GET datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L82)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getStatus`

Expects: `Optional<Boolean>`
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### PATCH datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L95)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `patchQuery`

Expects: `Optional<Boolean>`
Expects: [MetaDataPatch](#Type-MetaDataPatch)
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### DELETE datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L107)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `deleteQuery`

Returns: `void`

</p></details>

### POST datasets/{dataset}/queries/{query}/cancel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L126)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `cancel`

Returns: `void`

</p></details>

### POST datasets/{dataset}/queries/{query}/reexecute<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L116)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `reexecute`

Expects: `Optional<Boolean>`
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### GET datasets/{dataset}/result/{query}.csv<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ResultCsvResource.java#L52)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ResultCsvResource`

Method: `getAsCsv`

Expects: `String`
Expects: `String`
Expects: `Optional<Boolean>`
Returns: `Response`

</p></details>

---

## Base QueryDescription


Different types of QueryDescription can be used by setting `type` to one of the following values:


### ABSOLUTE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L38) | dateRange | `@NotNull @Valid Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L40) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L36) | query | `@NotNull @Valid Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L42) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
</p></details>

### ARRAY_CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L31-L35)</sup></sub></sup>
Query type that combines a set of {@link ConceptQuery}s which are separately evaluated and whose results are merged. If a SpecialDateUnion is required, the result will hold the union of all dates from the separate queries.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.ArrayConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L43) | childQueries | list of [CONCEPT_QUERY](#CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L46) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ArrayConceptQuery.java#L50) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
</p></details>

### CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L29)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.ConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L40) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L44) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/ConceptQuery.java#L36) | root | [@Valid @NotNull CQElement](#Base-CQElement) | ? |  |  | 
</p></details>

### ENTITY_DATE_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/EntityDateQuery.java#L30-L34)</sup></sub></sup>
This query uses the date range defined by {@link EntityDateQuery#query} for each entity and applies it as a date restriction for the following query defined by {@link EntityDateQuery#features}. The additional {@link EntityDateQuery#dateRange} is applied globally on all entities.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.EntityDateQuery`

No fields can be set for this type.

</p></details>

### EXPORT_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L45)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.export_form.ExportForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L62) | alsoCreateCoarserSubdivisions | `boolean` | `true` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L49) | queryGroupId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L59) | resolution | list of one of COMPLETE, YEARS, QUARTERS, DAYS | `["COMPLETE"]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L56) | timeMode | `@NotNull @Valid Mode` | `null` |  |  | 
</p></details>

### FULL_EXPORT_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.export_form.FullExportForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L56) | dateRange | `@Valid Range<LocalDate>` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L49) | queryGroupId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/FullExportForm.java#L60) | tables | list of [CQElement](#Base-CQElement) | `[]` |  |  | 
</p></details>

### RELATIVE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.RelativeFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L37) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L43) | indexPlacement | one of BEFORE, NEUTRAL, AFTER | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L41) | indexSelector | one of EARLIEST, LATEST, RANDOM | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L39) | outcomes | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L35) | query | `@NotNull @Valid Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L51) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L47) | timeCountAfter | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L45) | timeCountBefore | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L49) | timeUnit | one of DAYS, QUARTERS, YEARS | ? |  |  | 
</p></details>

### SECONDARY_ID_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L38)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.SecondaryIdQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L51) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | `"MERGE"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L55-L57) | query | [CONCEPT_QUERY](#CONCEPT_QUERY) | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L44) | root | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L47) | secondaryId | ID of `@NsIdRef @NotNull SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L61) | withSecondaryId | list of ID of `@NsIdRefCollection Set<Column>` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/SecondaryIdQuery.java#L65) | withoutSecondaryId | list of ID of `@NsIdRefCollection Set<Table>` | ␀ |  |  | 
</p></details>

### TABLE_EXPORT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L49-L51)</sup></sub></sup>
A TABLE_EXPORT creates a full export of the given tables. It ignores selects completely.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.TableExportQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L63) | dateRange | `@NotNull Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L69) | positions | map from `Column` to `int` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L59) | query | `@NonNull Query` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/TableExportQuery.java#L65) | tables | list of `CQUnfilteredTable` | ? |  |  | 
</p></details>



---

## Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


### AND<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L37)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQAnd`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L40) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L46) | createExists | `Optional<Boolean>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQAnd.java#L49) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQBeforeTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### BEFORE_OR_SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQBeforeOrSameTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### CONCEPT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L47)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQConcept`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L75) | aggregateEventDates | `@javax.validation.constraints.NotNull boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L54-L56) | elements | list of ID of `ConceptElement<?>` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L73) | excludeFromSecondaryIdQuery | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L72) | excludeFromTimeAggregation | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L68) | selects | list of ID of `Select` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQConcept.java#L62) | tables | list of [CQTable](#Type-CQTable) | `[]` |  |  | 
</p></details>

### DATE_RESTRICTION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQDateRestriction.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQDateRestriction`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQDateRestriction.java#L37) | child | [@Valid @NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQDateRestriction.java#L35) | dateRange | `@NotNull Range<LocalDate>` | `null` |  |  | 
</p></details>

### DAYS_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQDaysBeforeTemporalQuery.java#L14-L16)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### DAYS_OR_NO_EVENT_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQDaysBeforeOrNeverTemporalQuery.java#L13-L15)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}, or no events. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### EXTERNAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/external/CQExternal.java#L40-L42)</sup></sub></sup>
Allows uploading lists of entities.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### NEGATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L24)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQNegation`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L29) | child | [@Valid @NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQNegation.java#L32) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### OR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L39)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQOr`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L43) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L49) | createExists | `Optional<Boolean>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQOr.java#L53) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### RESULT_INFO_DECORATOR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L27-L29)</sup></sub></sup>
A wrapper for {@link CQElement}s to provide additional infos to parts of a query.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.ResultInfoDecorator`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L40) | child | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/ResultInfoDecorator.java#L38) | values | ClassToInstanceMap maps from base class `Object` to instances of subtypes |  |  |  | 
</p></details>

### SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/temporal/CQSameTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} at the same time as the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### SAVED_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L30)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/CQElement.java#L32-L34) | label | `String` | `null` |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L53) | excludeFromSecondaryId | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L39-L41) | queryId | ID of `ManagedExecution` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/specific/CQReusedQuery.java#L50) | resolvedQuery | `Query` | ␀ |  |  | 
</p></details>



---

## Base FilterValue


Different types of FilterValue can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L52)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQBigMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### INTEGER_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L76)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQIntegerRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### MONEY_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L84-L87)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQMoneyRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### REAL_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L95)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQRealRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L60)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>

### STRING<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L68)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.FilterValue$CQStringFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L30) | filter | ID of `@NotNull @NsIdRef Filter<VALUE>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/FilterValue.java#L35) | value | `@NotNull VALUE` | `null` |  |  | 
</p></details>



---

## Other Types

### Type CQTable<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L21)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.CQTable`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L35) | connector | ID of `@NsIdRef Connector` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L39) | dateColumn | [ValidityDateContainer](#Type-ValidityDateContainer) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L24) | filters | list of [FilterValue<?>](#Base-FilterValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/CQTable.java#L28) | selects | list of ID of `Select` | `[]` |  |  | 
</p></details>

### Type ConceptCodeList<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L64)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource$ConceptCodeList`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L68) | concepts | list of `String` | `null` |  |  | 
</p></details>

### Type CurrencyConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L202)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig$CurrencyConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L207) | decimalScale | `int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L206) | decimalSeparator | `String` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L204) | prefix | `String` | `"€"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L205) | thousandSeparator | `String` | `"."` |  |  | 
</p></details>

### Type ExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L19)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.ExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | createdAt | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L43) | finishTime | `LocalDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | id | ID of `ManagedExecution` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L26) | label | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | lastUsed | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | numberOfResults | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | own | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | owner | ID of `User` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | ownerName | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45) | queryType | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L40) | requiredTime | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L49-L51) | resultUrls | list of `URL` | ? |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L46) | secondaryId | ID of `SecondaryIdDescription` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | shared | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | startTime | `LocalDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | status | one of NEW, RUNNING, FAILED, DONE | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L35) | system | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | tags | list of `String` | ? |  |  | 
</p></details>

### Type FERoot<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FERoot.java#L11-L13)</sup></sub></sup>
This class represents the root node of the concepts as it is presented to the front end.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.frontend.FERoot`

No fields can be set for this type.

</p></details>

### Type FEValue<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L10-L12)</sup></sub></sup>
This class represents a values of a SELECT filter.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.frontend.FEValue`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L26) | optionValue | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/frontend/FEValue.java#L24) | templateValues | map from `String` to `String` | ? |  |  | 
</p></details>

### Type FilterValues<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L62)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$FilterValues`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L64) | values | list of `String` | `null` |  |  | 
</p></details>

### Type FormConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L49)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L77) | creationTime | `LocalDateTime` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L58) | dataset | ID of `@NsIdRef Dataset` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L62) | formId | `@NonNull UUID` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L60) | formType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L64) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L75) | owner | ID of `@MetaIdRef User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L67) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L65) | tags | list of `String` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L69-L72) | values | `@NotNull JsonNode` | `null` |  | This is a blackbox for us at the moment, where the front end saves the state of the formular, when the user saved it. | 
</p></details>

### Type FormConfigFullRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L174-L177)</sup></sub></sup>
API representation for a single {@link FormConfig} which includes the form fields an their values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigFullRepresentation`

No fields can be set for this type.

</p></details>

### Type FormConfigOverviewRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L150-L153)</sup></sub></sup>
API representation for the overview of all {@link FormConfig}s which does not include the form fields an their values.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigOverviewRepresentation`

No fields can be set for this type.

</p></details>

### Type FrontendConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L54) | currency | [CurrencyConfig](#Type-CurrencyConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L58) | queryUpload | `UploadConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L53) | version | `String` | `"0.0.0-SNAPSHOT"` |  |  | 
</p></details>

### Type FullExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L19-L23)</sup></sub></sup>
This status holds extensive information about the query description and meta data that is computational heavy and can produce a larger payload to requests. It should only be rendered, when a client asks for a specific execution, not if a list of executions is requested.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.FullExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L43) | finishTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L26) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L40) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L49-L51) | resultUrls | list of `URL` | `[]` |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L46) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | startTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | status | one of NEW, RUNNING, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L35) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | tags | list of `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L63-L65) | availableSecondaryIds | list of ID of `@NsIdRefCollection Set<SecondaryIdDescription>` | `null` |  | Possible {@link SecondaryIdDescription}s available, of {@link com.bakdata.conquery.models.datasets.concepts.Concept}s used in this Query. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L43-L45) | canExpand | `boolean` | `false` |  | Indicates if the concepts that are included in the query description can be accessed by the user. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L38-L40) | columnDescriptions | list of `ColumnDescriptor` | `null` |  | Holds a description for each column, present in the result. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L53-L55) | error | `ConqueryErrorInfo` | `null` |  | Is set when the QueryFailed | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L58-L60) | groups | `Collection<GroupId>` | `null` |  | The groups this execution is shared with. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L30-L34) | progress | `float` or `null` | `null` |  | The estimated progress of an running execution in the enclosing interval [0-1]. This value does not have to be set if the state is RUNNING and it must not be set if the state is not RUNNING. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/FullExecutionStatus.java#L48-L50) | query | [QueryDescription](#Base-QueryDescription) | `null` |  | Is set to the query description if the user can expand all included concepts. | 
</p></details>

### Type IdLabel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/IdLabel.java#L13-L16)</sup></sub></sup>
Container class for the frontend to provide a tuple of id and a corresponding label.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.IdLabel`

No fields can be set for this type.

</p></details>

### Type MetaDataPatch<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L29)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.MetaDataPatch`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L37) | groups | list of ID of `Group` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L36) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L35) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type OverviewExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/OverviewExecutionStatus.java#L5-L7)</sup></sub></sup>
Light weight description of an execution. Rendering the overview should not cause heavy computations.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.OverviewExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L29) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L43) | finishTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L37) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L26) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L30) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L39) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L34) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L31) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L32) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L45) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L40) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L49-L51) | resultUrls | list of `URL` | `[]` |  | The urls under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L46) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L33) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L42) | startTime | `LocalDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L38) | status | one of NEW, RUNNING, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L35) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/ExecutionStatus.java#L25) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type ResolvedConceptsResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L274)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedConceptsResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L279) | resolvedConcepts | list of ID of `ConceptElement` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L280) | resolvedFilter | [ResolvedFilterResult](#Type-ResolvedFilterResult) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L281) | unknownCodes | list of `String` | ? |  |  | 
</p></details>

### Type ResolvedFilterResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L264)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedFilterResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L270) | filterId | ID of `Filter` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L269) | tableId | ID of `Connector` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L271) | value | list of [FEValue](#Type-FEValue) | ? |  |  | 
</p></details>

### Type StringContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L67)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$StringContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L69) | text | `String` | `null` |  |  | 
</p></details>

### Type ValidityDateContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/ValidityDateContainer.java#L9)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.query.concept.filter.ValidityDateContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/query/concept/filter/ValidityDateContainer.java#L12) | value | ID of `@NsIdRef ValidityDate` | ? |  |  | 
</p></details>
