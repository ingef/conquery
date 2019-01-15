<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">Name</div>
		<div class="col-7">${c.id}</div>
	<div class="w-100"></div>
		<div class="col">Label</div>
		<div class="col-7">${c.label}</div>
	<div class="w-100"></div>
		<div class="col">Type</div>
		<div class="col-7">${c.class.simpleName}</div>
	<div class="w-100"></div>
		<div class="col">Structure Parent</div>
		<div class="col-7">${c.structureParent!}</div>
	<div class="w-100"></div>
		<div class="col">Connectors</div>
		<div class="col-7">
			<#list c.connectors as connector>
				<div class="row">
					<div class="col">Name</div>
					<div class="col-7">${connector.id}</div>
				<div class="w-100"></div>
					<div class="col">Label</div>
					<div class="col-7">${connector.label}</div>
				<div class="w-100"></div>
					<div class="col">Validity Dates</div>
					<div class="col-7">[ ${connector.validityDates?join(', ')} ]</div>
				<div class="w-100"></div>
					<div class="col">Table</div>
					<div class="col-7">${connector.table}</div>
				<div class="w-100"></div>
					<div class="col">Filters</div>
					<div class="col-7">
						<#list connector.allFilters as id, filter>
							<div class="row">
								<div class="col">Name</div>
								<div class="col-7">${filter.id}</div>
							<div class="w-100"></div>
								<div class="col">Label</div>
								<div class="col-7">${filter.label}</div>
							<div class="w-100"></div>
								<div class="col">Type</div>
								<div class="col-7">${filter.class.simpleName}</div>
							<div class="w-100"></div>
								<div class="col">Unit</div>
								<div class="col-7">${filter.unit!}</div>
							<div class="w-100"></div>
								<div class="col">Description</div>
								<div class="col-7">${filter.description!}</div>
							</div>
							<hr>
						</#list>
					</div>
				<hr/>
			</#list>
		</div>
	</div>
</@layout.layout>