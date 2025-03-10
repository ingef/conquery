<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<#import "templates/permissionCreator.html.ftl" as permissionCreator>
<#import "templates/roleHandler.html.ftl" as roleHandler>
<#import "templates/groupHandler.html.ftl" as groupHandler>
<@layout.layout>
<div class="container">
	<div class="row">
		<div class="col">
		<h2>User ${c.label}</h2>
		<small class="text-muted">${c.id}</small>
		</div>
	</div>
	<div class="row pt-3">
		<div class="col">

            <ul class="nav nav-tabs" id="myTab" role="tablist">
                <li class="nav-item">
                    <a class="nav-link active" id="ownedPermissions-tab" data-toggle="tab" href="#ownedPermissions" role="tab" aria-controls="ownedPermissions" aria-selected="true">Owned Permissions</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" id="createPermission-tab" data-toggle="tab" href="#createPermission" role="tab" aria-controls="createPermission" aria-selected="false">Create Permission</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" id="groups-tab" data-toggle="tab" href="#groups" role="tab" aria-controls="groups" aria-selected="false">Groups</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" id="roles-tab" data-toggle="tab" href="#roles" role="tab" aria-controls="roles" aria-selected="false">Roles</a>
                </li>
            </ul>
            <div class="tab-content" id="myTabContent">
                <div class="tab-pane fade show active" id="ownedPermissions" role="tabpanel" aria-labelledby="ownedPermissions-tab">
                    <@permissionTable.permissionTable ownerId=c.id permissions=c.permissions />
                </div>
                <div class="tab-pane fade" id="createPermission" role="tabpanel" aria-labelledby="createPermission-tab">
                    <@permissionCreator.permissionCreator ownerId=c.id permissionTemplateMap=c.permissionTemplateMap />
                </div>
                <div class="tab-pane fade" id="groups" role="tabpanel" aria-labelledby="groups-tab">
                    <@groupHandler.groupHandler c=c adminPathBase="/admin/${ctx.staticUriElem.GROUPS_PATH_ELEMENT}" />
                </div>
                <div class="tab-pane fade" id="roles" role="tabpanel" aria-labelledby="roles-tab">
                    <@roleHandler.roleHandler c=c adminPathBase="/admin/${ctx.staticUriElem.USERS_PATH_ELEMENT}" />
                </div>
            </div>
        </div>
    </div>
</div>

</@layout.layout>