
# API JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


### Base IQuery


Different types of IQuery can be used by setting `type` to one of the following values:


##### CONCEPT_QUERY
Java Name: `com.bakdata.conquery.models.query.concept.ConceptQuery`

The following fields are supported:

| Field | Type |
| --- | --- |
| root | [CQElement](#Base-CQElement) | 



### Base CQElement


Different types of CQElement can be used by setting `type` to one of the following values:


##### AND
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQAnd`

The following fields are supported:

| Field | Type |
| --- | --- |
| children | `List` | 

##### BEFORE
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeTemporalQuery`

No fields can be set for this type.


##### BEFORE_OR_SAME
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQBeforeOrSameTemporalQuery`

No fields can be set for this type.


##### CONCEPT
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQConcept`

The following fields are supported:

| Field | Type |
| --- | --- |
| excludeFromTimeAggregation | `boolean` | 
| ids | `List` | 
| label | `String` | 
| selects | `List` | 
| tables | `List` | 

##### DATE_RESTRICTION
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQDateRestriction`

The following fields are supported:

| Field | Type |
| --- | --- |
| child | [CQElement](#Base-CQElement) | 
| dateRange | `Range` | 

##### DAYS_BEFORE
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeTemporalQuery`

No fields can be set for this type.


##### DAYS_OR_NO_EVENT_BEFORE
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQDaysBeforeOrNeverTemporalQuery`

No fields can be set for this type.


##### EXTERNAL
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQExternal`

No fields can be set for this type.


##### EXTERNAL_RESOLVED
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQExternalResolved`

The following fields are supported:

| Field | Type |
| --- | --- |
| values | `Map` | 

##### NEGATION
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQNegation`

The following fields are supported:

| Field | Type |
| --- | --- |
| child | [CQElement](#Base-CQElement) | 

##### OR
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQOr`

The following fields are supported:

| Field | Type |
| --- | --- |
| children | `List` | 

##### SAME
Java Name: `com.bakdata.conquery.models.query.concept.specific.temporal.CQSameTemporalQuery`

No fields can be set for this type.


##### SAVED_QUERY
Java Name: `com.bakdata.conquery.models.query.concept.specific.CQReusedQuery`

No fields can be set for this type.




### Base FilterValue


Different types of FilterValue can be used by setting `type` to one of the following values:


##### BIG_MULTI_SELECT
Java Name: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | `Filter` | 
| value | `Object` | 

##### INTEGER_RANGE
Java Name: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQIntegerRangeFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | `Filter` | 
| value | `Object` | 

##### MONEY_RANGE
Java Name: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQIntegerRangeFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | `Filter` | 
| value | `Object` | 

##### MULTI_SELECT
Java Name: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQMultiSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | `Filter` | 
| value | `Object` | 

##### REAL_RANGE
Java Name: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQRealRangeFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | `Filter` | 
| value | `Object` | 

##### SELECT
Java Name: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQSelectFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | `Filter` | 
| value | `Object` | 

##### STRING
Java Name: `com.bakdata.conquery.models.query.concept.filter.FilterValue$CQStringFilter`

The following fields are supported:

| Field | Type |
| --- | --- |
| filter | `Filter` | 
| value | `Object` | 


