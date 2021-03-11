
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

### GET config/frontend<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConfigResource.java#L20)</sup></sub></sup>


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

### POST datasets/{dataset}/concepts/{concept}/tables/{table}/filters/{filter}/autocomplete<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `autocompleteTextFilter`

Expects: [StringContainer](#Type-StringContainer)
Expects: `OptionalInt`
Expects: `OptionalInt`
Returns: list of [FEValue](#Type-FEValue)

</p></details>

### POST datasets/{dataset}/concepts/{concept}/tables/{table}/filters/{filter}/resolve<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L38)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `resolveFilterValues`

Expects: [FilterValues](#Type-FilterValues)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### POST datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L55)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `postQuery`

Expects: [@NotNull(message="{javax.validation.constraints.NotNull.message}", groups={}, payload={}) @Valid QueryDescription](#Base-QueryDescription)
Returns: `Response`

</p></details>

### DELETE datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L70)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `cancel`

Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### GET datasets/{dataset}/queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/QueryResource.java#L81)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.QueryResource`

Method: `getStatus`

Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### GET datasets/{dataset}/result/{query}.csv<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ResultCSVResource.java#L49)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ResultCSVResource`

Method: `getAsCsv`

Expects: `String`
Expects: `String`
Expects: `Optional<Boolean>`
Returns: `Response`

</p></details>

### GET datasets/{dataset}/stored-queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/StoredQueriesResource.java#L42)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.StoredQueriesResource`

Method: `getAllQueries`

Expects: ID of `Dataset`
Returns: list of [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### GET datasets/{dataset}/stored-queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/StoredQueriesResource.java#L48)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.StoredQueriesResource`

Method: `getSingleQueryInfo`

Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### PATCH datasets/{dataset}/stored-queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/StoredQueriesResource.java#L61)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.StoredQueriesResource`

Method: `patchQuery`

Expects: [MetaDataPatch](#Type-MetaDataPatch)
Returns: [FullExecutionStatus](#Type-FullExecutionStatus)

</p></details>

### DELETE datasets/{dataset}/stored-queries/{query}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/StoredQueriesResource.java#L70)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.StoredQueriesResource`

Method: `deleteQuery`

Returns: `void`

</p></details>

---

## Base QueryDescription


Different types of QueryDescription can be used by setting `type` to one of the following values:


### ABSOLUTE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L26)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.AbsoluteFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L33) | dateRange | `@NotNull @Valid Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L35) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L31) | query | `@NotNull @Valid IQuery` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/AbsoluteFormQuery.java#L37) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
</p></details>

### ARRAY_CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ArrayConceptQuery.java#L26-L30)</sup></sub></sup>
Query type that combines a set of {@link ConceptQuery}s which are separately evaluated and whose results are merged. If a SpecialDateUnion is required, the result will hold the union of all dates from the separate queries.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.ArrayConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ArrayConceptQuery.java#L38) | childQueries | list of [CONCEPT_QUERY](#CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ArrayConceptQuery.java#L41) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ArrayConceptQuery.java#L45) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
</p></details>

### CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ConceptQuery.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.ConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ConceptQuery.java#L36) | dateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ConceptQuery.java#L40) | resolvedDateAggregationMode | one of NONE, MERGE, INTERSECT, LOGICAL | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/ConceptQuery.java#L32) | root | [@Valid @NotNull CQElement](#Base-CQElement) | ? |  |  | 
</p></details>

### ENTITY_DATE_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/EntityDateQuery.java#L24-L28)</sup></sub></sup>
This query uses the date range defined by {@link EntityDateQuery#query} for each entity and applies it as a date restriction for the following query defined by {@link EntityDateQuery#features}. The additional {@link EntityDateQuery#dateRange} is applied globally on all entities.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.EntityDateQuery`

No fields can be set for this type.

</p></details>

### EXPORT_FORM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L42)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.forms.export_form.ExportForm`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L53) | alsoCreateCoarserSubdivisions | `boolean` | `true` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L45) | queryGroup | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L50) | resolution | list of one of COMPLETE, YEARS, QUARTERS, DAYS | `["COMPLETE"]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/forms/export_form/ExportForm.java#L47) | timeMode | `@NotNull @Valid Mode` | `null` |  |  | 
</p></details>

### RELATIVE_FORM_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L27)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.managed.RelativeFormQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L33) | features | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L39) | indexPlacement | one of BEFORE, NEUTRAL, AFTER | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L37) | indexSelector | one of EARLIEST, LATEST, RANDOM | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L35) | outcomes | [ARRAY_CONCEPT_QUERY](#ARRAY_CONCEPT_QUERY) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L31) | query | `@NotNull @Valid IQuery` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L47) | resolutionsAndAlignmentMap | list of `ResolutionAndAlignment` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L43) | timeCountAfter | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L41) | timeCountBefore | `@javax.validation.constraints.Min(0) int` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/managed/RelativeFormQuery.java#L45) | timeUnit | one of DAYS, QUARTERS, YEARS | ? |  |  | 
</p></details>

### SECONDARY_ID_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/SecondaryIdQuery.java#L38)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.SecondaryIdQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/SecondaryIdQuery.java#L43) | root | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/SecondaryIdQuery.java#L46) | secondaryId | ID of `@NsIdRef @NotNull SecondaryIdDescription` | `null` |  |  | 
</p></details>

### TABLE_EXPORT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/TableExportQuery.java#L41-L43)</sup></sub></sup>
A TABLE_EXPORT creates a full export of the given tables. It ignores selects completely.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.TableExportQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/TableExportQuery.java#L55) | dateRange | `@NotNull Range<LocalDate>` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/TableExportQuery.java#L51) | query | `@NonNull IQuery` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/TableExportQuery.java#L57) | tables | list of `CQUnfilteredTable` | ? |  |  | 
</p></details>



---

## Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


### AND<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQAnd.java#L33)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQAnd`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ␀ |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQAnd.java#L36) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQAnd.java#L42) | createExists | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQAnd.java#L45) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQBeforeTemporalQuery.java#L14-L16)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### BEFORE_OR_SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQBeforeOrSameTemporalQuery.java#L17-L19)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### CONCEPT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L64)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQConcept`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ␀ |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L87) | aggregateEventDates | `@javax.validation.constraints.NotNull boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L70-L72) | elements | list of ID of `ConceptElement<?>` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L85) | excludeFromSecondaryIdQuery | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L84) | excludeFromTimeAggregation | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L80) | selects | list of ID of `Select` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L77) | tables | list of [CQTable](#Type-CQTable) | `[]` |  |  | 
</p></details>

### DATE_RESTRICTION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQDateRestriction.java#L29)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQDateRestriction`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ␀ |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQDateRestriction.java#L35) | child | [@Valid @NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQDateRestriction.java#L33) | dateRange | `@NotNull Range<LocalDate>` | `null` |  |  | 
</p></details>

### DAYS_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQDaysBeforeTemporalQuery.java#L16-L18)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### DAYS_OR_NO_EVENT_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQDaysBeforeOrNeverTemporalQuery.java#L15-L17)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}, or no events. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### EXTERNAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQExternal.java#L37)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQExternal`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ? |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQExternal.java#L42) | format | list of one of ID, EVENT_DATE, START_DATE, END_DATE, DATE_RANGE, DATE_SET, IGNORE | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQExternal.java#L46) | values | list of `String` | ? |  |  | 
</p></details>

### NEGATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQNegation.java#L23)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQNegation`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ␀ |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQNegation.java#L28) | child | [@Valid @NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQNegation.java#L31) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### OR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQOr.java#L36)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQOr`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ␀ |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQOr.java#L40) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQOr.java#L46) | createExists | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQOr.java#L50) | dateAction | one of BLOCK, MERGE, INTERSECT, NEGATE | ␀ |  |  | 
</p></details>

### RESULT_INFO_DECORATOR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/ResultInfoDecorator.java#L25-L27)</sup></sub></sup>
A wrapper for {@link CQElement}s to provide additional infos to parts of a query.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.ResultInfoDecorator`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ␀ |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/ResultInfoDecorator.java#L38) | child | [@NotNull CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/ResultInfoDecorator.java#L36) | values | ClassToInstanceMap maps from base class `Object` to instances of subtypes |  |  |  | 
</p></details>

### SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQSameTemporalQuery.java#L14-L16)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} at the same time as the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQSameTemporalQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ? |  | Allows the user to define labels. | 
</p></details>

### SAVED_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQReusedQuery.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQReusedQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/CQElement.java#L27-L29) | label | `String` | ? |  | Allows the user to define labels. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQReusedQuery.java#L35) | query | ID of `ManagedExecution` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQReusedQuery.java#L37) | resolvedQuery | `IQuery` | ? |  |  | 
</p></details>



---

## Base FilterValue


Different types of FilterValue can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue$CQMultiSelectFilter.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `@Valid @NotNull @NsIdRef Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `@Valid @NotNull VALUE` | `null` |  |  | 
</p></details>

### INTEGER_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue$CQIntegerRangeFilter.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQIntegerRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `@Valid @NotNull @NsIdRef Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `@Valid @NotNull VALUE` | `null` |  |  | 
</p></details>

### MONEY_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue$CQMoneyRangeFilter.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMoneyRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `@Valid @NotNull @NsIdRef Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `@Valid @NotNull VALUE` | `null` |  |  | 
</p></details>

### MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue$CQMultiSelectFilter.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `@Valid @NotNull @NsIdRef Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `@Valid @NotNull VALUE` | `null` |  |  | 
</p></details>

### REAL_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue$CQRealRangeFilter.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQRealRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `@Valid @NotNull @NsIdRef Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `@Valid @NotNull VALUE` | `null` |  |  | 
</p></details>

### SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue$CQSelectFilter.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `@Valid @NotNull @NsIdRef Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `@Valid @NotNull VALUE` | `null` |  |  | 
</p></details>

### STRING<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue$CQStringFilter.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQStringFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `@Valid @NotNull @NsIdRef Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `@Valid @NotNull VALUE` | `null` |  |  | 
</p></details>



---

## Other Types

### Type CQTable<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L19)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.CQTable`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L37) | dateColumn | [ValidityDateContainer](#Type-ValidityDateContainer) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L22) | filters | list of [FilterValue<?>](#Base-FilterValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L33) | id | ID of `Connector` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L26) | selects | list of ID of `Select` | `[]` |  |  | 
</p></details>

### Type ConceptCodeList<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource$ConceptCodeList.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource$ConceptCodeList`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource$ConceptCodeList.java) | concepts | list of `String` | `null` |  |  | 
</p></details>

### Type CurrencyConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig$CurrencyConfig.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig$CurrencyConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig$CurrencyConfig.java) | decimalScale | `int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig$CurrencyConfig.java) | decimalSeparator | `String` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig$CurrencyConfig.java) | prefix | `String` | `"€"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig$CurrencyConfig.java) | thousandSeparator | `String` | `"."` |  |  | 
</p></details>

### Type ExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L15)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.execution.ExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L25) | createdAt | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L33) | id | ID of `ManagedExecution` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L22) | label | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L26) | lastUsed | `ZonedDateTime` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L35) | numberOfResults | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L30) | own | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L27) | owner | ID of `User` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L28) | ownerName | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L38) | queryType | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L36) | requiredTime | `long` or `null` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L42-L44) | resultUrl | `URL` | ? |  | The url under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L39) | secondaryId | ID of `SecondaryIdDescription` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L29) | shared | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L34) | status | one of NEW, RUNNING, CANCELED, FAILED, DONE | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L31) | system | `boolean` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L21) | tags | list of `String` | ? |  |  | 
</p></details>

### Type FERoot<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/api/description/FERoot.java#L11-L13)</sup></sub></sup>
This class represents the root node of the concepts as it is presented to the front end.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.api.description.FERoot`

No fields can be set for this type.

</p></details>

### Type FEValue<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/api/description/FEValue.java#L8-L10)</sup></sub></sup>
This class represents a values of a SELECT filter.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.api.description.FEValue`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/api/description/FEValue.java#L17) | optionValue | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/api/description/FEValue.java#L16) | templateValues | map from `String` to `String` | ? |  |  | 
</p></details>

### Type FilterValues<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource$FilterValues.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$FilterValues`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource$FilterValues.java) | values | list of `String` | `null` |  |  | 
</p></details>

### Type FormConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L44)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L70) | creationTime | `LocalDateTime` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L53) | dataset | ID of `Dataset` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L56) | formId | `@NonNull UUID` | generated default varies |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L54) | formType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L58) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L69) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L61) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L59) | tags | list of `String` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig.java#L63-L66) | values | `@NotNull JsonNode` | `null` |  | This is a blackbox for us at the moment, where the front end saves the state of the formular, when the user saved it. | 
</p></details>

### Type FormConfigFullRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig$FormConfigFullRepresentation.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigFullRepresentation`

No fields can be set for this type.

</p></details>

### Type FormConfigOverviewRepresentation<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/forms/configs/FormConfig$FormConfigOverviewRepresentation.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.forms.configs.FormConfig$FormConfigOverviewRepresentation`

No fields can be set for this type.

</p></details>

### Type FrontendConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L13)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L17) | currency | [CurrencyConfig](#Type-CurrencyConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L16) | version | `String` | `"0.0.0-SNAPSHOT"` |  |  | 
</p></details>

### Type FullExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/FullExecutionStatus.java#L18-L22)</sup></sub></sup>
This status holds extensive information about the query description and meta data that is computational heavy and can produce a larger payload to requests. It should only be rendered, when a client asks for a specific execution, not if a list of executions is requested.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.execution.FullExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L25) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L33) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L22) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L26) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L35) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L30) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L27) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L28) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L38) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L36) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L42-L44) | resultUrl | `URL` | `null` |  | The url under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L39) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L29) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L34) | status | one of NEW, RUNNING, CANCELED, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L31) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L21) | tags | list of `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/FullExecutionStatus.java#L54-L56) | availableSecondaryIds | list of ID of `@NsIdRefCollection Set<SecondaryIdDescription>` | `null` |  | Possible {@link SecondaryIdDescription}s available, of {@link com.bakdata.conquery.models.concepts.Concept}s used in this Query. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/FullExecutionStatus.java#L34-L36) | canExpand | `boolean` | `false` |  | Indicates if the concepts that are included in the query description can be accessed by the user. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/FullExecutionStatus.java#L29-L31) | columnDescriptions | list of `ColumnDescriptor` | `null` |  | Holds a description for each column, present in the result. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/FullExecutionStatus.java#L44-L46) | error | `ConqueryErrorInfo` | `null` |  | Is set when the QueryFailed | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/FullExecutionStatus.java#L49-L51) | groups | `Collection<GroupId>` | `null` |  | The groups this execution is shared with. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/FullExecutionStatus.java#L39-L41) | query | [QueryDescription](#Base-QueryDescription) | `null` |  | Is set to the query description if the user can expand all included concepts. | 
</p></details>

### Type IdLabel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/IdLabel.java#L13-L16)</sup></sub></sup>
Container class for the frontend to provide a tuple of id and a corresponding label.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.IdLabel`

