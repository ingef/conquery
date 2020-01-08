<#import "templates/template.html.ftl" as layout>
<#import "templates/authEntityOverview.html.ftl" as entityOverview>
<@layout.layout>
	<h1>Roles</h1>
	<@entityOverview.entityOverview pathBase="./${ctx.staticUriElem.ROLES_PATH_ELEMENT}/" entities=c entityName="Role" />
</@layout.layout>