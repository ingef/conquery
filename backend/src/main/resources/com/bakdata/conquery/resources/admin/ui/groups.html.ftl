<#import "templates/template.html.ftl" as layout>
<#import "templates/authEntityOverview.html.ftl" as entityOverview>
<@layout.layout>
	<h1>Groups</h1>
	<@entityOverview.entityOverview uiPathBase="./${ctx.staticUriElem.GROUPS_PATH_ELEMENT}" adminPathBase="/admin/${ctx.staticUriElem.GROUPS_PATH_ELEMENT}" entities=c entityName="Group" />
</@layout.layout>