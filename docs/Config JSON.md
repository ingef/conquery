
# Config JSON
This is an automatically created documentation. It is not 100% accurate since the generator does not handle every edge case.

Instead of a list ConQuery also always accepts a single element.

The `config.json` is required for every type of execution. Its root element is a [ConqueryConfig](#Type-ConqueryConfig) object.


---

## Base AuthorizationConfig
An `AuthorizationConfig` defines the initial users that are created on application startup and other permission related options.

Different types of AuthorizationConfig can be used by setting `type` to one of the following values:


### DEFAULT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/DefaultAuthorizationConfig.java#L12)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.auth.DefaultAuthorizationConfig`

No fields can be set for this type.

</p></details>

### DEVELOPMENT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/develop/DevelopmentAuthorizationConfig.java#L18)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.auth.develop.DevelopmentAuthorizationConfig`

No fields can be set for this type.

</p></details>



---

## Base AuthenticationConfig
An `AuthenticationConfig` is used to define how specific realms for authentication are configured.

Different types of AuthenticationConfig can be used by setting `type` to one of the following values:


### DEVELOPMENT<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/develop/DevAuthConfig.java#L9-L12)</sup></sub></sup>
Default configuration for the auth system. Sets up all other default components. This configuration causes that every request is handled as invoked by the super user.

<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.auth.develop.DevAuthConfig`

No fields can be set for this type.

</p></details>

### LOCAL_AUTHENTICATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/basic/LocalAuthenticationConfig.java#L16)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.auth.basic.LocalAuthenticationConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/basic/LocalAuthenticationConfig.java#L27) | jwtDuration | `int` | `12` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/basic/LocalAuthenticationConfig.java#L21-L23) | passwordStoreConfig | [XodusConfig](#Type-XodusConfig) |  |  | Configuration for the password store. An encryption for the store it self might be set here. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/basic/LocalAuthenticationConfig.java#L30-L32) | storeName | `String` | `"authenticationStore"` |  | The name of the folder the store lives in. | 
</p></details>

### OIDC_RESOURCE_OWNER_PASSWORD_CREDENTIAL_AUTHENTICATION<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/auth/oidc/passwordflow/OIDCResourceOwnerPasswordCredentialRealmFactory.java#L25)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.auth.oidc.passwordflow.OIDCResourceOwnerPasswordCredentialRealmFactory`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | allowAnyHostname | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | alwaysRefreshToken | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | clientKeyPassword | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | clientKeystore | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | clientKeystorePassword | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | connectionPoolSize | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | disableTrustManager | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | ignoreOAuthQueryParameter | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | minTimeBetweenJwksRequests | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | pkce | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | policyEnforcerConfig | `PolicyEnforcerConfig` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | principalAttribute | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | proxyUrl | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | publicKeyCacheTtl | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | registerNodeAtStartup | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | registerNodePeriod | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | tokenCookiePath | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | tokenMinimumTimeToLive | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | tokenStore | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | truststore | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | truststorePassword | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | turnOffChangeSessionIdOnLogin | `boolean` or `null` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/AdapterConfig.java) | verifyTokenAudience | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | autodetectBearerOnly | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | bearerOnly | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | cors | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | corsAllowedHeaders | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | corsAllowedMethods | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | corsExposedHeaders | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | corsMaxAge | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | credentials | map from `String` to `Object` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | enableBasicAuth | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | exposeToken | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | publicClient | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | redirectRewriteRules | map from `String` to `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | resource | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseAdapterConfig.java) | useResourceRoleMappings | `boolean` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseRealmConfig.java) | authServerUrl | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseRealmConfig.java) | confidentialPort | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseRealmConfig.java) | realm | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseRealmConfig.java) | realmKey | `String` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/org/keycloak/representations/adapters/config/BaseRealmConfig.java) | sslRequired | `String` | ␀ |  |  | 
</p></details>



---

## Base PluginConfig
A `PluginConfig` is used to define settings for Conquery plugins.

Different types of PluginConfig can be used by setting `type` to one of the following values:




---

## Base IdMappingConfig
An `IdMappingConfig` is used to define how multi column entity IDs are printed and parsed

Different types of IdMappingConfig can be used by setting `type` to one of the following values:


### NO_ID_MAPPING<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/mapping/NoIdMapping.java#L9)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.identifiable.mapping.NoIdMapping`

No fields can be set for this type.

</p></details>

### SIMPLE<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/identifiable/mapping/SimpleIdMapping.java#L6)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.identifiable.mapping.SimpleIdMapping`

No fields can be set for this type.

