
# Import JSONs
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

Each `*.import.json` has to contain exactly one [ImportDescriptor](#Type-ImportDescriptor).


---

## Base OutputDescription


Different types of OutputDescription can be used by setting `operation` to one of the following values:


### COPY<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.CopyOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java) | inputColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/CopyOutput.java) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | required | `boolean` | `false` |  |  | 
</p></details>

### EPOCH<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochOutput.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochOutput.java) | inputColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | required | `boolean` | `false` |  |  | 
</p></details>

### EPOCH_DATE_RANGE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.EpochDateRangeOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java) | allowOpen | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java) | endColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/EpochDateRangeOutput.java) | startColumn | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | required | `boolean` | `false` |  |  | 
</p></details>

### LINE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/LineOutput.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.LineOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | required | `boolean` | `true` |  |  | 
</p></details>

### NULL<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/NullOutput.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.outputs.NullOutput`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/NullOutput.java) | inputType | one of STRING, INTEGER, BOOLEAN, REAL, DECIMAL, MONEY, DATE, DATE_RANGE | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/outputs/OutputDescription.java) | required | `boolean` | `false` |  |  | 
</p></details>



---

## Other Types

### Type TableImportDescriptor<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableImportDescriptor.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.TableImportDescriptor`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/Labeled.java) | label | `String` | `null` | `"someLabel"` | shown in the frontend | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/NamedImpl.java) | name | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableImportDescriptor.java) | inputs | list of [TableInputDescriptor](#Type-TableInputDescriptor) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableImportDescriptor.java) | table | `String` | `null` |  |  | 
</p></details>

### Type TableInputDescriptor<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.preproc.TableInputDescriptor`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java) | filter | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java) | output | list of [OutputDescription](#Base-OutputDescription) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java) | primary | [OutputDescription](#Base-OutputDescription) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/preproc/TableInputDescriptor.java) | sourceFile | `File` | `null` |  |  | 
</p></details>
