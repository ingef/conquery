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
		<@layout.kc k="Table">
		<a href="./${c.dataset.id}/tables/${connector.table.id}">${connector.table.name}</a>
        </@layout.kc>
		<@layout.kc k="Filters">
			<ul>
			<#list connector.collectAllFilters() as filter>
				<li>
					<@layout.kid k="ID" v=filter.id/>
					<@layout.kv k="Label" v=filter.label/>
					<@layout.kv k="Type" v=filter.class.simpleName/>
					<@layout.kv k="Columns" v=filter.requiredColumns?join(', ')/>
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
					<@layout.kv k="Columns" v=select.requiredColumns?join(', ')/>
				</li>
			</#list>
			</ul>
		</@layout.kc>
	</#list>
	</@layout.kc>
</@layout.layout>
