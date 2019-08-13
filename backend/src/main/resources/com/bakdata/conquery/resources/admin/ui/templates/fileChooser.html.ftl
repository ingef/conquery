<#import "template.html.ftl" as t>
<div>
	<#list files as file>
	<span style="padding-left:${file.depth*20}px;">
		<#if file.file>
			<@linkCreator>${file.relativePath}</@linkCreator>${file.name}</a> (${t.si(file.f.length())}B)
		<#else>
			<i class="fas fa-folder"></i> ${file.name}
		</#if>
	</span><br/>
	</#list>
</div>