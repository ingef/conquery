<#macro nullValue type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.numberType.class.simpleName}.ftl" as sub/>
	<@sub.nullValue type=type.numberType/>
</#macro>
<#macro kryoSerialization type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.numberType.class.simpleName}.ftl" as sub/>
	<@sub.kryoSerialization type=type.numberType><#nested/></@sub.kryoSerialization>
</#macro>
<#macro kryoDeserialization type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.numberType.class.simpleName}.ftl" as sub/>
	<@sub.kryoDeserialization type=type.numberType/>
</#macro>
<#macro nullCheck type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.numberType.class.simpleName}.ftl" as sub/>
	<@sub.nullCheck type=type.numberType><#nested/></@sub.nullCheck>
</#macro>
<#macro majorTypeTransformation type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.numberType.class.simpleName}.ftl" as sub/>
	<@sub.majorTypeTransformation type=type.numberType><#nested/></@sub.majorTypeTransformation>
</#macro>
<#macro unboxValue type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.numberType.class.simpleName}.ftl" as sub/>
	<@sub.unboxValue type=type.numberType><#nested/></@sub.unboxValue>
</#macro>