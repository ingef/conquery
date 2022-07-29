
# Config JSON
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

The `config.json` is required for every type of execution. Its root element is a [ConqueryConfig](#Type-ConqueryConfig) object.


---

## Base AuthorizationConfig
An `AuthorizationConfig` defines the initial users that are created on application startup and other permission related options.

Different types of AuthorizationConfig can be used by setting `type` to one of the following values:


### DEFAULT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/DefaultAuthorizationConfig.java#L11)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.auth.DefaultAuthorizationConfig`

No fields can be set for this type.

</p></details>

### DEVELOPMENT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/DevelopmentAuthorizationConfig.java#L16)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.auth.DevelopmentAuthorizationConfig`

No fields can be set for this type.

</p></details>



---

## Base AuthenticationRealmFactory
An `AuthenticationConfig` is used to define how specific realms for authentication are configured.

Different types of AuthenticationRealmFactory can be used by setting `type` to one of the following values:


### API_TOKEN<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/ApiTokenRealmFactory.java#L27)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.auth.ApiTokenRealmFactory`

No fields can be set for this type.

</p></details>

### DEVELOPMENT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/develop/DevAuthConfig.java#L10-L13)</sup></sub></sup>
Default configuration for the auth system. Sets up all other default components. This configuration causes that every request is handled as invoked by the super user.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.auth.develop.DevAuthConfig`

No fields can be set for this type.

</p></details>

### JWT_PKCE_REALM<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/JwtPkceVerifyingRealmFactory.java#L51-L62)</sup></sub></sup>
A realm that verifies oauth tokens using PKCE. Since the adminEnd-UI mainly works with direct links that do not support setting of the Authorization-header to transport an access token and it also does not support the oauth code flow, this realm proxies the flow. 1. The user/client is redirected to the IDP for authentication and redirected back to the adminEnd 2. The this realm then picks up the code the transported authorization code from the query and redeems it for an access token and refresh token. 3. Both are converted to cookies, which are saved on the client upon redirection to the page the user initially wanted to visit. 4. With the redirection the client sends back the cookies from which the adminEnd extracts the access token and verifies it. (5.) If the previous step failed. The refresh token is extracted and exchanged for a fresh access token and refresh token and continues with step 3.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.auth.JwtPkceVerifyingRealmFactory`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/JwtPkceVerifyingRealmFactory.java#L76) | additionalTokenChecks | list of `String` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/JwtPkceVerifyingRealmFactory.java#L99-L102) | alternativeIdClaims | list of `String` | `[]` |  | Which claims hold alternative Ids of the user in case the user name does not match a user. Pay attention, that the user must not be able to alter the value of any of these claims. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/JwtPkceVerifyingRealmFactory.java#L70-L73) | client | `String` | `null` |  | The client id is also used as the expected audience in the validated token. Ensure that the IDP is configured accordingly. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/JwtPkceVerifyingRealmFactory.java#L85-L87) | idpConfiguration | `IdpConfiguration` | `null` |  | See wellKnownEndpoint. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/JwtPkceVerifyingRealmFactory.java#L90-L94) | tokenLeeway | `@javax.validation.constraints.Min(0) int` | `60` |  | A leeway for token's expiration in seconds, this should be a short time. <p> One Minute is the default. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/JwtPkceVerifyingRealmFactory.java#L79-L82) | wellKnownEndpoint | `URI` | `null` |  | Either the wellKnownEndpoint from which an idpConfiguration can be obtained or the idpConfiguration must be supplied. If the idpConfiguration is given, the wellKnownEndpoint is ignored. | 
</p></details>

### LOCAL_AUTHENTICATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/LocalAuthenticationConfig.java#L34)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.auth.LocalAuthenticationConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/LocalAuthenticationConfig.java#L56) | directory | `File` | `"./storage"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/LocalAuthenticationConfig.java#L46) | jwtDuration | `@MinDuration(value=1, unit=TimeUnit.MINUTES) Duration` | `"12 hours"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/LocalAuthenticationConfig.java#L40-L42) | passwordStoreConfig | [XodusConfig](#Type-XodusConfig) |  |  | Configuration for the password store. An encryption for the store it self might be set here. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/LocalAuthenticationConfig.java#L49-L51) | storeName | `String` | `"authenticationStore"` |  | The name of the folder the store lives in. | 
</p></details>

### OIDC_AUTHORIZATION_CODE_FLOW<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/OIDCAuthorizationCodeFlowRealmFactory.java#L11-L13)</sup></sub></sup>
Factory for a simple realm that just forwards tokens to the IDP for verification.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.auth.OIDCAuthorizationCodeFlowRealmFactory`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/OIDCAuthorizationCodeFlowRealmFactory.java#L18) | client | `IntrospectionDelegatingRealmFactory` | `null` |  |  | 
</p></details>

### OIDC_RESOURCE_OWNER_PASSWORD_CREDENTIAL_AUTHENTICATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/OIDCResourceOwnerPasswordCredentialRealmFactory.java#L15-L17)</sup></sub></sup>
Realm that supports the Open ID Connect Resource-Owner-Password-Credential-Flow with a Keycloak IdP.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.auth.OIDCResourceOwnerPasswordCredentialRealmFactory`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/auth/OIDCResourceOwnerPasswordCredentialRealmFactory.java#L22) | client | `IntrospectionDelegatingRealmFactory` | `null` |  |  | 
</p></details>



---

## Base PluginConfig
A `PluginConfig` is used to define settings for Conquery plugins.

Different types of PluginConfig can be used by setting `type` to one of the following values:




---

## Other Types

### Type APIConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/APIConfig.java#L6)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.APIConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/APIConfig.java#L9) | allowCORSRequests | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/APIConfig.java#L10) | caching | `boolean` | `true` |  |  | 
</p></details>

### Type CSVConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L20-L22)</sup></sub></sup>
Holds the necessary information to configure CSV parsers and writers.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.CSVConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L26) | comment | `char` | `"\u0000"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L27) | delimeter | `char` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L31) | encoding | `@NotNull Charset` | `"UTF-8"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L25) | escape | `char` | `"\\"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L28) | lineSeparator | `String` | `"\n"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L35) | maxColumns | `int` | `1000000` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L34) | parseHeaders | `boolean` | `true` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L30) | quote | `char` | `"\""` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L33) | skipHeader | `boolean` | `false` |  |  | 
</p></details>

