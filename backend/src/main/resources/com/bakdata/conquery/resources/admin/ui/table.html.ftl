<#import "templates/template.html.ftl" as layout>
<#import "templates/breadcrumbs.html.ftl" as breadcrumbs>
<#import "templates/infoCard.html.ftl" as infoCard>
<#import "templates/accordion.html.ftl" as accordion>
<#import "templates/table.html.ftl" as table>

<#macro deleteTagButton id>
  <a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.table.dataset.id}/tables/${c.table.id}/imports/${id}', {method : 'delete'}).then(() => location.reload());"><i class="fas fa-trash-alt text-danger"></i></a>
</#macro>

<#macro columnInfoRender id>
  <#assign element = c.table.columns
    ?filter( x -> x.id == id )
    ?first
  />
  <ul>
      <#if element.sharedDictionary??>
          <li style="display: inline;">Shared Dictionary  ${("<code>"?no_esc+element.sharedDictionary+"</code>"?no_esc)!}</li>
      </#if>
      <#if element.secondaryId??>
          <li style="display: inline;">${element.secondaryId}</li>
      </#if>
  </ul>
</#macro>

<@layout.layout>
  <@breadcrumbs.breadcrumbs
    labels=["Datasets", c.table.dataset.label, "Tables", c.table.label]
    links=[
      "/admin-ui/datasets",
      "/admin-ui/datasets/${c.table.dataset.id}",
      "/admin-ui/datasets/${c.table.dataset.id}#Tables"
    ]
  />
  <@infoCard.infoCard
    class="d-inline-flex"
    title="Table ${c.table.label}"
    labels=["ID", "Label", "Dictionaries", "CBlocks", "Size"]
    values=[c.table.id, c.table.label, layout.si(c.dictionariesSize)+"B", layout.si(c.getCBlocksSize())+"B", layout.si(c.size)+"B"]
  />

  <@accordion.accordionGroup class="mt-3">
    <@accordion.accordion summary="Tags" infoText="${c.imports?size} entries">
      <@table.table
        columns=["id", "name", "numberOfEntries", "actions"]
        items=c.imports?sort_by("name")
        link="/admin-ui/datasets/${c.table.dataset.id}/tables/${c.table.id}/import/"
        deleteButton=deleteTagButton
      />
    </@accordion.accordion>
    <@accordion.accordion summary="Concepts" infoText="${c.concepts?size} entries">
      <@table.table
        columns=["id", "name"]
        items=c.concepts?sort_by("name")
        link="/admin-ui/datasets/${c.table.dataset.id}/concepts/"
      />
    </@accordion.accordion>
    <@accordion.accordion summary="Columns" infoText="${c.table.columns?size} entries">
      <@table.table
        columns=["id", "label", "type", "infos"]
        items=c.table.columns?sort_by("name")
        renderers={ "infos": columnInfoRender }
      />
    </@accordion.accordion>
  </@accordion.accordionGroup>
</@layout.layout>