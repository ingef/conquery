<#import "templates/template.html.ftl" as layout>
<#import "templates/breadcrumbs.html.ftl" as breadcrumbs>
<#import "templates/infoCard.html.ftl" as infoCard>
<#import "templates/accordion.html.ftl" as accordion>
<#import "templates/table.html.ftl" as table>

<@layout.layout>
  <@breadcrumbs.breadcrumbs
    labels=["Datasets", c.dataset.label, "Concepts", c.label]
    links=[
      "/admin-ui/datasets",
      "/admin-ui/datasets/${c.dataset.id}",
      "/admin-ui/datasets/${c.dataset.id}#Concepts"
    ]
  />
  <@infoCard.infoCard
    class="d-inline-flex"
    title="Concept ${c.label}"
    labels=["ID", "Label", "Type", "Structure Parent", "Elements"]
    values=[c.id, c.label, c.class.simpleName, c.structureParent!"", c.countElements()?string.number]
  />

  <@accordion.accordionGroup class="mt-3">
    <@accordion.accordion summary="Selects" infoText="${c.selects?size} entries">
      <#assign idHeader = "id" />
      <#assign labelHeader = "label" />
      <#assign simpleNameHeader = "simpleName" />
      <#assign descriptionHeader = "description" />
      <@table.table
        columns=[idHeader, labelHeader, simpleNameHeader, descriptionHeader]
        items=c.selects
          ?sort_by("name")
          ?map( x ->
            {
              "${idHeader}": x.id,
              "name": x.name,
              "${labelHeader}": x.label,
              "${simpleNameHeader}": x.class.simpleName,
              "${descriptionHeader}": x.description!""
            }
          )
      />
    </@accordion.accordion>
    <@accordion.accordion summary="Connectors" infoText="${c.connectors?size} entries">
      <#assign idHeader = "id" />
      <#assign labelHeader = "label" />
      <#assign simpleNameHeader = "simpleName" />
      <#assign descriptionHeader = "description" />
      <@table.table
        columns=[idHeader, labelHeader, simpleNameHeader, descriptionHeader]
        items=c.connectors
          ?sort_by("name")
          ?map( x ->
            {
              "${idHeader}": x.id,
              "name": x.name,
              "${labelHeader}": x.label,
              "${simpleNameHeader}": x.class.simpleName,
              "${descriptionHeader}": x.description!""
            }
          )
        link="/admin-ui/datasets/${c.dataset.id}/connectors/"
      />
    </@accordion.accordion>
  </@accordion.accordionGroup>
</@layout.layout>
