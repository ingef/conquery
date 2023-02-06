<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<@layout.kid k="ID" v=c.imp.id/>
	<@layout.kv k="Entries" v=c.imp.numberOfEntries?string.number/>
	<@layout.kv k="Size" v=layout.si(c.imp.estimateMemoryConsumption())+"B"/>
	<@layout.kv k="CBlocksSize" v=layout.si(c.getCBlocksMemoryBytes())+"B"/>
	<@layout.kc k="Columns">
		<ul>
		<#list c.imp.columns as column>
			<li>
			<@layout.kid k="ID" v=column.id/>
			<@layout.kv k="Size" v=layout.si(column.getMemorySizeBytes())+"B"/>
			<@layout.kc k="type">${column.typeDescription}</@layout.kc>
			</li>
		</#list>
		</ul>
	</@layout.kc>
</@layout.layout>