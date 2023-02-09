<#import "templates/template.html.ftl" as layout>
<#import "templates/table.html.ftl" as table>
<#import "templates/accordion.html.ftl" as accordion>
<#import "templates/infoCard.html.ftl" as infoCard>

<#assign columnsMappers=["id", "initialized", "actions"]>
<#assign columnsSearchIndices=["id", "actions"]>
<#assign columnsTables=["id", "label", "imports", "entries", "actions"]>
<#assign columnsConcepts=["id", "label", "actions"]>
<#assign columnsSecondaryIds=["id", "label"]>

<#macro deleteMappersButton id>
	 <a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/internToExtern/${id}',{method: 'delete'}).then(function(res){if(res.ok)location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
</#macro>

<#macro deleteSearchIndiciesButton id>
	<a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/searchIndex/${id}',{method: 'delete'}).then(function(res){if(res.ok)location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
</#macro>

<#macro deleteTablesButton id>
	<a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/tables/${id}',{method: 'delete'}).then(function(res){if(res.ok)location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
</#macro>

<#macro deleteConceptsButton id>
    <a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/concepts/${id}',{method: 'delete'}).then(function(res){if(res.ok)location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
</#macro>

<#macro label>
  <form method="post" enctype="multipart/form-data">
    <input id="newDatasetLabel" type="text" name="label" title="Label of the dataset" value="${c.ds.label}">
    <input type="submit" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/label',{ method: 'post', body: document.getElementById('newDatasetLabel').value}).then(function(res){if(res.ok)location.reload();});"/>
  </form>
</#macro>
<#macro idMapping><a href="./${c.ds.id}/mapping">Here</a></#macro>

<@layout.layout>
  <div class="d-flex justify-content-between mb-3">
    <div class="d-flex align-items-start">
      <@infoCard.infoCard
        class="d-inline-flex"
        title="Dataset ${c.ds.label}"
        labels=["ID", "Label", "Dictionaries", "Size", "IdMapping"]
        values=[c.ds.id, label, layout.si(c.dictionariesSize)+"B", layout.si(c.size)+"B", idMapping]
      />
      <!-- File Upload -->
      <div class="card d-inline-flex mx-3">
        <div class="card-body">
          <h5 class="card-title">File Upload</h5>
          <form class="d-flex flex-column align-items-stretch" onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/internToExtern');">
            <select
              class="custom-select"
              onchange="let x = {mapping: {name: 'mapping', uri: 'internToExtern', accept: '*.mapping.json'}, table: {name: 'table_schema', uri: 'tables', accept: '*.table.json'}, concept: {name: 'concept_schema', uri: 'concepts', accept: '*.concept.json'}, structure: {name: 'structure_schema', uri: 'structure', accept: 'structure.json'}}; let data = x[this.value]; let fi = $(this).next(); fi.value = ''; fi.attr('accept', data.accept); fi.attr('name', data.name); $(this).parent().attr('onsubmit', 'postFile(event, \'/admin/datasets/${c.ds.id}/' + data.uri + '\')');"
              required
            >
              <option value="mapping" selected>Mapping JSON</option>
              <option value="table">Table JSON</option>
              <option value="concept">Concept JSON</option>
              <option value="structure">Structure JSON</option>
            </select>
            <input
              type="file"
              class="restparam form-control my-3"
              name="mapping"
              accept="*.mapping.json"
              multiple
              required
            />
            <input class="btn btn-primary" type="submit"/>
          </form>
        </div>
      </div>
    </div>
    <!-- Dataset Actions -->
    <div>
      <button 
        type="button" class="btn" data-toggle="tooltip" data-placement="bottom" title="Update Matching Stats"
        onclick="rest('/admin/datasets/${c.ds.id}/update-matching-stats',{method: 'post'})"
      >
        <i class="fa fa-sync"></i>
      </button>
      <button
        type="button" class="btn" data-toggle="tooltip" data-placement="bottom" title="Clear Mapping Cache"
        onclick="rest('/admin/datasets/${c.ds.id}/clear-internToExtern-cache',{method: 'post'})"
      >
        <i class="fa fa-eraser"></i>
      </button>
    </div>
  </div>

  <@accordion.accordionGroup>
    <@accordion.accordion summary="Mappings" infoText="${c.internToExternMappers?size} Einträge">
      <@table.table columns=columnsMappers items=c.internToExternMappers deleteButton=deleteMappersButton />
    </@accordion.accordion>
    <@accordion.accordion summary="SearchIndices" infoText="${c.searchIndices?size} Einträge">
      <@table.table columns=columnsSearchIndices items=c.searchIndices deleteButton=deleteSearchIndiciesButton />
    </@accordion.accordion>
    <@accordion.accordion summary="Tables" infoText="${c.tables?size} Einträge">
        <@table.table columns=columnsTables items=c.tables?sort_by("name") deleteButton=deleteTablesButton link="./${c.ds.id}/tables/" />
    </@accordion.accordion>
    <@accordion.accordion summary="Concepts" infoText="${c.concepts?size} Einträge">
        <@table.table columns=columnsConcepts items=c.concepts?sort_by("name") deleteButton=deleteConceptsButton link="./${c.ds.id}/concepts/" />
    </@accordion.accordion>
    <@accordion.accordion summary="SecondaryIds" infoText="${c.secondaryIds?size} Einträge">
        <@table.table columns=columnsSecondaryIds items=c.secondaryIds?sort_by("name") />
    </@accordion.accordion>
  </@accordion.accordionGroup>
</@layout.layout>

