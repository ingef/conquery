<#import "templates/template.html.ftl" as layout>
<#import "templates/breadcrumbs.html.ftl" as breadcrumbs>
<#import "templates/infoCard.html.ftl" as infoCard>
<#import "templates/accordion.html.ftl" as accordion>
<#import "templates/table.html.ftl" as table>

<@layout.layout>
  <@breadcrumbs.breadcrumbs
    labels=["Datasets", c.imp.table.dataset.label, "Tables", c.imp.table.label, "Tags", c.imp.id]
    links=[
      "/admin-ui/datasets",
      "/admin-ui/datasets/${c.imp.table.dataset.id}",
      "/admin-ui/datasets/${c.imp.table.dataset.id}#Tables",
      "/admin-ui/datasets/${c.imp.table.dataset.id}/tables/${c.imp.table.id}",
      "/admin-ui/datasets/${c.imp.table.dataset.id}/tables/${c.imp.table.id}#Tags"
    ]
  />

  <@infoCard.infoCard
    class="d-inline-flex"
    title="Tag"
    labels=["ID", "Entries", "Size", "CBlocksSize"]
    values=[c.imp.id, c.imp.numberOfEntries?string.number, layout.si(c.imp.estimateMemoryConsumption())+"B", layout.si(c.getCBlocksMemoryBytes())+"B"]
  />

  <@accordion.accordion summary="Columns" infoText="${c.imp.columns?size} entries" class="my-3">
    <#assign idHeader="id" />
    <#assign sizeHeader="size" />
    <#assign typeHeader="type" />
    <@table.table
      columns=[idHeader, sizeHeader, typeHeader]
      items=c.imp.columns
        ?map( x ->
          {
            "${idHeader}": x.id,
            "${sizeHeader}": layout.si(x.getMemorySizeBytes())+"B",
            "${typeHeader}": x.typeDescription
          }
        )
    />
  </@accordion.accordion>
</@layout.layout>