<#import "templates/template.html.ftl" as layout>
	<@layout.layout>
		<#noparse>
			<div class="row">
				<div class="col">
					<script>
					function setValue() {
						var x = document.getElementById("samples").value;
						document.getElementById("script").value = samples[x];
					}
					var samples = {
						"null": "",
						"datasetLabel": "com.bakdata.conquery.models.worker.Namespace ns = namespaces.get(new DatasetId(\"demo\"));\nns.getDataset().setLabel(\"Demo\");\nns.getStorage().updateDataset(ns.getDataset());",
						"addPermission": `namespaces.getMetaStorage().addPermission(
	new com.bakdata.conquery.models.auth.permissions.DatasetPermission(new com.bakdata.conquery.models.identifiable.ids.specific.UserId("demo@demo.com"),
	com.bakdata.conquery.models.auth.permissions.Ability.READ.asSet(),
	new com.bakdata.conquery.models.identifiable.ids.specific.DatasetId("test"))
)`				};
				
				
				
				
				
				
					</script>
					Sample Scripts:
					<select id="samples" onchange="setValue()">
						<option value="null"></option>
						<option value="translateId">find internal id</option>
						<option value="addPermission">add permission</option>
						<option value="datasetLabel">change dataset label</option>
					</select>
					<form action="/admin/script" method="POST" enctype="multipart/form-data">
						<div class="form-group">
							<div class="custom-control custom-switch">
								<input class="custom-control-input" type="checkbox" role="switch" id="jsonOutCheck" checked>
								<label class="custom-control-label" for="jsonOutCheck">JSON Response</label>
							</div>
							<label for="comment">Groovy Script:</label>
							<textarea id="script" class="form-control text-monospace" style="font-family:monospace;" rows="10" id="query"></textarea>
							<input class="btn btn-primary" type="submit" onclick="postScriptHandler(event, document.getElementById('jsonOutCheck').checked, document.getElementById('answer'))" />
						</div>
					</form>
					<div class="card">
						<div class="card-body">
							<h5 class="card-title">Answer</h5>
							<pre id="answer">
					</pre>
						</div>
					</div>
				</div>
			</div>
		</#noparse>
	</@layout.layout>