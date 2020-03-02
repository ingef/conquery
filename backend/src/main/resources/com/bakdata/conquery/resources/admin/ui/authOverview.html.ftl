<#import "templates/template.html.ftl" as layout>
<#import "templates/overviewSnippet.html.ftl" as snippet>
<@layout.layout>
	<div class="row">
		<div class="col">
		<h2>Auth Overview</h2>
        <div>
        <button type="button" class="btn btn-primary" id="btn-download-csv">Download as CSV</button>
        </div>
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
    <script type="application/javascript">
            $('[data-toggle="tooltip"]').tooltip()
            $('[data-toggle="popover"]').popover({ trigger: 'hover'})
            $('.popover-dismiss').popover({ trigger: 'focus'})

            document.querySelector('#btn-download-csv').addEventListener('click', downloadCSV);
 
            function downloadCSV () {
                let fileName = "authOverview.csv";
                fetch("/admin/${ctx.staticUriElem.AUTH_OVERVIEW_PATH_ELEMENT}",{
                    method: 'GET',
                    cache: 'no-cache',
                    headers: {
                    'Accept': 'text/csv; charset=utf-8'
                    }
                }).then( (response) => {
                    if(!response.ok) {
                        throw new Error("Could not download overview as CSV.");
                    } 
                    return response.blob();

                }).then((blob) => {
                    let a = document.createElement("a");
                    document.body.appendChild(a);
                    a.style = "display: none";

                    let url = window.URL.createObjectURL(blob);
                    a.href = url;
                    a.download = fileName;
                    a.click()
                    return a;
                }).then((link) => {
                    window.URL.revokeObjectURL(link.href);

                }).catch((error) => {
                    alert(error);
                })
            }
    </script>
</@layout.layout>