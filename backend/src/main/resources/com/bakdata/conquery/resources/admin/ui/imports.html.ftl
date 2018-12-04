<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<br/>
	<form method="post" enctype="multipart/form-data">
		<input type="text" name="tag" required/>
		<br/>
		<input type="file" id="filepicker" name="files" accept=".cqpp" required multiple />
		<br/>
		<input type="submit"/>
	</form>
</@layout.layout>