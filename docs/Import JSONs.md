
# Import JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.


### Base Output


Different types of Output can be used by setting `operation` to one of the following values:


##### CONCAT
Java Name: `com.bakdata.conquery.models.preproc.outputs.ConcatOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| inputColumns | List of `int` | 
| name | `String` | 
| required | `boolean` | 

##### COPY
Java Name: `com.bakdata.conquery.models.preproc.outputs.CopyOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| inputColumn | `int` | 
| inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | 
| name | `String` | 
| required | `boolean` | 

##### DATE_RANGE
Java Name: `com.bakdata.conquery.models.preproc.outputs.DateRangeOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| endColumn | `int` | 
| startColumn | `int` | 
| name | `String` | 
| required | `boolean` | 

##### EPOCH
Java Name: `com.bakdata.conquery.models.preproc.outputs.EpochOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| column | `int` | 
| name | `String` | 
| required | `boolean` | 

##### EPOCH_DATE_RANGE
Java Name: `com.bakdata.conquery.models.preproc.outputs.EpochDateRangeOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| endColumn | `int` | 
| startColumn | `int` | 
| name | `String` | 
| required | `boolean` | 

##### LINE
Java Name: `com.bakdata.conquery.models.preproc.outputs.LineOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| name | `String` | 
| required | `boolean` | 

##### NULL
Java Name: `com.bakdata.conquery.models.preproc.outputs.NullOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | 
| name | `String` | 
| required | `boolean` | 

##### QUARTER_TO_FIRST_DAY
Java Name: `com.bakdata.conquery.models.preproc.outputs.QuarterToFirstDayOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| name | `String` | 
| required | `boolean` | 
| quarterColumn | `int` | 
| yearColumn | `int` | 

##### QUARTER_TO_RANGE
Java Name: `com.bakdata.conquery.models.preproc.outputs.QuarterToRangeOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| name | `String` | 
| required | `boolean` | 
| quarterColumn | `int` | 
| yearColumn | `int` | 

##### SOURCE
Java Name: `com.bakdata.conquery.models.preproc.outputs.SourceOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| name | `String` | 
| required | `boolean` | 

##### STARTING_FROM
Java Name: `com.bakdata.conquery.models.preproc.outputs.StartingFromOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| name | `String` | 
| required | `boolean` | 
| inputColumn | `int` | 

##### UNPIVOT
Java Name: `com.bakdata.conquery.models.preproc.outputs.UnpivotOutput`

The following fields are supported:
| Field | Type |
| --- | --- |
| name | `String` | 
| required | `boolean` | 
| includeNulls | `boolean` | 
| inputColumns | List of `int` | 
| inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | 


