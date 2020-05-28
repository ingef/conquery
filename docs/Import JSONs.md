
# Import JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

Each `*.import.json` has to contain exactly one [ImportDescriptor](#Type-ImportDescriptor).


---

## Base OutputDescription


Different types of OutputDescription can be used by setting `operation` to one of the following values:


### COPY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java#L15-L17)</sup></sub></sup>
Parse column as type.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.CopyOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java#L33) | inputColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java#L36) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L31) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L34) | required | `boolean` | `false` |  |  | 
</p></details>

### DATE_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput.java#L18-L20)</sup></sub></sup>
Parse input columns as {@link CDateRange}. Input values must be {@link com.bakdata.conquery.models.common.CDate} based ints.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.DateRangeOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput.java#L31-L33) | allowOpen | `boolean` | `false` |  | Parse null values as open date-range if true. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput.java#L28) | endColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/DateRangeOutput.java#L28) | startColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L31) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L34) | required | `boolean` | `false` |  |  | 
</p></details>

### EPOCH<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochOutput.java#L13-L15)</sup></sub></sup>
Parse input column as {@link com.bakdata.conquery.models.common.CDate} based int.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochOutput.java#L23) | inputColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L31) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L34) | required | `boolean` | `false` |  |  | 
</p></details>

### EPOCH_DATE_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java#L15-L17)</sup></sub></sup>
Parse input columns as {@link CDateRange}. Input values must be {@link com.bakdata.conquery.models.common.CDate} based ints.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochDateRangeOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java#L28-L30) | allowOpen | `boolean` | `false` |  | Parse null values as open date-range if true. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java#L25) | endColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java#L25) | startColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L31) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L34) | required | `boolean` | `false` |  |  | 
</p></details>

### LINE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/LineOutput.java#L11-L13)</sup></sub></sup>
Outputs the current line in the file.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.LineOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L31) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L34) | required | `boolean` | `true` |  |  | 
</p></details>

### NULL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/NullOutput.java#L12-L14)</sup></sub></sup>
Output a null value.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.NullOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/NullOutput.java#L21) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L31) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java#L34) | required | `boolean` | `false` |  |  | 
</p></details>



---

## Other Types

### Type TableImportDescriptor<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableImportDescriptor.java#L21-L25)</sup></sub></sup>
Combines potentially multiple input files to be loaded into a single table. Describing their respective transformation. All Inputs must produce the same types of outputs. For further detail see {@link TableInputDescriptor}, and {@link Preprocessor}.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.TableImportDescriptor`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java#L19-L22) | label | `String` | `null` | "someLabel" | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java#L14) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableImportDescriptor.java#L38-L40) | inputs | list of [TableInputDescriptor](#Type-TableInputDescriptor) | `null` |  | A single source input. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableImportDescriptor.java#L32-L34) | table | `String` | `null` |  | Target table to load the import to. | 
</p></details>

### Type TableInputDescriptor<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java#L27-L33)</sup></sub></sup>
An input describes transformations on a single CSV file to be loaded into the table described in {@link TableImportDescriptor}. It requires a primary Output and at least one normal output. Input data can be filter using the field filter, which is evaluated as a groovy script on every row.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.TableInputDescriptor`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java#L47) | filter | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java#L56) | output | list of [OutputDescription](#Base-OutputDescription) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java#L49-L52) | primary | [OutputDescription](#Base-OutputDescription) |  |  | Output producing the primary column. This should be the primary key across all tables. Default is `COPY("pid", STRING)` | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java#L44) | sourceFile | `File` | `null` |  |  | 
</p></details>
