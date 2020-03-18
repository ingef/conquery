<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<@layout.kid k="ID" v=c.id/>
	<@layout.kv k="Label" v=c.label/>
	<@layout.kv k="Type" v=c.class.simpleName/>
	<@layout.kv k="Structure Parent" v=c.structureParent/>
	<@layout.kv k="Elements" v=c.countElements()?string.number/>
	<@layout.kc k="Selects">
			<ul>
			<#list c.selects as select>
				<li>
					<@layout.kid k="ID" v=select.id/>
					<@layout.kv k="Label" v=select.label/>
					<@layout.kv k="Type" v=select.class.simpleName/>
					<@layout.kv k="Description" v=select.description/>
				</li>
			</#list>
			</ul>
		</@layout.kc>
	<@layout.kc k="Connectors">
	<#list c.connectors as connector>
		<@layout.kid k="ID" v=connector.id/>
		<@layout.kv k="Label" v=connector.label/>
		<@layout.kv k="Validity Dates" v=connector.validityDates?join(', ')/>
		<@layout.kv k="Table" v=connector.table/>
		<@layout.kc k="Filters">
			<ul>
			<#list connector.collectAllFilters() as filter>
				<li>
					<@layout.kid k="ID" v=filter.id/>
					<@layout.kv k="Label" v=filter.label/>
					<@layout.kv k="Type" v=filter.class.simpleName/>
					<@layout.kv k="Unit" v=filter.unit/>
					<@layout.kv k="Description" v=filter.description/>
				</li>
			</#list>
			</ul>
		</@layout.kc>
		<@layout.kc k="Selects">
			<ul>
			<#list connector.selects as select>
				<li>
					<@layout.kid k="ID" v=select.id/>
					<@layout.kv k="Label" v=select.label/>
					<@layout.kv k="Type" v=select.class.simpleName/>
					<@layout.kv k="Description" v=select.description/>
				</li>
			</#list>
			</ul>
		</@layout.kc>
	</#list>
	</@layout.kc>
</@layout.layout>
