<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<ul>
			<#list c as dataset>
				<li>
					<a href="/admin/datasets/${dataset.id}">${dataset.label}</a> 
					<a href="" onclick="event.preventDefault(); fetch('./datasets/${dataset.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
			<br/><br/><br/><br/>
			<h3>Create Dataset</h3>
			<form method="post" enctype="multipart/form-data">
				<div class="form-group">
					<label for="dataset_name">Name:</label>
					<input type="text" class="form-control" name="dataset_name" pattern="<#include "templates/namePattern.ftl">" title="Name of the new dataset" required>
				</div>
				<input class="btn btn-primary" type="submit"/>
			</form>
		</div>
	</div>
</@layout.layout>