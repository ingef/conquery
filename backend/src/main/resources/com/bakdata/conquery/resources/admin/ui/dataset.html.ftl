<#import "templates/template.html.ftl" as layout>
<#import "templates/table.html.ftl" as table>
<#import "templates/accordion.html.ftl" as accordion>
<#import "templates/infoCard.html.ftl" as infoCard>
<#import "templates/editableText.html.ftl" as editableText>
<#import "templates/breadcrumbs.html.ftl" as breadcrumbs>

<#assign columnsMappers=["id", "initialized", "actions"]>
<#assign columnsSearchIndices=["id", "actions"]>
<#assign columnsTables=["id", "label", "imports", "entries", "actions"]>
<#assign columnsConcepts=["id", "label", "actions"]>
<#assign columnsSecondaryIds=["id", "label"]>

<#macro deleteButton id contentPath testId="">
	 <a href="" onclick="event.preventDefault(); restOptionalForce('/${ctx.staticUriElem.ADMIN_SERVLET_PATH}/datasets/${c.ds.id}/${contentPath}/${id}',{method: 'delete'}).then(function(res){if(res.ok)location.reload();});" data-test-id="${testId}">
    <i class="fas fa-trash-alt text-danger"></i>
  </a>
</#macro>
<#macro deleteMappersButton id><@deleteButton id="${id}" contentPath="internToExtern" /></#macro>
<#macro deleteSearchIndiciesButton id><@deleteButton id="${id}" contentPath="searchIndex" /></#macro>
<#macro deleteTablesButton id><@deleteButton id="${id}" contentPath="tables" testId="delete-btn-table-${id}" /></#macro>
<#macro deleteConceptsButton id><@deleteButton id="${id}" contentPath="concepts" testId="delete-btn-concept-${id}" /></#macro>

<#macro label>
  <@editableText.editableText text="${c.ds.label}" onChange="(label) => rest('/admin/datasets/${c.ds.id}/label',{ method: 'post', body: label}).then(function(res){if(res.ok)location.reload();})" />
</#macro>
<#macro idMapping><a href="./${c.ds.id}/mapping">Here</a></#macro>

<@layout.layout>
  <!-- Javascript -->
  <script><#include "scripts/dataset.js" /></script>

  <!-- Dataset page -->
  <@breadcrumbs.breadcrumbs
    labels=["Datasets", c.ds.label]
    links=["/admin-ui/datasets"]
  />
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
              data-test-id="upload-select"
              onchange="updateDatasetUploadForm(this)"
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
              data-test-id="upload-input"
              name="mapping"
              accept="*.mapping.json"
              multiple
              required
            />
            <input class="btn btn-primary" data-test-id="upload-btn" type="submit" value="upload"/>
          </form>
        </div>
      </div>
    </div>
    <!-- Dataset Actions -->
    <div class="d-flex flex-column" style="gap: 0.5rem;">
      <button 
        type="button"
        class="btn btn-secondary"
        onclick="rest('/admin/datasets/${c.ds.id}/update-matching-stats',{method: 'post'})"
      >
        Update Matching Stats
      </button>
      <button
        type="button"
        class="btn btn-danger"
        onclick="rest('/admin/datasets/${c.ds.id}/clear-internToExtern-cache',{method: 'post'})"
      >
        Clear Mapping Cache
      </button>
    </div>
  </div>

  <@accordion.accordionGroup>
    <@accordion.accordion summary="Mappings" infoText="${c.internToExternMappers?size} entries">
      <@table.table columns=columnsMappers items=c.internToExternMappers deleteButton=deleteMappersButton />
    </@accordion.accordion>
    <@accordion.accordion summary="SearchIndices" infoText="${c.searchIndices?size} entries">
      <@table.table columns=columnsSearchIndices items=c.searchIndices deleteButton=deleteSearchIndiciesButton />
    </@accordion.accordion>
    <@accordion.accordion summary="Tables" infoText="${c.tables?size} entries">
        <@table.table columns=columnsTables items=c.tables?sort_by("name") deleteButton=deleteTablesButton link="./${c.ds.id}/tables/" />
    </@accordion.accordion>
    <@accordion.accordion summary="Concepts" infoText="${c.concepts?size} entries">
        <@table.table columns=columnsConcepts items=c.concepts?sort_by("name") deleteButton=deleteConceptsButton link="./${c.ds.id}/concepts/" />
    </@accordion.accordion>
    <@accordion.accordion summary="SecondaryIds" infoText="${c.secondaryIds?size} entries">
        <@table.table columns=columnsSecondaryIds items=c.secondaryIds?sort_by("name") />
    </@accordion.accordion>
  </@accordion.accordionGroup>
</@layout.layout>

