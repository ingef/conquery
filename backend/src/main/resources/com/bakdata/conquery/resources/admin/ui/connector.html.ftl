<#import "templates/template.html.ftl" as layout>
<#import "templates/breadcrumbs.html.ftl" as breadcrumbs>
<#import "templates/infoCard.html.ftl" as infoCard>
<#import "templates/accordion.html.ftl" as accordion>
<#import "templates/table.html.ftl" as table>

<#function prepareTableConnectorItems connectorItems>
  <#return connectorItems
    ?sort_by("name")
    ?map(x -> {
      "${idHeader}": x.id,
      "name": x.name,
      "${labelHeader}": x.label,
      "${requiredColumnsHeader}": x.requiredColumns?sort_by("name")?join(', ')
    })
  />
</#function>

<@layout.layout>
  <@breadcrumbs.breadcrumbs
    labels=["Datasets", c.concept.dataset.label, "Concept", c.concept.label, "Connector", c.label]
    links=[
      "/admin-ui/datasets",
      "/admin-ui/datasets/${c.dataset.id}",
      "/admin-ui/datasets/${c.dataset.id}#Concepts",
      "/admin-ui/datasets/${c.dataset.id}/concepts/${c.concept.id}"
      "/admin-ui/datasets/${c.dataset.id}/concepts/${c.concept.id}#Connectors"
    ]
  />

  <@infoCard.infoCard
    class="d-inline-flex mt-2"
    labels=["ID", "Label", "Validity Dates", "Table"]
    values=[c.id, c.label, c.validityDates?join(', '), c.table.name]
    links={"Table": "/admin-ui/datasets/${c.dataset.id}/tables/${c.table.id}"}
  />
  <@accordion.accordionGroup>
    <#assign idHeader = "id">
    <#assign labelHeader = "label">
    <#assign requiredColumnsHeader = "requiredColumns">
    <@accordion.accordion summary="Filters" infoText="${c.collectAllFilters()?size} entries">
      <@table.table
        columns=[idHeader, labelHeader, requiredColumnsHeader]
        items=prepareTableConnectorItems(c.collectAllFilters())
      />
    </@accordion.accordion>
    <@accordion.accordion summary="Selects" infoText="${c.selects?size} entries">
      <@table.table
        columns=[idHeader, labelHeader, requiredColumnsHeader]
        items=prepareTableConnectorItems(c.selects)
      />
    </@accordion.accordion>
  </@accordion.accordionGroup>
</@layout.layout>