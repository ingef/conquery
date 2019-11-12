<#macro entityOverview pathBase entities entityName>
    <div class="row">
		<div class="col">
			<ul>
			<#list entities as entity>
				<li>
					<a href="${pathBase}${entity.id}">${entity.label}</a> 
					<a href="" onclick="event.preventDefault(); fetch('${pathBase}${entity.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
			<button class="btn btn-primary" onclick="downloadEntities()">Download</button>
			<form>
				<div class="form-group">
					<h3>Create ${entityName}</h3>
				  <label for="entity_name">Name:</label>
				  <input id="entity_name" name="entity_name" class="form-control text-monospace" style="font-family:monospace;">
				  <label for="entity_id">ID:</label>
				  <input id="entity_id" name="entity_id"  class="form-control text-monospace" style="font-family:monospace;">
				  <input class="btn btn-primary" type="submit" onclick="createEntity()"/>
				</div> 
			</form>

		</div>
	</div>
	<script type="application/javascript">
        function createEntity() {
            event.preventDefault(); 
            fetch('${pathBase}',
            {
                method: 'post',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                        name: document.getElementById('entity_id').value,
                        label: document.getElementById('entity_name').value
                    })
            }).then(function(){location.reload()});
        }

        function downloadEntities() {
            event.preventDefault(); 
            fetch('${pathBase}',
            {
                method: 'get',
                headers: {'Accept': 'application/json'}
            })
            .then(response => {return response.json()})
            .then(json => {
                console.log(json);
                uriContent = "data:application/octet-stream," + encodeURIComponent(JSON.stringify(json));
                newWindow = window.open(uriContent, 'entities');
                });
        }
	
	</script>
</#macro>