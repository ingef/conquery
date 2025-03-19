# Solr Search Entities
We use solr's data binding to map search documents to java classes.

## Field Definitions
The solr binder does not really define which field type the document will use in solr. This is defined by the schema 
defined in the solr core/collection. We use the default managed-schema.xml which defines [`dynamicFields` here](https://github.com/apache/solr/blob/main/solr/server/solr/configsets/_default/conf/managed-schema.xml#L130).
As a consequence, we need to postfix our fields with e.g. `_s` to ensure our variable is not `multivalued` (the default 
for a java.lang.String field).

If we don't postfix out variable names the field becomes `multivalued`. This allows indexing beans, but fails when we
want to query and deserialize them.