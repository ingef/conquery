<#import "templates/template.html.ftl" as layout>
<@layout.layout><#noparse>
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
					"translateId": `namespaces.get(new DatasetId("demo")).getStorage().getPrimaryDictionary().getId("3124")`,
					"addPermission": `namespaces.getMetaStorage().addPermission(
	new com.bakdata.conquery.models.auth.permissions.DatasetPermission(new com.bakdata.conquery.models.identifiable.ids.specific.UserId("demo@demo.com"),
	com.bakdata.conquery.models.auth.permissions.Ability.READ.asSet(),
	new com.bakdata.conquery.models.identifiable.ids.specific.DatasetId("test"))
)`,
					"dictionaryOverview": `result = [];
for(def ns : namespaces.getNamespaces()) {
	storage = ns.getStorage();
	result.add(new Tuple(
		storage.getPrimaryDictionary().estimateTypeSize(),
		storage.getPrimaryDictionary().size(),
		"PID "+ns.getDataset().getName(),
		storage.getPrimaryDictionary().getElement(0)
	));
	for(def imp : storage.getAllImports()) {
		if(imp.getTable().getTable()!="ALL_IDS_TABLE") {
			for(def col : imp.getColumns()) {
				if(col.getType() instanceof com.bakdata.conquery.models.events.stores.specific.AStringType) {
					result.add(new Tuple(
						   col.getType().estimateTypeSize(),
						   col.getType().size(),
						   col.getId(),
						   col.getType().getElement(0)
						));
				}
			}
		}
	}
}

result.sort{-it[0]};
String print ="size\tentries\tname\texample\\n";
for(def t:result) {
	print+="\${com.jakewharton.byteunits.BinaryByteUnit.format(t[0])}\t\${t[1]}\t\${t[2]}\t\${t[3]}\\n";
}
return print;`
				};
				
				
				
				
				
				
			</script>
			Sample Scripts:
			<select id="samples" onchange="setValue()">
				<option value="null"></option>
				<option value="translateId">find internal id</option>
				<option value="addPermission">add permission</option>
				<option value="datasetLabel">change dataset label</option>
				<option value="dictionaryOverview">dictionary overview</option>
			</select>
			
			<form action="/admin/script" method="POST" enctype="multipart/form-data">
				<div class="form-group">
					<label for="comment">Groovy Script:</label>
					<textarea id="script" class="form-control text-monospace" style="font-family:monospace;" rows="10" id="query"></textarea>
					<input class="btn btn-primary" type="submit"  onclick="event.preventDefault(); document.getElementById('answer').innerHTML='waiting for response'; fetch('/admin/script', {method: 'post', credentials: 'same-origin', body: document.getElementById('script').value, headers: {'Content-Type': 'text/plain'}}).then(response => response.text().then(function (text) {document.getElementById('answer').innerHTML=text}))"/>
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
</#noparse></@layout.layout>