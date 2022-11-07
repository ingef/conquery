{
    "title": {
        "en": "ARX Anonymization",
        "de": "ARX Anonymisierung"
    },
    "description": {
		"en": "With this form, the actual result of the provided query is anonymized using ARX.",
		"de": "Mit diesem Formular wird das eigentliche Ergebnis der übergebenen Anfrage mittels ARX anonymisiert."
	},
    "type": "ARX_FORM",
    "fields": [
        {
            "name": "queryGroup",
            "type": "RESULT_GROUP",
            "label": {
                "de": "Versichertengruppe",
                "en": "Group"
            },
            "dropzoneLabel": {
                "en": "Drop an existing query here.",
                "de": "Füge eine Versichertengruppe aus einer bestehenden Anfrage hinzu"
            },
            "validations": [
                "NOT_EMPTY"
            ],
            "tooltip": {
                "en": "Group, whose result is anonymized.",
                "de": "Versichertengruppe (Anfrage), deren Ergebnis anonymisiert wird."
            }
        },
        {
            "name": "suppressionLimit",
            "type": "NUMBER",
            "defaultValue": 0.02,
            "min": 0,
            "max": 1,
			"label": {
				"en": "Suppression Limit",
				"de": "Unterdrückungslimit"
			},
			"pattern": "^\\d+.?\\d*$",
			"validations": [
				"NOT_EMPTY",
				"GREATER_THAN_ZERO"
			]
		},
		{
			"name": "privacyModel",
			"type": "SELECT",
			"label": {
				"en": "Privacy Model",
				"de": "Datenschutz Modell"
			},
			"defaultValue": "K_ANONYMITY_11",
			"options": [
                <#list privacyModels as modelId, model>
                    {
                        "label": {
                            <#list model.localizedLabels as locale, label>
                            "${locale}": "${label}"
                            <#sep>,</#sep>
                            </#list>
                        },
                        "value": "${modelId}"
                    }
                    <#sep>,</#sep>
                </#list>
			],
			"validations": [
				"NOT_EMPTY"
			],
			"tooltip": {
			}
		}
	]
}