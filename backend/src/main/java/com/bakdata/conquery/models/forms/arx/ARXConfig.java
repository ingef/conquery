package com.bakdata.conquery.models.forms.arx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.PluginConfig;
import com.bakdata.conquery.models.forms.arx.models.KAnonymity;
import com.bakdata.conquery.models.forms.arx.models.PopulationUniqueness;
import com.bakdata.conquery.models.forms.arx.models.PrivacyModel;
import com.bakdata.conquery.models.forms.frontendconfiguration.FormFrontendConfigInformation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableCollection;
import io.dropwizard.views.View;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.deidentifier.arx.risk.RiskModelPopulationUniqueness;

@CPSType(id = "ARX", base = PluginConfig.class)
@Data
@Slf4j
public class ARXConfig implements PluginConfig {

	private TreeMap<String, PrivacyModel> privacyModels = new TreeMap<>(Map.of(
			"K_ANONYMITY_5", KAnonymity.builder()
									   .localizedLabels(
											   Map.of(
													   "de", "K-Anonymität 5",
													   "en", "K-Anonymity 5"
											   ))
									   .k(5).build(),
			"K_ANONYMITY_11", KAnonymity.builder()
										.localizedLabels(
												Map.of(
														"de", "K-Anonymität 11",
														"en", "K-Anonymity 11"
												))
										.k(11).build(),
			"PITMAN_1_PERCENT", PopulationUniqueness.builder()
													.localizedLabels(Map.of(
															"de", "Pitman 1%",
															"en", "Pitman 1%"
													))
													.populationUniquenessModel(RiskModelPopulationUniqueness.PopulationUniquenessModel.PITMAN)
													.build()
	));

	@Override
	public void initialize(ManagerNode managerNode) {
		PluginConfig.super.initialize(managerNode);

		managerNode.getFormScanner().registerFrontendFormConfigProvider(this::renderFormFrontendConfig);
	}

	public void renderFormFrontendConfig(ImmutableCollection.Builder<FormFrontendConfigInformation> formConfigInfos) {
		ResourceList frontendConfigs = CPSTypeIdResolver.SCAN_RESULT
				.getResourcesMatchingPattern(Pattern.compile(".*/arx_form\\.frontend_conf\\.json\\.ftl"));

		for (Resource config : frontendConfigs) {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final String templatePath = config.getPath();
			try (config) {

				// We prepend / to the path so freemarker does not prepend the package of this class
				Freemarker.HTML_RENDERER.render(new FrontendConfigView("/" + templatePath, getPrivacyModels()), Locale.ROOT, baos);

				JsonNode configTree = Jackson.MAPPER.reader().readTree(new ByteArrayInputStream(baos.toByteArray()));
				if (!configTree.isObject()) {
					log.warn("Expected '{}' to be an JSON object but was '{}'. Skipping registration.", templatePath, configTree.getNodeType());
				}
				formConfigInfos.add(new FormFrontendConfigInformation("Template " + templatePath, (ObjectNode) configTree));
			}
			catch (IOException e) {
				throw new IllegalArgumentException(String.format("Could not parse the frontend config: %s", templatePath), e);
			}
		}

	}

	@Getter
	public static class FrontendConfigView extends View {

		private final Map<String, PrivacyModel> privacyModels;

		protected FrontendConfigView(String templateName, Map<String, PrivacyModel> privacyModels) {
			super(templateName, StandardCharsets.UTF_8);
			this.privacyModels = privacyModels;
		}
	}
}
