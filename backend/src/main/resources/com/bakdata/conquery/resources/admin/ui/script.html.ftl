<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
		
			<script>
				function setValue() {
				  var x = document.getElementById("samples").value;
				  document.getElementById("script").value = samples[x];
				}
				
				var samples = {
					"null": "",
					"translateId": "namespaces.get(new DatasetId(\"demo\")).getStorage().getPrimaryDictionary().getId(\"3124\")"
				};
			</script>
			Sample Scripts:
			<select id="samples" onchange="setValue()">
				<option value="null"></option>
				<option value="translateId">find internal id</option>
			</select>
			
			<form action="/admin/script" method="POST" enctype="multipart/form-data">
				<div class="form-group">
					<label for="comment">Groovy Script:</label>
					<textarea id="script" class="form-control text-monospace" style="font-family:monospace;" rows="10" id="query"></textarea>
					<input class="btn btn-primary" type="submit"  onclick="event.preventDefault(); fetch('/admin/script', {method: 'post', body: document.getElementById('script').value, headers: {'Content-Type': 'text/plain'}}).then(response => response.text().then(function (text) {document.getElementById('answer').innerHTML=text}))"/>
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
</@layout.layout>