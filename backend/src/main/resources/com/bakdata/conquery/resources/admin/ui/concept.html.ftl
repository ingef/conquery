<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<@layout.kid k="Name" v=c.id/>
	<@layout.kv k="Label" v=c.label/>
	<@layout.kv k="Type" v=c.class.simpleName/>
	<@layout.kv k="Structure Parent" v=c.structureParent/>
	<@layout.kv k="Elements" v=c.countElements()/>
	<@layout.kc k="Connectors">
	<#list c.connectors as connector>
		<@layout.kid k="Name" v=connector.id/>
		<@layout.kv k="Label" v=connector.label/>
		<@layout.kv k="Validity Dates" v=connector.validityDates?join(', ')/>
		<@layout.kv k="Table" v=connector.table/>
		<@layout.kc k="Filters">
			<#list connector.collectAllFilters() as filter>
				<@layout.kid k="Name" v=filter.id/>
				<@layout.kv k="Label" v=filter.label/>
				<@layout.kv k="Type" v=filter.class.simpleName/>
				<@layout.kv k="Unit" v=filter.unit/>
				<@layout.kv k="Description" v=filter.description/>
			</#list>
		</@layout.kc>
		<@layout.kc k="Selects">
			<#list connector.selects as select>
				<@layout.kid k="Name" v=select.id/>
				<@layout.kv k="Label" v=select.label/>
				<@layout.kv k="Type" v=select.class.simpleName/>
				<@layout.kv k="Description" v=select.description/>
			</#list>
		</@layout.kc>
	</#list>
	</@layout.kc>
</@layout.layout>