### Type ClusterConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L14)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.ClusterConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L25-L29) | backpressure | `@javax.validation.constraints.Min(0) int` | `3000` |  | Amount of backpressure before jobs can volunteer to block to send messages to their shards. Mostly {@link com.bakdata.conquery.models.jobs.ImportJob} is interested in this. Note that an average import should create more than #Entities / {@linkplain #entityBucketSize} jobs (via {@link com.bakdata.conquery.models.jobs.CalculateCBlocksJob}) in short succession, which will cause it to sleep. This field helps alleviate memory pressure on the Shards by slowing down the Manager, should it be sending too fast. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L22) | entityBucketSize | `@javax.validation.constraints.Min(1) int` | `1000` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L18) | managerURL | `@Valid @NotNull InetAddress` | `"localhost"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L20) | mina | [MinaConfig](#Type-MinaConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L16) | port | `@io.dropwizard.validation.PortRange int` | `16170` |  |  | 
</p></details>

### Type ConqueryConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L34)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.ConqueryConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L76) | api | [APIConfig](#Type-APIConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L50) | arrow | `@Valid @NotNull ArrowConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L89) | authentication | `@Valid @NotNull AuthenticationConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L93) | authenticationRealms | list of [AuthenticationRealmFactory](#Base-AuthenticationRealmFactory) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L97) | authorizationRealms | [@Valid @NotNull AuthorizationConfig](#Base-AuthorizationConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L41) | cluster | [ClusterConfig](#Type-ClusterConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L47) | csv | [CSVConfig](#Type-CSVConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L110-L112) | debugMode | `boolean` or `null` | `null` |  | null means here that we try to deduce from an attached agent | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L100) | excel | `@Valid @NotNull ExcelConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L115) | failOnError | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L79) | frontend | [FrontendConfig](#Type-FrontendConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L104) | jerseyClient | `@Valid @NotNull JerseyClientConfiguration` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L64) | locale | [LocaleConfig](#Type-LocaleConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L87) | metricsConfig | `ConqueryMetricsConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L108) | plugins | list of `PluginConfig` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L44) | preprocessor | [PreprocessingConfig](#Type-PreprocessingConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L73) | queries | [QueryConfig](#Type-QueryConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L54-L56) | resultProviders | list of `ResultRendererProvider` |  |  | The order of this lists determines the ordner of the generated result urls in a query status. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L83) | search | `@NotNull @Valid SearchConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L67) | standalone | [StandaloneConfig](#Type-StandaloneConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L70) | storage | `@Valid @NotNull StoreFactory` |  |  |  | 
</p></details>

