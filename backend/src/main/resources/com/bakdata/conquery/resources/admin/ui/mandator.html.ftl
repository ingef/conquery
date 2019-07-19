<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<@layout.layout>
	<div class="row">
		<div class="col">
		<h2>${c.self.label}</h2>
		<h3>Dataset Permissions</h3>
		<@permissionTable.permissionTable permissions=c.datasetPermissions/>
		
		<form method="POST" enctype="multipart/form-data">
			<h4>New Dataset Permission</h4>
			<div class="form-row">
  				<div class="form-group col">
					<label for="permissionowner_id">Owner:</label>
					<input type="text" id="permissionowner_id"  class="form-control" name="permissionowner_id" value="${c.self.id}" readonly>
				</div>
  				<div class="form-group col">
				  <label for="abilities">Abilities:</label>
					<#list c.abilities as ability>
						<div class="form-check">
							<input class="form-check-input" type="checkbox" name="abilities" value="${ability}" id="abilitySet">
							<label class="form-check-label" for="abilities">
								${ability}
							</label>
						</div>
					</#list>
				</div>
  				<div class="form-group col">
					<label for="dataset_id">Dataset:</label>
					<select class="form-control" id="dataset_id" name="dataset_id">
						<#list c.datasets as dataset>
							<option value="${dataset.id}">${dataset.label}</option>
						</#list>
					</select>
				</div>
				<input class="btn btn-primary" type="submit" onclick="event.preventDefault(); fetch('/admin/permissions', {method: 'post', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({type: 'DATASET_PERMISSION', ownerId: document.getElementById('permissionowner_id').value, abilities: document.getElementById('abilitySet').value, instanceId:document.getElementById('dataset_id').value})}).then(function(){location.reload()})"/>
			</div> 
		</form>
		<h3>Query Permissions</h3>
		<@permissionTable.permissionTable permissions=c.queryPermissions/>
		<h3>Other Permissions</h3>
		<@permissionTable.permissionTable permissions=c.otherPermissions/>
		</div>
	</div>
	<div class="row">
		<h3>Users</h3>
		<div class="col">
			<ul>
			<#list c.users as user>
				<li>
					${user.label}
				</li>
			</#list>
			</ul>
		</div>
	</div>
</@layout.layout>