No fields can be set for this type.

</p></details>

### Type MetaDataPatch<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L28)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.MetaDataPatch`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L36) | groups | list of ID of `Group` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L35) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/apiv1/MetaDataPatch.java#L34) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type OverviewExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/OverviewExecutionStatus.java#L5-L7)</sup></sub></sup>
Light weight description of an execution. Rendering the overview should not cause heavy computations.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.execution.OverviewExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L25) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L33) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L22) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L26) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L35) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L30) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L27) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L28) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L38) | queryType | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L36) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L42-L44) | resultUrl | `URL` | `null` |  | The url under from which the result of the execution can be downloaded as soon as it finished successfully. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L39) | secondaryId | ID of `SecondaryIdDescription` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L29) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L34) | status | one of NEW, RUNNING, CANCELED, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L31) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L21) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type ResolvedConceptsResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedConceptsResult.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedConceptsResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedConceptsResult.java) | resolvedConcepts | list of ID of `ConceptElement` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedConceptsResult.java) | resolvedFilter | [ResolvedFilterResult](#Type-ResolvedFilterResult) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedConceptsResult.java) | unknownCodes | list of `String` | ? |  |  | 
</p></details>

### Type ResolvedFilterResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedFilterResult.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedFilterResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedFilterResult.java) | filterId | ID of `Filter` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedFilterResult.java) | tableId | ID of `Connector` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor$ResolvedFilterResult.java) | value | list of [FEValue](#Type-FEValue) | ? |  |  | 
</p></details>

### Type StringContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource$StringContainer.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$StringContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource$StringContainer.java) | text | `String` | `null` |  |  | 
</p></details>

### Type ValidityDateContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/ValidityDateContainer.java#L10)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.ValidityDateContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/ValidityDateContainer.java#L13) | value | ID of `ValidityDate` | ? |  |  | 
</p></details>