### Type CurrencyConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L206)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig$CurrencyConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L211) | decimalScale | `int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L210) | decimalSeparator | `String` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L208) | prefix | `String` | `"€"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L209) | thousandSeparator | `String` | `"."` |  |  | 
</p></details>

### Type FrontendConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L45)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L55) | currency | [CurrencyConfig](#Type-CurrencyConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L59) | queryUpload | `UploadConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L54) | version | `String` | `"0.0.0-SNAPSHOT"` |  |  | 
</p></details>

### Type LocaleConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L27)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.LocaleConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L33-L37) | dateFormatMapping | map from `Locale` to `String` |  |  | Mappings from user provided locale to date format which is used in the generation of result tables. The formats are also available for parsing dates using the {@link DateReader}. However, the locale is neglected there and the formats are tried until one that fits is found. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L30) | frontend | `@NotNull Locale` | `""` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L69-L73) | listFormats | list of `ListFormat` |  |  | List formats that are available for parsing inputs and (the first one) for rendering result tables. Spaces at the ends of the separator are only relevant for the output of results. For the input (parsing) the separator string can be surrounded by an arbitrary number of spaces. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L53-L57) | localeRangeStartEndSeparators | map from `Locale` to `String` |  |  | Mappings from user provided locale to date range format which is used in the generation of result tables. The formats are also available for parsing dates ranges using the {@link DateReader}. However, the locale is neglected there and the formats are tried until one that fits is found. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L45-L47) | parsingDateFormats | `@NotNull Set<String>` | `["yyyyMMdd"]` |  | Additional date formats that are available only for parsing. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L64-L66) | parsingRangeStartEndSeparators | `Set<String>` | `["/"]` |  | Additional date range formats that are available only for parsing. | 
</p></details>

### Type MinaConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L13)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.MinaConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L31-L34) | bothIdleTime | `int` | `0` |  | The delay before we notify a session that it has been idle on read and write. Default to infinite | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L22-L22) | maxReadBufferSize | `int` | `524288000` |  | The maximum size of the buffer used to read incoming data | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L16-L16) | minReadBufferSize | `int` | `64` |  | The minimum size of the buffer used to read incoming data | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L19-L19) | readBufferSize | `int` | `8192` |  | The default size of the buffer used to read incoming data | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L25-L25) | readerIdleTime | `int` | `0` |  | The delay before we notify a session that it has been idle on read. Default to infinite | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L43) | throughputCalculationInterval | `int` | `3` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L40-L40) | useReadOperation | `boolean` | `false` |  | A flag set to true when weallow the application to do a session.read(). Default to false | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L37-L37) | writeTimeout | `int` | `0` |  | The delay to wait for a write operation to complete before bailing out | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/MinaConfig.java#L28-L28) | writerIdleTime | `int` | `0` |  | The delay before we notify a session that it has been idle on write. Default to infinite | 
</p></details>

