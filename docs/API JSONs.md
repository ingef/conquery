
# API JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


---

## Base IQuery


Different types of IQuery can be used by setting `type` to one of the following values:


### CONCEPT_QUERY
Java Name: `com.bakdata.conquery.models.query.concept.ConceptQuery`

The following fields are supported:

| Field | Type |
| --- | --- |
| root | [CQElement](#Base-CQElement) | 



---

## Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


### AND
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQAnd`

The following fields are supported:

| Field | Type |
| --- | --- |
| children | list of [CQElement](#Base-CQElement) | 

### BEFORE
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeTemporalQuery`

No fields can be set for this type.


### BEFORE_OR_SAME
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

No fields can be set for this type.


### CONCEPT
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQConcept`

The following fields are supported:

| Field | Type |
| --- | --- |
| excludeFromTimeAggregation | `boolean` | 
| ids | list of ID of `ConceptElement` | 
| label | `String` | 
| selects | list of ID of list of `Select` | 
| tables | list of `CQTable` | 

### DATE_RESTRICTION
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQDateRestriction`

The following fields are supported:

| Field | Type |
| --- | --- |
| child | [CQElement](#Base-CQElement) | 
| dateRange | `Range<LocalDate>` | 

### DAYS_BEFORE
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

No fields can be set for this type.


### DAYS_OR_NO_EVENT_BEFORE
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

No fields can be set for this type.


### EXTERNAL
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQExternal`

The following fields are supported:

| Field | Type |
| --- | --- |
| format | list of one of ID, EVENT_DATE, START_DATE, END_DATE, DATE_RANGE, DATE_SET, IGNORE | 
| values | list of `String` | 

### EXTERNAL_RESOLVED
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQExternalResolved`

The following fields are supported:

| Field | Type |
| --- | --- |
