<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<ul>
			<#list c as role>
				<li>
					<a href="/admin/roles/${role.id}">${role.label}</a> 
					<a href="" onclick="event.preventDefault(); fetch('./roles/${role.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
			<form>
				<div class="form-group">
					<h3>Create Role</h3>
				  <label for="role_name">Name:</label>
				  <input id="role_name" name="role_name" class="form-control text-monospace" style="font-family:monospace;">
				  <label for="role_id">ID:</label>
				  <input id="role_id" name="role_id"  class="form-control text-monospace" style="font-family:monospace;">
				  <input class="btn btn-primary" type="submit" onclick="createRole()"/>
				</div> 
			</form>

			<button class="btn btn-primary" onclick="downloadRoles()"> Download Roles </button>
		</div>
	</div>
	<script type="application/javascript">
	function createRole() {
		event.preventDefault(); 
		fetch('./roles/',
		{
			method: 'post',
			headers: {'Content-Type': 'application/json'},
			body: JSON.stringify({
					name: document.getElementById('role_id').value,
					label: document.getElementById('role_name').value
				})
		}).then(function(){location.reload()});
	}

	function downloadRoles() {
		event.preventDefault(); 
		fetch('./roles/',
		{
			method: 'get',
			headers: {'Accept': 'application/json'}
		})
		.then(response => {return response.json()})
		.then(json => {
			console.log(json);
			uriContent = "data:application/octet-stream," + encodeURIComponent(JSON.stringify(json));
			newWindow = window.open(uriContent, 'Roles');
			});
	}
	
	</script>
</@layout.layout>