### Type PreprocessingConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L10)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.PreprocessingConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L17) | faultyLineThreshold | `@javax.validation.constraints.Min(0) @javax.validation.constraints.Max(1) double` | `0.01` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L14) | maximumPrintedErrors | `@javax.validation.constraints.Min(0) int` | `10` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L12) | nThreads | `@javax.validation.constraints.Min(1) int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L20) | parsers | `ParserConfig` |  |  |  | 
</p></details>

### Type QueryConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/QueryConfig.java#L8)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.QueryConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/QueryConfig.java#L11) | executionPool | `ThreadPoolDefinition` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/QueryConfig.java#L13) | oldQueriesTime | `Duration` | `"30 days"` |  |  | 
</p></details>

### Type StandaloneConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StandaloneConfig.java#L6)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.StandaloneConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StandaloneConfig.java#L8) | numberOfShardNodes | `int` | `2` |  |  | 
</p></details>

### Type XodusConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L16)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.XodusConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L22) | cipherBasicIV | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L20) | cipherId | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L21) | cipherKey | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L41) | envCloseForcedly | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L50) | envGatherStatistics | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L38) | envIsReadonly | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L46) | envMaxParallelReadonlyTxns | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L45) | envMaxParallelTxns | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L49) | envMonitorTxnsCheckFreq | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L47) | envMonitorTxnsTimeout | `@NotNull @Valid Duration` | `"10 minutes"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L39) | envReadonlyEmptyStores | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L40) | envStoreGetCacheSize | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L44) | envTxnDowngradeAfterFlush | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L43) | envTxnReplayMaxCount | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L42) | envTxnReplayTimeout | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L37) | fullFileReadonly | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L52) | gcEnabled | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L56) | gcFileMinAge | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L64) | gcFilesDeletionDelay | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L57) | gcFilesInterval | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L54) | gcMinUtilization | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L55) | gcRenameFiles | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L58) | gcRunPeriod | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L53) | gcStartIn | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L62) | gcTransactionAcquireTimeout | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L63) | gcTransactionTimeout | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L61) | gcUseExclusiveTransaction | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L60) | gcUtilizationFromFile | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L59) | gcUtilizationFromScratch | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L31) | logCacheFreePhysicalMemoryThreshold | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L33) | logCacheNonBlocking | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L29) | logCacheOpenFilesCount | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L28) | logCachePageSize | `Size` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L32) | logCacheShared | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L30) | logCacheUseNio | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L34) | logCleanDirectoryExpected | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L35) | logClearInvalid | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L23) | logDurableWrite | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L24) | logFileSize | `@MaxSize(value=1, unit=SizeUnit.GIGABYTES) Size` | `"400 megabytes"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L26) | logLockId | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L27) | logLockTimeout | `Duration` | `"1 second"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L36) | logSyncPeriod | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L65) | managementEnabled | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L66) | managementOperationsRestricted | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L18) | memoryUsage | `Size` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L19) | memoryUsagePercentage | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L51) | treeMaxPageSize | `int` or `null` | `null` |  |  | 
</p></details>

### Type XodusStoreFactory<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L79)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.XodusStoreFactory`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L123) | directory | `Path` | `"file://./storage"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L134-L136) | removeUnreadableFromStore | `boolean` | `false` |  | Flag for the {@link SerializingStore} whether to delete values from the underlying store, that cannot be mapped to an object anymore. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L139-L141) | unreadableDataDumpDirectory | `File` | `null` |  | When set, all values that could not be deserialized from the persistent store, are dump into individual files. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L130) | useWeakDictionaryCaching | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L125) | validateOnWrite | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L131) | weakCacheDuration | `@NotNull Duration` | `"48 hours"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusStoreFactory.java#L126) | xodus | [XodusConfig](#Type-XodusConfig) |  |  |  | 
</p></details>
