<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<h3>Dataset ${c.ds.label}</h3>
	
	<@layout.kid k="ID" v=c.ds.id/>
	<@layout.kc k="Label">
		<form method="post" enctype="multipart/form-data">
			<input id="newDatasetLabel" type="text" name="label" title="Label of the dataset" value="${c.ds.label}">
			<input type="submit" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/label',{ method: 'post', body: document.getElementById('newDatasetLabel').value}).then(function(){location.reload();});"/>
		</form>
	</@layout.kc>
	<@layout.kv k="Dictionaries" v=layout.si(c.dictionariesSize)+"B"/>
	<@layout.kv k="Size" v=layout.si(c.size)+"B"/>
	<@layout.kc k="IdMapping"><a href="./${c.ds.id}/mapping">Here</a></@layout.kc>
	<@layout.kc k="SecondaryIds">
	    <ul>
        <#list c.secondaryIds as secondaryId>
            <li>${secondaryId}</li>
        </#list>
	    </ul>
	</@layout.kc>
    <@layout.kc k="Mappings">
        <ul>
        <#list c.secondaryIds as secondaryId>
            <li>${secondaryId}</li>
        </#list>
        </ul>
    </@layout.kc>
	<@layout.kc k="Tables">
		<ul>
			<#list c.tables?sort_by("label") as table>
				<li>
					<a href="./${c.ds.id}/tables/${table.id}">${table.label} <span>[${table.imports}] (${table.entries})</span></a>
					<a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/tables/${table.id}',{method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
		</ul>
	</@layout.kc>
	<@layout.kc k="Concepts">
		<ul>
		<#list c.concepts?sort_by("label") as concept>
			<li>
				<a href="./${c.ds.id}/concepts/${concept.id}">${concept.label}</a>
				<a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.ds.id}/concepts/${concept.id}',{method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
			</li>
		</#list>
		</ul>
	</@layout.kc>
	
	<form action="/admin/datasets/${c.ds.id}/update-matching-stats" method="post" enctype="multipart/form-data">
        <h3>Start Update Matching Stats Job</h3>
        <input class="btn btn-primary" type="submit"/>
    </form>
	<h3>Add Table</h3>
	<form onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/tables');">
		<div class="form-group">
			<input type="file" class="restparam" name="table_schema" title="Schema of the Table" accept="*.table.json" multiple required>
		</div>
		<input class="btn btn-primary" type="submit"/>
	</form>
	
	<h3>Add Concept</h3>
	<form onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/concepts');">
		<div class="form-group">
			<input type="file" class="restparam" name="concept_schema" title="Schema of the Concept" accept="*.concept.json" multiple required>
		</div>
		<input class="btn btn-primary" type="submit"/>
	</form>
	
	<h3>Structure Nodes</h3>
	<form onsubmit="postFile(event, '/admin/datasets/${c.ds.id}/structure');">
		<div class="form-group">
			<label for="structure_schema">Set Structure Nodes</label>
			<input type="file" class="restparam" name="structure_schema" title="Schema of the Structure Nodes" accept="structure.json" required>
		</div>
		<input class="btn btn-primary" type="submit"/>
	</form>
</@layout.layout>

