
# REST API JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


# REST endpoints

### GET /<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/APIResource.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.APIResource`

Method: `getDatasets`

Returns: list of [IdLabel](#Type-IdLabel)

</p></details>

### GET config<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/ConfigResource.java#L20)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.ConfigResource`

Method: `getFrontendConfig`

Returns: [FrontendConfig](#Type-FrontendConfig)

</p></details>

### GET datasets/{datasetName}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/DatasetResource.java#L26)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.DatasetResource`

Method: `getRoot`

Returns: [FERoot](#Type-FERoot)

</p></details>

### GET datasets/{datasetName}/concepts/{conceptName}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L41)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource`

Method: `getNode`

Returns: `Response`

</p></details>

### POST datasets/{datasetName}/concepts/{conceptName}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L59)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource`

Method: `resolve`

Expects: [ConceptCodeList](#Type-ConceptCodeList)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### POST datasets/{datasetName}/concepts/{conceptName}/tables/{tableName}/filters/{filterName}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L41)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `resolveFilterValues`

Expects: [FilterValues](#Type-FilterValues)
Returns: [ResolvedConceptsResult](#Type-ResolvedConceptsResult)

</p></details>

### POST datasets/{datasetName}/concepts/{conceptName}/tables/{tableName}/filters/{filterName}<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L47)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource`

Method: `autocompleteTextFilter`

Expects: [StringContainer](#Type-StringContainer)
Returns: list of [FEValue](#Type-FEValue)

</p></details>

### POST datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/QueryResource.java#L50)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.QueryResource`

Method: `postQuery`

Expects: [IQuery](#Base-IQuery)
Returns: [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### DELETE datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/QueryResource.java#L68)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.QueryResource`

Method: `cancel`

Returns: [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### GET datasets/{dataset}/queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/QueryResource.java#L80)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.QueryResource`

Method: `getStatus`

Returns: [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### GET datasets/{dataset}/result/<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/ResultCSVResource.java#L55)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.ResultCSVResource`

Method: `getAsCsv`

Expects: `String`
Returns: `Response`

</p></details>

### GET datasets/{dataset}/stored-queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L53)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.StoredQueriesResource`

Method: `getAllQueries`

Returns: list of [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### GET datasets/{dataset}/stored-queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L61)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.StoredQueriesResource`

Method: `getQueryWithSource`

Returns: [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### PATCH datasets/{dataset}/stored-queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L75)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.StoredQueriesResource`

Method: `patchQuery`

Expects: [QueryPatch](#Type-QueryPatch)
Returns: [ExecutionStatus](#Type-ExecutionStatus)

</p></details>

### DELETE datasets/{dataset}/stored-queries<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L110)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.StoredQueriesResource`

Method: `deleteQuery`

Returns: `void`

</p></details>

---

## Base IQuery


Different types of IQuery can be used by setting `type` to one of the following values:


### ARRAY_CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/ArrayConceptQuery.java#L20-L24)</sup></sub></sup>
Query type that combines a set of {@link ConceptQuery}s which are separately evaluated and whose results are merged. If a SpecialDateUnion is required, the result will hold the union of all dates from the separate queries.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.ArrayConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/ArrayConceptQuery.java#L29) | childQueries | list of [CONCEPT_QUERY](#CONCEPT_QUERY) | `[]` |  |  | 
</p></details>

### CONCEPT_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/ConceptQuery.java#L23)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.ConceptQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/ConceptQuery.java#L29) | root | [CQElement](#Base-CQElement) | ? |  |  | 
</p></details>



---

## Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


### AND<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQAnd.java#L24)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQAnd`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQAnd.java#L26) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
</p></details>

### BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQBeforeTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeTemporalQuery`

No fields can be set for this type.

</p></details>

### BEFORE_OR_SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQBeforeOrSameTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

No fields can be set for this type.

</p></details>

### CONCEPT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L47)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQConcept`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L66) | excludeFromTimeAggregation | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L57) | ids | list of ID of `ConceptElement` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L55) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L62) | selects | list of ID of `Select` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQConcept.java#L59) | tables | list of [CQTable](#Type-CQTable) | `null` |  |  | 
</p></details>

### DATE_RESTRICTION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQDateRestriction.java#L29)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQDateRestriction`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQDateRestriction.java#L35) | child | [CQElement](#Base-CQElement) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQDateRestriction.java#L33) | dateRange | `Range<LocalDate>` | `null` |  |  | 
</p></details>

### DAYS_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQDaysBeforeTemporalQuery.java#L15-L17)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

No fields can be set for this type.

</p></details>

### DAYS_OR_NO_EVENT_BEFORE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQDaysBeforeOrNeverTemporalQuery.java#L14-L16)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}, or no events. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

No fields can be set for this type.

</p></details>

### EXTERNAL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQExternal.java#L39)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQExternal`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQExternal.java#L44) | format | list of one of ID, EVENT_DATE, START_DATE, END_DATE, DATE_RANGE, DATE_SET, IGNORE | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQExternal.java#L48) | values | list of `String` | ? |  |  | 
</p></details>

### NEGATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQNegation.java#L20)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQNegation`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQNegation.java#L25) | child | [CQElement](#Base-CQElement) | `null` |  |  | 
</p></details>

### OR<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQOr.java#L27)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQOr`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQOr.java#L30) | children | list of [CQElement](#Base-CQElement) | `null` |  |  | 
</p></details>

### SAME<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/temporal/CQSameTemporalQuery.java#L12-L14)</sup></sub></sup>
Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} at the same time as the events of {@code index}. And the time where this has happened.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQSameTemporalQuery`

No fields can be set for this type.

</p></details>

### SAVED_QUERY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQReusedQuery.java#L31)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.specific.CQReusedQuery`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQReusedQuery.java#L35) | query | ID of `ManagedExecution` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/specific/CQReusedQuery.java#L37) | resolvedQuery | [IQuery](#Base-IQuery) | ? |  |  | 
</p></details>



---

## Base FilterValue


Different types of FilterValue can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L42)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `VALUE` | `null` |  |  | 
</p></details>

### INTEGER_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L67)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQIntegerRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `VALUE` | `null` |  |  | 
</p></details>

### MONEY_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L67)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQIntegerRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `VALUE` | `null` |  |  | 
</p></details>

### MULTI_SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L42)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `VALUE` | `null` |  |  | 
</p></details>

### REAL_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L76)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQRealRangeFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `VALUE` | `null` |  |  | 
</p></details>

### SELECT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L51)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQSelectFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `VALUE` | `null` |  |  | 
</p></details>

### STRING<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L59)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQStringFilter`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L31) | filter | ID of `Filter<?>` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/FilterValue.java#L36) | value | `VALUE` | `null` |  |  | 
</p></details>



---

## Other Types

### Type CQTable<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L24)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.CQTable`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L28) | dateColumn | [ValidityDateColumn](#Type-ValidityDateColumn) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L30) | filters | list of [FilterValue<?>](#Base-FilterValue) | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L26) | id | ID of `Connector` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L33) | selects | list of ID of `Select` | `[]` |  |  | 
</p></details>

### Type ConceptCodeList<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L72)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptResource$ConceptCodeList`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptResource.java#L75) | concepts | list of `String` | `null` |  |  | 
</p></details>

### Type CurrencyConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L20)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig$CurrencyConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L26) | decimalScale | `int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L25) | decimalSeparator | `String` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L23) | prefix | `String` | `"€"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L24) | thousandSeparator | `String` | `"."` |  |  | 
</p></details>

### Type ExecutionStatus<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L16)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.execution.ExecutionStatus`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L26) | createdAt | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L35) | id | ID of `ManagedExecution` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L25) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L27) | lastUsed | `ZonedDateTime` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L37) | message | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L38) | numberOfResults | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L31) | own | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L28) | owner | ID of `User` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L29) | ownerName | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L33) | query | [IQuery](#Base-IQuery) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L39) | requiredTime | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L40) | resultUrl | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L30) | shared | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L36) | status | one of NEW, RUNNING, CANCELED, FAILED, DONE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L32) | system | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/execution/ExecutionStatus.java#L24) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type FERoot<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/api/description/FERoot.java#L10-L12)</sup></sub></sup>
This class represents the root node of the concepts as it is presented to the front end.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.api.description.FERoot`

No fields can be set for this type.

</p></details>

### Type FEValue<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/api/description/FEValue.java#L8-L10)</sup></sub></sup>
This class represents a values of a SELECT filter.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.api.description.FEValue`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/api/description/FEValue.java#L17) | optionValue | `String` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/api/description/FEValue.java#L16) | templateValues | map from `String` to `String` | ? |  |  | 
</p></details>

### Type FilterValues<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L61)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$FilterValues`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L63) | values | list of `String` | `null` |  |  | 
</p></details>

### Type FrontendConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L13)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L17) | currency | [CurrencyConfig](#Type-CurrencyConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L16) | version | `String` | `"0.0.0-SNAPSHOT"` |  |  | 
</p></details>

### Type IdLabel<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/IdLabel.java#L9)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.IdLabel`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/IdLabel.java#L13) | id | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/IdLabel.java#L11) | label | `String` | `null` |  |  | 
</p></details>

### Type QueryPatch<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L101)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.apiv1.StoredQueriesResource$QueryPatch`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L107) | groups | list of ID of `Group` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L105) | label | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L106) | shared | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/apiv1/StoredQueriesResource.java#L104) | tags | list of `String` | `null` |  |  | 
</p></details>

### Type ResolvedConceptsResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L219)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedConceptsResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L224) | resolvedConcepts | list of ID of `ConceptElement` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L225) | resolvedFilter | [ResolvedFilterResult](#Type-ResolvedFilterResult) | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L226) | unknownCodes | list of `String` | ? |  |  | 
</p></details>

### Type ResolvedFilterResult<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L199)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.ConceptsProcessor$ResolvedFilterResult`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L205) | filterId | ID of `Filter` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L204) | tableId | ID of `Connector` | ? |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/ConceptsProcessor.java#L206) | value | list of [FEValue](#Type-FEValue) | ? |  |  | 
</p></details>

### Type StringContainer<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L66)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.resources.api.FilterResource$StringContainer`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/resources/api/FilterResource.java#L68) | text | `String` | `null` |  |  | 
</p></details>

### Type ValidityDateColumn<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L42)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.query.concept.filter.CQTable$ValidityDateColumn`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/s/backend/src/main/java/com/bakdata/conquery/models/query/concept/filter/CQTable.java#L44) | value | ID of `ValidityDate` | `null` |  |  | 
</p></details>
