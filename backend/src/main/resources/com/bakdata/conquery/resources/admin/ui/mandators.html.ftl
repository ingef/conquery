<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<form action="/admin/mandator" method="POST" enctype="multipart/form-data">
				<div class="form-group">
					<h3>New Mandator</h3>
				  <label for="mandantor_name">Name:</label>
				  <input id="mandator_name" name="mandantor_name" class="form-control text-monospace" style="font-family:monospace;">
				  <label for="mandantor_id">ID:</label>
				  <input id="mandantor_id" name="mandantor_id"  class="form-control text-monospace" style="font-family:monospace;">
				  <input class="btn btn-primary" type="submit"/>
				</div> 
			</form>
		</div>
	</div>
</@layout.layout>