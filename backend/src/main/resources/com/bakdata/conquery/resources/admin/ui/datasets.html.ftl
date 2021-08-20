<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<ul>
			<#list c as dataset>
				<li>
					<a href="/admin-ui/datasets/${dataset.id}">${dataset.label}</a>
					<a href="" onclick="deleteDataset('${dataset.id}')"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
			<br/><br/>
			<form>
				<div class="form-group">
					<h3>Create Dataset</h3>
					<label for="entity_name">Name:</label>
					<input id="entity_name" name="entity_name" pattern="<#include "templates/namePattern.ftl">" class="form-control text-monospace" style="font-family:monospace;">
					<label for="entity_id">ID:</label>
					<input id="entity_id" name="entity_id"  class="form-control text-monospace" style="font-family:monospace;">
					<input class="btn btn-primary" type="submit" onclick="createDataset()"/>
				</div>
			</form>

		</div>
	</div>
	<script>

		function createDataset() {
			event.preventDefault();
			fetch(
				'/admin/datasets',
				{
					method: 'post',
					headers: {
                        'Content-Type': 'application/json'
                    },
					credentials: 'same-origin',
					body: JSON.stringify({
							name: document.getElementById('entity_id').value,
							label: document.getElementById('entity_name').value
						})
			}).then(function(){location.reload();});
		}

		function deleteDataset(datasetId) {
			event.preventDefault();
			fetch(
				${r"`/admin/datasets/${datasetId}`"},
				{
					method: 'delete',
					credentials: "same-origin"
				}).then(function(){location.reload();});
		}
	</script>
</@layout.layout>