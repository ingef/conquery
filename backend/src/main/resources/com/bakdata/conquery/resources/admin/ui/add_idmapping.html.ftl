<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<form action="/admin/datasets/${c.dataset}/mapping" enctype="multipart/form-data" method="post">
		<label>Upload an ID Mapping Here
			<input name="data_csv" type="file">
		</label>
		<input class="btn btn-primary" type="submit"/>
	</form>
</@layout.layout>