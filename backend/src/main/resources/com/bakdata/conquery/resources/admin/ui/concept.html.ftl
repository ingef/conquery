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
      <@table.table
        columns=["id", "label", "simpleName", "description"]
        items=c.selects?sort_by("name")?map(x -> {"id": x.id, "label": x.label, "simpleName": x.class.simpleName, "description": x.description!""})
      />
    </@accordion.accordion>
    <@accordion.accordion summary="Connectors" infoText="${c.connectors?size} entries">
	    <#list c.connectors as connector>
        <div class="d-flex flex-row align-items-start my-3" style="gap: 0.5rem;">
          <@infoCard.infoCard
            labels=["ID", "Label", "Validity Dates", "Table"]
            values=[connector.id, connector.label, connector.validityDates?join(', '), connector.table.name]
            gridTemplateColumns="auto"
            style="flex-basis: 22rem; flex-shrink: 0;"
            links={"Table": "/admin-ui/datasets/${c.dataset.id}/tables/${connector.table.id}"}
          />
          <@accordion.accordionGroup style="flex-grow: 1; margin-top: 0 !important;">
            <@accordion.accordion summary="Filters" infoText="${connector.collectAllFilters()?size} entries">
              <@table.table
                columns=["id", "label", "simpleName", "requiredColumns"]
                items=connector.collectAllFilters()?sort_by("name")?map(x -> {"id": x.id, "label": x.label, "simpleName": x.class.simpleName, "requiredColumns": x.requiredColumns?sort_by("name")?join(', ')})
              />
            </@accordion.accordion>
            <@accordion.accordion summary="Selects" infoText="${connector.selects?size} entries">
              <@table.table
                columns=["id", "label", "simpleName", "requiredColumns"]
                items=connector.selects?sort_by("name")?map(x -> {"id": x.id, "label": x.label, "simpleName": x.class.simpleName, "requiredColumns": x.requiredColumns?sort_by("name")?join(', ')})
              />
            </@accordion.accordion>
          </@accordion.accordionGroup>
        </div>
      </#list>
    </@accordion.accordion>
  </@accordion.accordionGroup>
</@layout.layout>
