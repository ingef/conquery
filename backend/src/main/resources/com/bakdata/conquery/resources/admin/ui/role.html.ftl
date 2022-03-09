<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<#import "templates/permissionCreator.html.ftl" as permissionCreator>

<@layout.layout>
<div class="container">
	<div class="row">
		<div class="col">
		<h2>Role ${c.owner.label}</h2>
		<small class="text-muted">${c.owner.id}</small>
		</div>
	</div>
	<div class="row pt-3">
		<div class="col">

			<ul class="nav nav-tabs" id="myTab" role="tablist">
				<li class="nav-item">
					<a class="nav-link active" id="ownedPermissions-tab" data-toggle="tab"
						href="#ownedPermissions" role="tab" aria-controls="ownedPermissions"
						aria-selected="true">Owned Permissions</a>
				</li>
				<li class="nav-item">
					<a class="nav-link" id="createPermission-tab" data-toggle="tab" href="#createPermission"
						role="tab" aria-controls="createPermission" aria-selected="false">Create
						Permission</a>
				</li>
				<li class="nav-item">
					<a class="nav-link" id="owners-tab" data-toggle="tab" href="#owners" role="tab"
						aria-controls="owners" aria-selected="false">Role Owner</a>
				</li>
			</ul>
			<div class="tab-content" id="myTabContent">
				<div class="tab-pane fade show active" id="ownedPermissions" role="tabpanel"
					aria-labelledby="ownedPermissions-tab">
					<@permissionTable.permissionTable ownerId=c.owner.getId() permissions=c.permissions />
				</div>
				<div class="tab-pane fade" id="createPermission" role="tabpanel"
					aria-labelledby="createPermission-tab">
					<@permissionCreator.permissionCreator ownerId=c.owner.getId()
						permissionTemplateMap=c.permissionTemplateMap />
				</div>
				<div class="tab-pane fade" id="owners" role="tabpanel" aria-labelledby="owners-tab">

					<div class="row">
						<div class="col">
							<table class="table table-striped">
								<thead>
									<tr>
										<th scope="col">User</th>
									</tr>
								</thead>
								<tbody>
									<#list c.users as user>
										<tr>
											<td>
												<a href="/admin/users/${user.id}">${user.label}</a>
											</td>
										</tr>
									</#list>
								</tbody>
							</table>
						</div>

						<div class="col">
							<table class="table table-striped">
								<thead>
									<tr>
										<th scope="col">Groups</th>
									</tr>
								</thead>
								<tbody>
									<#list c.groups as group>
										<tr>
											<td>
												<a href="/admin/groups/${group.id}">${group.label}</a>
											</td>
										</tr>
									</#list>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
</@layout.layout>