</p></details>



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
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/CSVConfig.java#L31) | encoding | `Charset` | `"UTF-8"` |  |  | 
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
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L22) | entityBucketSize | `int` | `1000` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L18) | managerURL | `InetAddress` | `"localhost"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L20) | mina | [MinaConfig](#Type-MinaConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ClusterConfig.java#L16) | port | `int` | `16170` |  |  | 
</p></details>

### Type ConqueryConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L23)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.ConqueryConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L52) | api | [APIConfig](#Type-APIConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L39) | arrow | `ArrowConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L68) | authentication | list of [AuthenticationConfig](#Base-AuthenticationConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L72) | authorization | [AuthorizationConfig](#Base-AuthorizationConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L30) | cluster | [ClusterConfig](#Type-ClusterConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L36) | csv | [CSVConfig](#Type-CSVConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L55) | dateFormats | list of `String` | `["yyyy-MM-dd","yyyyMMdd","dd.MM.yyyy"]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L77-L79) | debugMode | `boolean` or `null` | `null` |  | null means here that we try to deduce from an attached agent | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L59) | frontend | [FrontendConfig](#Type-FrontendConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L65) | idMapping | [IdMappingConfig](#Base-IdMappingConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L41) | locale | [LocaleConfig](#Type-LocaleConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L63) | metricsConfig | `ConqueryMetricsConfig` |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L75) | plugins | list of `PluginConfig` | `[]` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L33) | preprocessor | [PreprocessingConfig](#Type-PreprocessingConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L49) | queries | [QueryConfig](#Type-QueryConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L43) | standalone | [StandaloneConfig](#Type-StandaloneConfig) |  |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/ConqueryConfig.java#L46) | storage | [StorageConfig](#Type-StorageConfig) |  |  |  | 
</p></details>

### Type CurrencyConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L20)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.FrontendConfig$CurrencyConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L26) | decimalScale | `int` | `2` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L25) | decimalSeparator | `String` | `","` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L23) | prefix | `String` | `"€"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/FrontendConfig.java#L24) | thousandSeparator | `String` | `"."` |  |  | 
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

### Type LocaleConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L11)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.LocaleConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L13) | currency | `Currency` | `"EUR"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/LocaleConfig.java#L15) | frontend | `Locale` | `""` |  |  | 
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

### Type PreprocessingConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L11)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.PreprocessingConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L13) | directories | list of [PreprocessingDirectories](#Type-PreprocessingDirectories) | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L20) | faultyLineThreshold | `double` | `0.01` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L17) | maximumPrintedErrors | `int` | `10` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L15) | nThreads | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingConfig.java#L23) | parsers | `ParserConfig` |  |  |  | 
</p></details>

### Type PreprocessingDirectories<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingDirectories.java#L14)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.PreprocessingDirectories`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingDirectories.java#L16) | csvDir | `File` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingDirectories.java#L18) | descriptionsDir | `File` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/PreprocessingDirectories.java#L20) | preprocessedOutputDir | `File` | `null` |  |  | 
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

### Type StorageConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L16)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.StorageConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L19) | directory | `File` | `"./storage"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L29) | nThreads | `int` | ␀ |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L32-L34) | removeUnreadablesFromStore | `boolean` | `false` |  | Flag for the {@link SerializingStore} whether to delete values from the underlying store, that cannot be mapped to an object anymore. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L37-L39) | unreadbleDataDumpDirectory | `Optional<File>` | `null` |  | When set, all values that could not be deserialized from the persistent store, are dump into individual files. | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L25) | useWeakDictionaryCaching | `boolean` | `true` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L21) | validateOnWrite | `boolean` | `false` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L26) | weakCacheDuration | `Duration` | `"48 hours"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/StorageConfig.java#L22) | xodus | [XodusConfig](#Type-XodusConfig) |  |  |  | 
</p></details>

### Type XodusConfig<sup><sub><sup> [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L17)</sup></sub></sup>


<details><summary>Details</summary><p>

Java Type: `com.bakdata.conquery.models.config.XodusConfig`

Supported Fields:

|  | Field | Type | Default | Example | Description |
| --- | --- | --- | --- | --- | --- |
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L23) | cipherBasicIV | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L21) | cipherId | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L22) | cipherKey | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L42) | envCloseForcedly | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L51) | envGatherStatistics | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L39) | envIsReadonly | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L47) | envMaxParallelReadonlyTxns | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L46) | envMaxParallelTxns | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L50) | envMonitorTxnsCheckFreq | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L48) | envMonitorTxnsTimeout | `Duration` | `"10 minutes"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L40) | envReadonlyEmptyStores | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L41) | envStoreGetCacheSize | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L45) | envTxnDowngradeAfterFlush | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L44) | envTxnReplayMaxCount | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L43) | envTxnReplayTimeout | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L38) | fullFileReadonly | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L53) | gcEnabled | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L57) | gcFileMinAge | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L65) | gcFilesDeletionDelay | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L58) | gcFilesInterval | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L55) | gcMinUtilization | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L56) | gcRenameFiles | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L59) | gcRunPeriod | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L54) | gcStartIn | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L63) | gcTransactionAcquireTimeout | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L64) | gcTransactionTimeout | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L62) | gcUseExclusiveTransaction | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L61) | gcUtilizationFromFile | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L60) | gcUtilizationFromScratch | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L32) | logCacheFreePhysicalMemoryThreshold | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L34) | logCacheNonBlocking | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L30) | logCacheOpenFilesCount | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L29) | logCachePageSize | `Size` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L33) | logCacheShared | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L31) | logCacheUseNio | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L35) | logCleanDirectoryExpected | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L36) | logClearInvalid | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L24) | logDurableWrite | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L25) | logFileSize | `Size` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L27) | logLockId | `String` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L28) | logLockTimeout | `Duration` | `"1 second"` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L37) | logSyncPeriod | `long` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L66) | managementEnabled | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L67) | managementOperationsRestricted | `boolean` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L19) | memoryUsage | `Size` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L20) | memoryUsagePercentage | `int` or `null` | `null` |  |  | 
| [✎](https://github.com/bakdata/conquery/edit/develop/backend/src/main/java/com/bakdata/conquery/models/config/XodusConfig.java#L52) | treeMaxPageSize | `int` or `null` | `null` |  |  | 
</p></details>
