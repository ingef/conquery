package com.bakdata.conquery.resources.admin.ui.model;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.resources.ResourceConstants;
import freemarker.template.TemplateModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UIContext {

	private static final TemplateModel STATIC_URI_ELEMENTS = ResourceConstants.getAsTemplateModel();

	private final Supplier<Collection<ShardNodeInformation>> shardNodeSupplier;

	@Getter
	public final TemplateModel staticUriElem = STATIC_URI_ELEMENTS;

	public Map<SocketAddress, ShardNodeInformation> getShardNodes() {
		return shardNodeSupplier.get().stream().collect(Collectors.toMap(
				ShardNodeInformation::getRemoteAddress,
				Function.identity()
		));
	}
}
