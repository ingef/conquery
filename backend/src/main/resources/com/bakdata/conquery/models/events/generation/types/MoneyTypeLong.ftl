<#macro nullValue type>
	<#import "/com/bakdata/conquery/models/events/generation/types/IntegerTypeLong.ftl" as copy/>
	<@copy.nullValue type=type/>
</#macro>
<#macro kryoSerialization type>
	<#import "/com/bakdata/conquery/models/events/generation/types/IntegerTypeLong.ftl" as copy/>
	<@copy.kryoSerialization type=type><#nested/></@copy.kryoSerialization>
</#macro>
<#macro kryoDeserialization type>
	<#import "/com/bakdata/conquery/models/events/generation/types/IntegerTypeLong.ftl" as copy/>
	<@copy.kryoDeserialization type=type/>
</#macro>
<#macro nullCheck type>
	<#import "/com/bakdata/conquery/models/events/generation/types/IntegerTypeLong.ftl" as copy/>
	<@copy.nullCheck type=type><#nested/></@copy.nullCheck>
</#macro>
<#macro majorTypeTransformation type><#nested></#macro>