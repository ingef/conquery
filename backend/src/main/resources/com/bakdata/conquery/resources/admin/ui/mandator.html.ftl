<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<h3>Permissions</h3>
		<div class="col">
			<ul>
			<table class="table table-striped">
				<thead>
					<tr>
					<th scope="col">Type</th>
					<th scope="col">Target</th>
					<th scope="col">Abilities</th>
					</tr>
				</thead>
				<tbody>
					<#list c.permissions as permission>
						<tr>
							<td>${permission.getClass().getSimpleName()}</td>
							<td>${permission.getTarget().toString()}</td>
							<td><#list permission.getAbilities() as ability>${ability} </#list></td>
						</tr>
					</#list>
				</tbody>
			</table>
			</ul>
		</div>
	</div>
	<div class="row">
		<h3>Users</h3>
		<div class="col">
			<ul>
			<#list c.users as user>
				<li>
					<a href="/admin/user/${user.id}">${user.label}</a> 
					<a href="" onclick="event.preventDefault(); fetch('./user/${user.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
		</div>
	</div>
</@layout.layout>