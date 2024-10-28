<#import "templates/template.html.ftl" as layout>
<#import "templates/breadcrumbs.html.ftl" as breadcrumbs>
<#import "templates/infoCard.html.ftl" as infoCard>
<#import "templates/accordion.html.ftl" as accordion>
<#import "templates/table.html.ftl" as tableMacro>

<@layout.layout>
  <#assign table=c.imp.table.resolve() />
  <#assign dataset=c.imp.dataset.resolve() />
  <@breadcrumbs.breadcrumbs
    labels=["Datasets", dataset.label, "Tables", table.label, "Tags", c.imp.id]
    links=[
      "/admin-ui/datasets",
      "/admin-ui/datasets/${c.imp.table.dataset}",
      "/admin-ui/datasets/${c.imp.table.dataset}#Tables",
      "/admin-ui/datasets/${c.imp.table.dataset}/tables/${c.imp.table}",
      "/admin-ui/datasets/${c.imp.table.dataset}/tables/${c.imp.table}#Tags"
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
    <@tableMacro.table
      columns=[idHeader, sizeHeader, typeHeader]
      items=c.imp.columns
        ?map( x ->
          {
            "${idHeader}": x.id,
            "name": x.name,
            "${sizeHeader}": layout.si(x.getMemorySizeBytes())+"B",
            "${typeHeader}": x.typeDescription
          }
        )
    />
  </@accordion.accordion>
</@layout.layout>