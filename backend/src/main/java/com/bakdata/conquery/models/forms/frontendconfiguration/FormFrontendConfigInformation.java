package com.bakdata.conquery.models.forms.frontendconfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class FormFrontendConfigInformation {
    private String origin;
    /**
     * The actual frontend configuration for the form.
     */
    private JsonNode configTree;
}
