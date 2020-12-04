<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<@layout.kid k="ID" v=c.id/>
	<@layout.kv k="Entries" v=c.numberOfEntries?string.number/>
	<@layout.kv k="CodeGen Suffix" v=c.suffix/>
	<@layout.kv k="Size" v=layout.si(c.estimateMemoryConsumption())+"B"/>
	<@layout.kc k="Columns">
		<ul>
		<#list c.columns as column>
			<li>
			<@layout.kid k="ID" v=column.id/>
			<@layout.kv k="Size" v=layout.si(column.typeDescription.estimateMemoryConsumption())+"B"/>
			<@layout.kc k="type">${column.typeDescription}</@layout.kc>
			</li>
		</#list>
		</ul>
	</@layout.kc>
</@layout.layout>