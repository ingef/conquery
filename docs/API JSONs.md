
# API JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


---

## Base IQuery


Different types of IQuery can be used by setting `type` to one of the following values:


### CONCEPT_QUERY
Java Type: `com.bakdata.conquery.models.query.concept.ConceptQuery`

The following fields are supported:

| Field | Type |
| --- | --- |
| root | [CQElement](#Base-CQElement) | 



---

## Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


### AND
Java Type: `com.bakdata.conquery.models.query.concept.specific.CQAnd`

The following fields are supported:

| Field | Type |
| --- | --- |
| children | list of [CQElement](#Base-CQElement) | 

### BEFORE
Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeTemporalQuery`

No fields can be set for this type.


### BEFORE_OR_SAME
Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

No fields can be set for this type.


### CONCEPT
Java Type: `com.bakdata.conquery.models.query.concept.specific.CQConcept`

The following fields are supported:

| Field | Type |
| --- | --- |
| excludeFromTimeAggregation | `boolean` | 
| ids | list of ID of `ConceptElement` | 
| label | `String` | 
| selects | list of ID of list of `Select` | 
| tables | list of `CQTable` | 

### DATE_RESTRICTION
Java Type: `com.bakdata.conquery.models.query.concept.specific.CQDateRestriction`

The following fields are supported:

| Field | Type |
| --- | --- |
| child | [CQElement](#Base-CQElement) | 
| dateRange | `Range<LocalDate>` | 

### DAYS_BEFORE
Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

No fields can be set for this type.


### DAYS_OR_NO_EVENT_BEFORE
Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

No fields can be set for this type.


### EXTERNAL
Java Type: `com.bakdata.conquery.models.query.concept.specific.CQExternal`

The following fields are supported:

| Field | Type |
| --- | --- |
| format | list of one of ID, EVENT_DATE, START_DATE, END_DATE, DATE_RANGE, DATE_SET, IGNORE | 
| values | list of `String` | 

### NEGATION
Java Type: `com.bakdata.conquery.models.query.concept.specific.CQNegation`

The following fields are supported:

| Field | Type |
| --- | --- |
| child | [CQElement](#Base-CQElement) | 

### OR
Java Type: `com.bakdata.conquery.models.query.concept.specific.CQOr`

The following fields are supported:

| Field | Type |
| --- | --- |
| children | list of [CQElement](#Base-CQElement) | 

### SAME
Java Type: `com.bakdata.conquery.models.query.concept.specific.temporal.CQSameTemporalQuery`

No fields can be set for this type.


### SAVED_QUERY
Java Type: `com.bakdata.conquery.models.query.concept.specific.CQReusedQuery`

The following fields are supported:

| Field | Type |
| --- | --- |
| query | ID of `ManagedExecution` | 
| resolvedQuery | [IQuery](#Base-IQuery) | 



---

## Base FilterValue


Different types of FilterValue can be used by setting `type` to one of the following values:


### BIG_MULTI_SELECT
Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | ID of `Filter<?>` | 
| value | `VALUE` | 

### INTEGER_RANGE
Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQIntegerRangeFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | ID of `Filter<?>` | 
| value | `VALUE` | 

### MONEY_RANGE
Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQIntegerRangeFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | ID of `Filter<?>` | 
| value | `VALUE` | 

### MULTI_SELECT
Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | ID of `Filter<?>` | 
| value | `VALUE` | 

### REAL_RANGE
Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQRealRangeFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | ID of `Filter<?>` | 
| value | `VALUE` | 

### SELECT
Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | ID of `Filter<?>` | 
| value | `VALUE` | 

### STRING
Java Type: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQStringFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | ID of `Filter<?>` | 
| value | `VALUE` | 



---

## Other Types
