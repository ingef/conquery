<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<@layout.kv k="Name" v=c.id/>
		<@layout.kv k="Label" v=c.label/>
		<@layout.kv k="Type" v=c.class.simpleName/>
		<@layout.kv k="Structure Parent" v=c.structureParent!/>
		<@layout.kv k="Elements" v=c.countElements()/>
		<div class="col">Connectors</div>
		<div class="col-7">
			<#list c.connectors as connector>
				<@layout.kv k="Name" v=connector.id/>
					<@layout.kv k="Label" v=connector.label/>
					<div class="col">Validity Dates</div>
					<div class="col-7">[ ${connector.validityDates?join(', ')} ]</div>
					<@layout.kv k="Table" v=connector.table/>
					<div class="col">Filters</div>
					<div class="col-7">
						<#list connector.collectAllFilters() as filter>
							<div class="row">
								<@layout.kv k="Name" v=filter.id/>
								<@layout.kv k="Label" v=filter.label/>
								<@layout.kv k="Type" v=filter.class.simpleName/>
								<@layout.kv k="Unit" v=filter.unit!/>
								<@layout.kv k="Description" v=filter.description!/>
							</div>
							<hr>
						</#list>
					</div>
					<div class="col">Selects</div>
					<div class="col-7">
						<#list (connector.collectAllSelects())![] as select>
							<div class="row">
								<@layout.kv k="Name" v=select.id/>
								<@layout.kv k="Label" v=select.label/>
								<@layout.kv k="Type" v=select.class.simpleName/>
								<@layout.kv k="Description" v=filter.description!/>
							</div>
							<hr>
						</#list>
					</div>
				<hr/>
			</#list>
		</div>
	</div>
</@layout.layout>
