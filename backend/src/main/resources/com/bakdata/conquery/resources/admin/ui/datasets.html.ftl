<#import "templates/template.html.ftl" as layout>
<#import "templates/table.html.ftl" as table>
<#assign columns=["id", "label", "actions"]>

<#macro deleteDatasetButton id>
	<a data-test-id="delete-btn-${id}" href="" onclick="deleteDataset(event, '${id}')"><i class="fas fa-trash-alt text-danger"></i></a>
</#macro>

<@layout.layout>
	<div class="row">
		<div class="col">
			<h1>Datasets</h1>
			<div class="col bg-light pb-3 pt-1 rounded">
				<form>
					<h3>Create Dataset</h3>
					<div class="form-group">
						<label for="entity_name">Label:</label>
						<input id="entity_name" name="entity_name" data-test-id="entity-name" pattern="<#include "templates/namePattern.ftl">" class="form-control text-monospace" style="font-family:monospace;">
					</div>
					<div class="form-group">
						<label for="entity_id">ID:</label>
						<input id="entity_id" name="entity_id" data-test-id="entity-id" class="form-control text-monospace" style="font-family:monospace;">
					</div>
					<input class="btn btn-primary mt-3" data-test-id="create-dataset-btn" type="submit" onclick="createDataset(event)"/>
				</form>
			</div>

			<div id="all_datasets" class="mt-1">
				<h3>All Datasets</h3>
				<@table.table columns=columns items=c?sort_by("name") link="${ctx.adminContextPath?url_path}/admin-ui/datasets/" deleteButton=deleteDatasetButton cypressId="datasets"/>
			</div>

		</div>
	</div>
</@layout.layout>