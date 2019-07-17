<#macro nullValue type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.subType.class.simpleName}.ftl" as sub/>
	<@sub.nullValue type=type.subType/>
</#macro>
<#macro kryoSerialization type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.subType.class.simpleName}.ftl" as sub/>
	<@sub.kryoSerialization type=type.subType><#nested/></@sub.kryoSerialization>
</#macro>
<#macro kryoDeserialization type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.subType.class.simpleName}.ftl" as sub/>
	<@sub.kryoDeserialization type=type.subType/>
</#macro>
<#macro nullCheck type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.subType.class.simpleName}.ftl" as sub/>
	<@sub.nullCheck type=type.subType><#nested/></@sub.nullCheck>
</#macro>
<#macro majorTypeTransformation type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.subType.class.simpleName}.ftl" as sub/>
	<@sub.majorTypeTransformation type=type.subType><#nested/></@sub.majorTypeTransformation>
</#macro>
<#macro unboxValue type>
	<#import "/com/bakdata/conquery/models/events/generation/types/${type.subType.class.simpleName}.ftl" as sub/>
	<@sub.unboxValue type=type.subType><#nested/></@sub.unboxValue>
</#macro>