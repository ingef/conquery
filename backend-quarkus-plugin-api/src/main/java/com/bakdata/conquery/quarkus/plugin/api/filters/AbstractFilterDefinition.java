package com.bakdata.conquery.quarkus.plugin.api.filters;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractFilterDefinition implements FilterDefinition {

	@Schema(description = "Filter implementation discriminator.")
	private String type;
	@Schema(description = "Stable local name used to form the filter id. Falls back to the label when omitted.", pattern = "^\\w+$")
	private String name;
	@Schema(description = "Label displayed to users.")
	private String label;
	@JsonAlias("description")
	@Schema(description = "Additional explanation displayed for the filter.")
	private String tooltip;
	@Schema(description = "Unit displayed with filter values.")
	private String unit;
	@Schema(description = "Optional input validation pattern.")
	private String pattern;
	@Schema(description = "Whether values may be supplied using a dropped file.")
	private Boolean allowDropFile;
	@Schema(description = "Default frontend value.")
	private Object defaultValue;
	@Schema(description = "Optional minimum frontend value.")
	private Integer min;
	@Schema(description = "Optional maximum frontend value.")
	private Integer max;

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getLabel() { return label; }
	public void setLabel(String label) { this.label = label; }
	public String getTooltip() { return tooltip; }
	public void setTooltip(String tooltip) { this.tooltip = tooltip; }
	public String getUnit() { return unit; }
	public void setUnit(String unit) { this.unit = unit; }
	public String getPattern() { return pattern; }
	public void setPattern(String pattern) { this.pattern = pattern; }
	public Boolean getAllowDropFile() { return allowDropFile; }
	public void setAllowDropFile(Boolean allowDropFile) { this.allowDropFile = allowDropFile; }
	public Object getDefaultValue() { return defaultValue; }
	public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
	public Integer getMin() { return min; }
	public void setMin(Integer min) { this.min = min; }
	public Integer getMax() { return max; }
	public void setMax(Integer max) { this.max = max; }
}
