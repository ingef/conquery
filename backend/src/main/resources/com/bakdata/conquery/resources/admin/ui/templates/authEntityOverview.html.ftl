<#macro entityOverview uiPathBase adminPathBase entities entityName>
<div class="container">
    <div class="row">
		<div class="col bg-light pb-3 rounded">
			<form>
				<h3>Create ${entityName}</h3>
                    <div class="form-group">
                      <label for="entity_name">Name:</label>
                      <input id="entity_name" name="entity_name" class="form-control text-monospace" style="font-family:monospace;">
                      <small id="nameHelp" class="form-text text-muted">The display name</small>
                    </div>

                    <div class="form-group">
                      <label for="entity_id">ID:</label>
                      <input id="entity_id" name="entity_id"  class="form-control text-monospace" style="font-family:monospace;">
                      <small id="idHelp" class="form-text text-muted">The internal id used to distinguish ${entityName}s (must be unique) </small>
                    </div>
                    <input class="btn btn-primary" type="submit" onclick="createEntity()"/>
			</form>
		</div>
	</div>
    <div class="row pt-5">
        <div class="col table-responsive">
            <table class="table table-sm table-striped">
                <thead>
                    <td>Label</td>
                    <td>Id</td>
                    <td></td>
                </thead>
                <tbody>
                <#list entities as entity>
                    <tr>
                        <td><a href="${uiPathBase}/${entity.id}">${entity.label}</a></td>
                        <td><a href="${uiPathBase}/${entity.id}">${entity.name}</a></td>
                        <td><a href="" onclick="deleteEntity('${entity.id}')"><i class="fas fa-trash-alt text-danger"></i></a></td>
                    </tr>
                </#list>
                </tbody>
            </table>
			<button class="btn btn-primary" onclick="downloadEntities()">Download</button>

		</div>
	</div>

</div>
	<script type="application/javascript">
        function createEntity() {
            event.preventDefault(); 
            rest('${adminPathBase}',
            {
                method: 'post',
                body: JSON.stringify({
                        name: document.getElementById('entity_id').value,
                        label: document.getElementById('entity_name').value
                    })
            }).then(function(){location.reload()});
        }

        function downloadEntities() {
            event.preventDefault(); 
            rest('${adminPathBase}',
            {
                method: 'get',
            })
            .then(response => {return response.json()})
            .then(json => {
                uriContent = "data:application/octet-stream," + encodeURIComponent(JSON.stringify(json));
                newWindow = window.open(uriContent, 'entities');
                });
        }

        function deleteEntity(entityId){
            event.preventDefault();
            rest(
                '${adminPathBase}/'+entityId,
                {
                    method: 'delete'
                })
                .then(function(){location.reload();});
        }
	
	</script>
</#macro>