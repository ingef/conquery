<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<h3>Dataset ${c.label}</h3>
	<div class="row">
		<div class="col">Name / Id</div>
		<div class="col">${c.id}</div>
		<div class="col-7"></div>
	<div class="w-100"></div>
		<div class="col">Label</div>
		<div class="col">${c.label}</div>
		<div class="col-7">
			<#-- <form method="post" enctype="multipart/form-data">
				<input type="text" name="dataset_label" title="Label of the dataset" value="${c.label}">
				<input type="submit"/>
			</form>
			-->
		</div>
	<div class="w-100"></div>
		<div class="col">Tables</div>
		<div class="col">
			<ul>
				<#list c.tables as id, table>
					<li>
						<a href="/admin/datasets/${c.id}/tables/${table.id}">${table.label}</a> 
						<a href="" onclick="event.preventDefault(); fetch('/admin/datasets/${c.id}/tables/${table.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
					</li>
				</#list>
			</ul>
		</div>
		<div class="col-7">
			<form action="/admin/datasets/${c.id}/tables" method="post" enctype="multipart/form-data">
				<div class="form-group">
					<label for="table_schema">Add Table</label>
					<input class="form-control-file" type="file" name="table_schema" title="Schema of the table" accept="*.table.json" multiple required>
				</div>
				<input class="btn btn-primary" type="submit"/>
			</form>
		</div>
	<div class="w-100"></div>
		<div class="col">Concepts</div>
		<div class="col">
			<ul>
			<#list c.concepts as concept>
				<li>
					<a href="/admin/datasets/${c.id}/concepts/${concept.id}">${concept.label}</a>
					<a href="" onclick="event.preventDefault(); fetch('/admin/datasets/${c.id}/concepts/${concept.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
		</div>
		<div class="col-7">
			<form action="/admin/datasets/${c.id}/concepts" method="post" enctype="multipart/form-data">
				<div class="form-group">
					<label for="concept_schema">Add Concept</label>
					<input type="file" name="concept_schema" title="Schema of the Concept" accept="*.concept.json" required>
				</div>
				<input class="btn btn-primary" type="submit"/>
			</form>
		</div>
	</div>
	
	<div class="row">
		<div class="col">
			<h3>Structure Nodes</h3>
			<form onsubmit="postFile(event, '/admin/datasets/${c.id}/structure');">
				<div class="form-group">
					<label for="structure_schema">Set Structure Nodes</label>
					<input type="file" class="restparam" name="structure_schema" title="Schema of the Structure Nodes" accept="structure.json" required>
				</div>
				<input class="btn btn-primary" type="submit"/>
			</form>
		</div>
	</div>
	<div class="row">
		<div class="col">
			<h3>Import Files</h3>
			<#macro linkCreator>
				<a href="#" onclick="event.preventDefault(); fetch('/admin/datasets/${c.id}/imports?file='+encodeURIComponent('<#nested/>'.trim()), {method: 'post'}).then(function(){location.reload();});">
			</#macro>
			<#include "templates/fileChooser.html.ftl"/>
		</div>
	</div>
	<div class="row">
		<div class="col">
			<h2>IdMapping</h2>
			<a href="/admin/datasets/${c.id}/mapping">Here</a>
		</div>
	</div>
</@layout.layout>
