<#import "templates/template.html.ftl" as layout>
<#import "templates/authEntityOverview.html.ftl" as entityOverview>
<@layout.layout>
	<h1>Groups</h1>
	<@entityOverview.entityOverview pathBase="./groups/" entities=c entityName="Group" />
</@layout.layout>