<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<form action="/admin/query" method="POST" enctype="multipart/form-data">
				<div class="form-group">
				  <label for="comment">Query JSON:</label>
				  <textarea id="query" class="form-control text-monospace" style="font-family:monospace;" rows="30" id="query">
{
	"type": "CONCEPT_QUERY",
	"root": {
		"type": "CONCEPT",
		"ids": "fdb.icd.a00-b99",
		"tables": [
			{
				"id": "fdb.icd.arzt_diagnose_icd_code"
			}
		]
	}
}</textarea>
				  <input class="btn btn-primary" type="submit"  onclick="event.preventDefault(); fetch('/admin/query/', {method: 'post', body: document.getElementById('query').value, headers: {'Content-Type': 'application/json'}}).then(response => response.blob()).then(blob => {var url = window.URL.createObjectURL(blob);var a = document.createElement('a');a.href = url;a.download = 'result.csv';document.body.appendChild(a);a.click();a.remove();})"/>
				</div> 
			</form>
		</div>
	</div>
</@layout.layout>