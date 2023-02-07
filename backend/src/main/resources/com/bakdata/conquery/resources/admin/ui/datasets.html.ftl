<#import "templates/template.html.ftl" as layout>
<#import "templates/styledTable.html.ftl" as styledTable>
<#assign columns=["id", "label"]>

<#macro deleteDatasetButton id>
	<a href="" onclick="deleteDataset('${id}')"><i class="fas fa-trash-alt text-danger"></i></a>
</#macro>

<@layout.layout>
	<div class="row">
		<div class="col">
			<div class="col bg-light pb-3 rounded">
				<form>
					<h3>Create Dataset</h3>
					<div class="form-group">
						<label for="entity_name">Name:</label>
						<input id="entity_name" name="entity_name" pattern="<#include "templates/namePattern.ftl">" class="form-control text-monospace" style="font-family:monospace;">
					</div>
					<div class="form-group">
						<label for="entity_id">ID:</label>
						<input id="entity_id" name="entity_id"  class="form-control text-monospace" style="font-family:monospace;">
					</div>
					<input class="btn btn-primary mt-3" type="submit" onclick="createDataset()"/>
				</form>
			</div>

			<div id="all_datasets" class="mt-1">
				<h3>Datasets</h3>
				<@styledTable.styledTable columns=columns items=c?sort_by("name") link="/admin-ui/datasets/" deleteButton=deleteDatasetButton />
			</div>

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