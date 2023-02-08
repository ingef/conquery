<#import "templates/template.html.ftl" as layout>
<#import "templates/table.html.ftl" as table>

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

<@layout.layout>
	<h3>Dataset ${c.ds.label}</h3>
	
	<@layout.kid k="ID" v=c.ds.id/>
	<@layout.kc k="Label">
		<form method="post" enctype="multipart/form-data">
			<input id="newDatasetLabel" type="text" name="label" title="Label of the dataset" value="${c.ds.label}">
			<input type="submit" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/label',{ method: 'post', body: document.getElementById('newDatasetLabel').value}).then(function(res){if(res.ok)location.reload();});"/>
		</form>
	</@layout.kc>
	<@layout.kv k="Dictionaries" v=layout.si(c.dictionariesSize)+"B"/>
	<@layout.kv k="Size" v=layout.si(c.size)+"B"/>
	<@layout.kc k="IdMapping"><a href="./${c.ds.id}/mapping">Here</a></@layout.kc>
    <@layout.kc k="Mappings">
        <@table.table columns=columnsMappers items=c.internToExternMappers deleteButton=deleteMappersButton />
    </@layout.kc>
    <@layout.kc k="SearchIndices">
        <@table.table columns=columnsSearchIndices items=c.searchIndices deleteButton=deleteSearchIndiciesButton />
    </@layout.kc>
	<@layout.kc k="Tables">
        <@table.table columns=columnsTables items=c.tables?sort_by("name") deleteButton=deleteTablesButton link="./${c.ds.id}/tables/" />
	</@layout.kc>
	<@layout.kc k="Concepts">
        <@table.table columns=columnsConcepts items=c.concepts?sort_by("name") deleteButton=deleteConceptsButton link="./${c.ds.id}/concepts/" />
	</@layout.kc>
    <@layout.kc k="SecondaryIds">
        <@table.table columns=columnsSecondaryIds items=c.secondaryIds?sort_by("name") />
	</@layout.kc>

    <div class="container">
        <div class="row">
            <div class="col-sm">
                <button class="btn btn-primary" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/update-matching-stats',{method: 'post'})">
                        Update Matching Stats
                </button>

                <button class="btn btn-primary" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/clear-internToExtern-cache',{method: 'post'})">
                    Clear Mapping Cache
                </button>
            </div>
        </div>
    </div>

    <#assign uploadContainerStyle = "border border-secondary rounded p-2 m-2">
    <div class="container">
        <div class="row">
            <div class="${uploadContainerStyle}">
                <form onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/internToExtern');">
                    <div class="form-group>
                        <label for="mappingFile" class="form-label">Upload mapping JSON</label>
                        <input type="file" class="restparam form-control" id="mappingFile" name="mapping" title="Mapping configuration" accept="*.mapping.json" multiple required>
                    </div>
                    <input class="btn btn-primary" type="submit"/>
                </form>
            </div>

            <div class="${uploadContainerStyle}">
                <form onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/tables');">
                    <div class="form-group">
                        <label for="tableFile" class="form-label">Upload table JSON</label>
                        <input type="file" class="restparam form-control" id="tableFile" name="table_schema" title="Schema of the Table" accept="*.table.json" multiple required>
                    </div>
                    <input class="btn btn-primary" type="submit"/>
                </form>
            </div>

            <div class="${uploadContainerStyle}">
                <form onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/concepts');">
                    <div class="form-group">
                        <label for="conceptFile" class="form-label">Upload concept JSON</label>
                        <input type="file" class="restparam form-control" id="conceptFile" name="concept_schema" title="Schema of the Concept" accept="*.concept.json" multiple required>
                    </div>
                    <input class="btn btn-primary" type="submit"/>
                </form>
            </div>

            <div class="${uploadContainerStyle}">
                <form onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/structure');">
                    <div class="form-group">
                        <label for="structureFile" class="form-label">Upload structure JSON</label>
                        <input type="file" class="restparam form-control" id="structureFile" name="structure_schema" title="Schema of the Structure Nodes" accept="structure.json" required>
                    </div>
                    <input class="btn btn-primary" type="submit"/>
                </form>
            </div>

        </div>
    </div>
</@layout.layout>

