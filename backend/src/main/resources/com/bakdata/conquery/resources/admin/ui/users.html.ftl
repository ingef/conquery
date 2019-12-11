<#import "templates/template.html.ftl" as layout>
<#import "templates/authEntityOverview.html.ftl" as entityOverview>
<@layout.layout>
	<h1>Users</h1>
	<@entityOverview.entityOverview pathBase="./${ctx.staticUriElem.USERS_PATH_ELEMENT}/" entities=c entityName="User" />
</@layout.layout>