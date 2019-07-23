<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<ul>
			<#list c as mandator>
				<li>
					<a href="/admin/mandators/${mandator.id}">${mandator.label}</a> 
					<a href="" onclick="event.preventDefault(); fetch('./mandators/${mandator.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
			<form action="/admin/mandators" method="POST" enctype="multipart/form-data">
				<div class="form-group">
					<h3>New Mandator</h3>
				  <label for="mandator_name">Name:</label>
				  <input id="mandator_name" name="mandator_name" class="form-control text-monospace" style="font-family:monospace;">
				  <label for="mandator_id">ID:</label>
				  <input id="mandator_id" name="mandator_id"  class="form-control text-monospace" style="font-family:monospace;">
				  <input class="btn btn-primary" type="submit"/>
				</div> 
			</form>
		</div>
	</div>
</@layout.layout>