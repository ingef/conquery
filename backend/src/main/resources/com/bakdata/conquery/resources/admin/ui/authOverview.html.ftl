<#import "templates/template.html.ftl" as layout>
<#import "templates/overviewSnippet.html.ftl" as snippet>
<@layout.layout>
<h2>Auth Overview</h2>
<div class="container">
	<div class="row pt-3">
		<div class="col">
            <div>
                <a class="btn btn-primary" href="/admin/${ctx.staticUriElem.AUTH_OVERVIEW_PATH_ELEMENT}/csv" download>Download as CSV</a>
            </div>
		</div>
	</div>
	<div class="row pt-3">
		<div class="col">
            <table class="table table-striped">
                <thead>
                    <tr>
                    <th scope="col">User</th>
                    <th scope="col">Groups</th>
                    <th scope="col">Effective Roles</th>
                    </tr>
                </thead>
                <tbody>
                    <#list c.overview as row>
                        <tr>
                            <td>
                                <@snippet.snippet permissionOwner=row.user linkBase="./${ctx.staticUriElem.USERS_PATH_ELEMENT}/" />
                            <td>
                                <ul class="list-inline">
                                <#list row.groups as group>
                                    <li class="list-inline-item"><@snippet.snippet permissionOwner=group linkBase="./${ctx.staticUriElem.GROUPS_PATH_ELEMENT}/" /></li>
                                 </#list>
                                </ul>
                            </td>
                            <td>
                                <#list row.effectiveRoles as role>
                                    <li class="list-inline-item"><@snippet.snippet permissionOwner=role linkBase="./${ctx.staticUriElem.ROLES_PATH_ELEMENT}/" /></li>
                                 </#list>
                            </td>
                        </tr>
                    </#list>
                </tbody>
            </table>
	    </div>
	</div>
</div>
</@layout.layout>