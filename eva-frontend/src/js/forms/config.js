// @flow

import type { Forms } from "./config-types";

const config: Forms = [
  {
    title: {
      de: "AU Bericht"
    },
    type: "AU_FORM",
    headline: {
      de: "AU Bericht"
    },
    fields: [
      {
        name: "title",
        type: "STRING",
        defaultValue: "Titel",
        label: {
          de: "Titel"
        },
        placeholder: {
          de: "Titel"
        }
      },
      {
        name: "description",
        type: "STRING",
        defaultValue: "Titel",
        label: {
          de: "Beschreibung"
        },
        placeholder: {
          de: "Beschreibung"
        },
        style: {
          fullWidth: true
        }
      },
      {
        name: "queryGroup",
        type: "RESULT_GROUP",
        label: {
          de: "Versichertengruppe"
        },
        dropzoneLabel: {
          de: "Füge eine Versichertengruppe aus einer bestehenden Anfrage hinzu"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "dateRange",
        type: "DATE_RANGE",
        label: {
          de: "Beobachtungszeitraum"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "baseCondition",
        type: "CONCEPT_LIST",
        label: {
          de: "Grundbedingung"
        },
        isTwoDimensional: false,
        conceptDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        },
        conceptColumnDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        }
        // disallowedConceptIds?: string[], // Matching the ID string, lowercased
        // validations?: ConceptListFieldValidation[]
      }
    ]
  },
  {
    title: {
      de: "Deskriptives Formular"
    },
    type: "DESCRIPTION_FORM",
    headline: {
      de: "Deskriptives Formular"
    },
    fields: [
      {
        name: "title",
        type: "STRING",
        defaultValue: "Titel",
        label: {
          de: "Titel"
        },
        placeholder: {
          de: "Titel"
        }
      },
      {
        name: "description",
        type: "STRING",
        defaultValue: "Beschreibung",
        label: {
          de: "Beschreibung"
        },
        placeholder: {
          de: "Beschreibung"
        },
        style: {
          fullWidth: true
        }
      },
      {
        name: "queryGroup",
        type: "RESULT_GROUP",
        label: {
          de: "Versichertengruppe"
        },
        dropzoneLabel: {
          de: "Füge eine Versichertengruppe aus einer bestehenden Anfrage hinzu"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "timeMode",
        type: "TAB",
        defaultValue: "RELATIVE",
        tabs: [
          {
            name: "ABSOLUTE",
            title: {
              de: "Absolut"
            },
            fields: [
              {
                name: "resolution",
                type: "SELECT",
                label: {
                  de: "Stratifizierung Beobachtungszeitraum"
                },
                defaultValue: "COMPLETE_ONLY",
                options: [
                  {
                    label: {
                      de: "Gesamter Zeitraum"
                    },
                    value: "COMPLETE_ONLY"
                  },
                  {
                    label: {
                      de: "Quartale"
                    },
                    value: "QUARTER_WISE"
                  },
                  {
                    label: {
                      de: "Jahre"
                    },
                    value: "YEAR_WISE"
                  }
                ]
              },
              {
                name: "dateRange",
                type: "DATE_RANGE",
                label: {
                  de: "Beobachtungszeitraum"
                }
              },
              {
                name: "features",
                type: "CONCEPT_LIST",
                label: {
                  de: "Konzepte"
                },
                isTwoDimensional: true,
                conceptDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                conceptColumnDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                validations: ["NOT_EMPTY"]
              }
            ]
          },
          {
            name: "RELATIVE",
            title: {
              de: "Relativ"
            },
            fields: [
              {
                name: "resolution",
                type: "SELECT",
                label: {
                  de: "Stratifizierung Beobachtungszeitraum"
                },
                defaultValue: "COMPLETE_ONLY",
                options: [
                  {
                    label: {
                      de: "Gesamter Zeitraum"
                    },
                    value: "COMPLETE_ONLY"
                  },
                  {
                    label: {
                      de: "Quartale"
                    },
                    value: "QUARTER_WISE"
                  },
                  {
                    label: {
                      de: "Jahre"
                    },
                    value: "YEAR_WISE"
                  }
                ],
                validations: ["NOT_EMPTY"]
              },
              {
                name: "timeCountBefore",
                type: "NUMBER",
                defaultValue: 1,
                label: {
                  de: "Quartale davor"
                },
                placeholder: {
                  de: "0"
                },
                pattern: "^(?!-)\\\\d*$",
                validations: ["NOT_EMPTY", "GREATER_THAN_ZERO"]
              },
              {
                name: "timeCountAfter",
                type: "NUMBER",
                defaultValue: 1,
                label: {
                  de: "Quartale danach"
                },
                placeholder: {
                  de: "0"
                },
                pattern: "^(?!-)\\\\d*$",
                validations: ["NOT_EMPTY", "GREATER_THAN_ZERO"]
              },
              {
                name: "timestamp",
                type: "SELECT",
                label: {
                  de: "Zeitstempel Indexdatum"
                },
                defaultValue: "FIRST",
                options: [
                  {
                    label: {
                      de: "ERSTES"
                    },
                    value: "FIRST"
                  },
                  {
                    label: {
                      de: "LETZTES"
                    },
                    value: "LAST"
                  },
                  {
                    label: {
                      de: "ZUFÄLLIG"
                    },
                    value: "RANDOM"
                  }
                ],
                validations: ["NOT_EMPTY"]
              },
              {
                name: "indexDate",
                type: "SELECT",
                label: {
                  de: "Zugehörigkeit Indexdatum"
                },
                defaultValue: "BEFORE",
                options: [
                  {
                    label: {
                      de: "VORBEOBACHTUNGSZEITRAUM"
                    },
                    value: "BEFORE"
                  },
                  {
                    label: {
                      de: "NEUTRAL"
                    },
                    value: "NEUTRAL"
                  },
                  {
                    label: {
                      de: "NACHBEOBACHTUNGSZEITRAUM"
                    },
                    value: "AFTER"
                  }
                ],
                validations: ["NOT_EMPTY"]
              },
              {
                name: "features",
                type: "CONCEPT_LIST",
                label: {
                  de: "Konzepte im Vorbeobachtungszeitraum (Feature Zeitraum)"
                },
                isTwoDimensional: true,
                conceptDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                conceptColumnDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                validations: ["NOT_EMPTY"]
              },
              {
                name: "outcomes",
                type: "CONCEPT_LIST",
                label: {
                  de: "Konzepte im Nachbeobachtungszeitraum (Outcome Zeitraum)"
                },
                isTwoDimensional: true,
                conceptDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                conceptColumnDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                validations: ["NOT_EMPTY"]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    title: {
      de: "Exportformular"
    },
    type: "EXPORT_FORM",
    headline: {
      de: "Exportformular"
    },
    fields: [
      {
        name: "queryGroup",
        type: "RESULT_GROUP",
        label: {
          de: "Versichertengruppe"
        },
        dropzoneLabel:
          "Füge eine Versichertengruppe aus einer bestehenden Anfrage hinzu",
        validate: ["NOT_EMPTY"]
      },
      {
        name: "timeMode",
        type: "TAB",
        defaultValue: "RELATIVE",
        tabs: [
          {
            name: "ABSOLUTE",
            title: {
              de: "Absolut"
            },
            fields: [
              {
                name: "dateRange",
                type: "DATE_RANGE",
                label: {
                  de: "Beobachtungszeitraum"
                }
              },
              {
                name: "features",
                type: "CONCEPT_LIST",
                label: {
                  de: "Konzepte"
                },
                conceptDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                validations: ["NOT_EMPTY"]
              }
            ]
          },
          {
            name: "RELATIVE",
            title: {
              de: "Relativ"
            },
            fields: [
              {
                name: "resolution",
                type: "SELECT",
                label: {
                  de: "Stratifizierung Beobachtungszeitraum"
                },
                defaultValue: "COMPLETE_ONLY",
                options: [
                  {
                    label: {
                      de: "Gesamter Zeitraum"
                    },
                    value: "COMPLETE_ONLY"
                  },
                  {
                    label: {
                      de: "Quartale"
                    },
                    value: "QUARTER_WISE"
                  },
                  {
                    label: {
                      de: "Jahre"
                    },
                    value: "YEAR_WISE"
                  }
                ],
                validations: ["NOT_EMPTY"]
              },
              {
                name: "timeCountBefore",
                type: "NUMBER",
                defaultValue: 1,
                label: {
                  de: "Quartale davor"
                },
                placeholder: {
                  de: "0"
                },
                pattern: "^(?!-)\\\\d*$",
                validations: ["NOT_EMPTY", "GREATER_THAN_ZERO"]
              },
              {
                name: "timeCountAfter",
                type: "NUMBER",
                defaultValue: 1,
                label: {
                  de: "Quartale danach"
                },
                placeholder: {
                  de: "0"
                },
                pattern: "^(?!-)\\\\d*$",
                validations: ["NOT_EMPTY", "GREATER_THAN_ZERO"]
              },
              {
                name: "timestamp",
                type: "SELECT",
                label: {
                  de: "Zeitstempel Indexdatum"
                },
                defaultValue: "FIRST",
                options: [
                  {
                    label: {
                      de: "ERSTES"
                    },
                    value: "FIRST"
                  },
                  {
                    label: {
                      de: "LETZTES"
                    },
                    value: "LAST"
                  },
                  {
                    label: {
                      de: "ZUFÄLLIG"
                    },
                    value: "RANDOM"
                  }
                ],
                validations: ["NOT_EMPTY"]
              },
              {
                name: "indexDate",
                type: "SELECT",
                label: {
                  de: "Zugehörigkeit Indexdatum"
                },
                defaultValue: "BEFORE",
                options: [
                  {
                    label: {
                      de: "VORBEOBACHTUNGSZEITRAUM"
                    },
                    value: "BEFORE"
                  },
                  {
                    label: {
                      de: "NEUTRAL"
                    },
                    value: "NEUTRAL"
                  },
                  {
                    label: {
                      de: "NACHBEOBACHTUNGSZEITRAUM"
                    },
                    value: "AFTER"
                  }
                ],
                validations: ["NOT_EMPTY"]
              },
              {
                name: "features",
                type: "CONCEPT_LIST",
                label: {
                  de: "Konzepte im Vorbeobachtungszeitraum (Feature Zeitraum)"
                },
                isTwoDimensional: true,
                conceptDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                conceptColumnDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                validations: ["NOT_EMPTY"]
              },
              {
                name: "outcomes",
                type: "CONCEPT_LIST",
                label: {
                  de: "Konzepte im Nachbeobachtungszeitraum (Outcome Zeitraum)"
                },
                isTwoDimensional: true,
                conceptDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                conceptColumnDropzoneLabel: {
                  de: "Füge ein Konzept oder eine Konzeptliste hinzu"
                },
                validations: ["NOT_EMPTY"]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    title: {
      de: "PSM Anfrage"
    },
    type: "PSM_FORM",
    headline: {
      de: "PSM Anfrage"
    },
    fields: [
      {
        name: "title",
        type: "STRING",
        defaultValue: "Titel",
        label: {
          de: "Titel"
        },
        placeholder: {
          de: "Titel"
        }
      },
      {
        name: "description",
        type: "STRING",
        defaultValue: "Titel",
        label: {
          de: "Beschreibung"
        },
        placeholder: {
          de: "Beschreibung"
        },
        style: {
          fullWidth: true
        }
      },
      {
        name: "controlGroupTimestamp",
        type: "SELECT",
        label: {
          de: "Zeitstempel Indexdatum Vergleichsgruppe"
        },
        defaultValue: "FIRST",
        options: [
          {
            label: {
              de: "ERSTES"
            },
            value: "FIRST"
          },
          {
            label: {
              de: "LETZTES"
            },
            value: "LAST"
          },
          {
            label: {
              de: "ZUFÄLLIG"
            },
            value: "RANDOM"
          }
        ],
        validations: ["NOT_EMPTY"]
      },
      {
        name: "controlGroupDataset",
        type: "DATASET_SELECT",
        label: {
          de: "Datensatz für Vergleichsgruppe"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "controlGroup",
        type: "RESULT_GROUP",
        validations: ["NOT_EMPTY"],
        label: {
          de: "Vergleichsgruppe"
        },
        dropzoneLabel: {
          de: "Füge eine Versichertengruppe aus einer bestehenden Anfrage hinzu"
        }
      },
      {
        name: "featureGroupTimestamp",
        type: "SELECT",
        label: {
          de: "Zeitstempel Indexdatum Interventionsgruppe"
        },
        defaultValue: "FIRST",
        options: [
          {
            label: {
              de: "ERSTES"
            },
            value: "FIRST"
          },
          {
            label: {
              de: "LETZTES"
            },
            value: "LAST"
          },
          {
            label: {
              de: "ZUFÄLLIG"
            },
            value: "RANDOM"
          }
        ],
        validations: ["NOT_EMPTY"]
      },
      {
        name: "featureGroupDataset",
        type: "DATASET_SELECT",
        label: {
          de: "Datensatz für Interventionsgruppe"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "featureGroup",
        type: "RESULT_GROUP",
        validations: ["NOT_EMPTY"],
        label: {
          de: "Interventionsgruppe"
        },
        dropzoneLabel: {
          de: "Füge eine Versichertengruppe aus einer bestehenden Anfrage hinzu"
        }
      },
      {
        name: "timeUnit",
        type: "SELECT",
        label: {
          de: "Zeiteinheit des Vor- und Nachbeobachtungszeitraums"
        },
        defaultValue: "QUARTERS",
        options: [
          {
            label: {
              de: "Tage"
            },
            value: "DAYS"
          },
          {
            label: {
              de: "Quartale"
            },
            value: "QUARTERS"
          }
        ],
        validations: ["NOT_EMPTY"]
      },
      {
        name: "timeCountBefore",
        type: "NUMBER",
        label: {
          de: "Zeit davor"
        },
        defaultValue: 1,
        validations: ["NOT_EMPTY", "GREATER_THAN_ZERO"],
        placeholder: {
          de: "-"
        },
        pattern: "^(?!-)\\\\d*$"
      },
      {
        name: "timeCountAfter",
        type: "NUMBER",
        label: {
          de: "Zeit danach"
        },
        defaultValue: 1,
        validations: ["NOT_EMPTY", "GREATER_THAN_ZERO"],
        placeholder: {
          de: "-"
        },
        pattern: "^(?!-)\\\\d*$"
      },
      {
        name: "indexDate",
        type: "SELECT",
        label: {
          de: "Zugehörigkeit Indexdatum"
        },
        defaultValue: "BEFORE",
        options: [
          {
            label: {
              de: "VORBEOBACHTUNGSZEITRAUM"
            },
            value: "BEFORE"
          },
          {
            label: {
              de: "NEUTRAL"
            },
            value: "NEUTRAL"
          },
          {
            label: {
              de: "NACHBEOBACHTUNGSZEITRAUM"
            },
            value: "AFTER"
          }
        ],
        validations: ["NOT_EMPTY"]
      },
      {
        name: "automaticVariableSelection",
        type: "CHECKBOX",
        label: {
          de: "Automatische Variablenselektion"
        }
      },

      {
        name: "features",
        type: "CONCEPT_LIST",
        label: {
          de: "Konzepte im Vorbeobachtungszeitraum (Feature Zeitraum)"
        },
        isTwoDimensional: true,
        conceptDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        },
        conceptColumnDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "outcomes",
        type: "CONCEPT_LIST",
        label: {
          de: "Konzepte im Nachbeobachtungszeitraum (Outcome Zeitraum)"
        },
        isTwoDimensional: true,
        conceptDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        },
        conceptColumnDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "caliper",
        type: "STRING",
        label: {
          de: "Caliper"
        },
        placeholder: {
          de: "-"
        },
        defaultValue: 0.2,
        step: "0.1",
        min: 0.0,
        max: 2.0,
        validations: ["NOT_EMPTY"]
      },
      {
        name: "matchingPartners",
        type: "NUMBER",
        label: {
          de: "Matching Partner"
        },
        defaultValue: 1,
        validations: ["NOT_EMPTY"]
      },
      {
        name: "excludeOutliersDead",
        type: "CHECKBOX",
        label: {
          de: "Verstorbene ausschließen"
        }
      },
      {
        name: "excludeOutliersMaxMoney",
        type: "NUMBER",
        label: {
          de: "Ausreißer ausschließen: max. Kosten"
        }
      }
    ]
  },
  {
    title: {
      de: "Regionale Verteilung"
    },
    type: "MAP_FORM",
    headline: {
      de: "Regionale Verteilung"
    },
    fields: [
      {
        name: "title",
        type: "STRING",
        defaultValue: "Titel",
        label: {
          de: "Titel"
        },
        placeholder: {
          de: "Titel"
        }
      },
      {
        name: "description",
        type: "STRING",
        defaultValue: "Titel",
        label: {
          de: "Beschreibung"
        },
        placeholder: {
          de: "Beschreibung"
        },
        style: {
          fullWidth: true
        }
      },
      {
        name: "region",
        type: "SELECT",
        label: {
          de: "Region"
        },
        defaultValue: "DEUTSCHLAND",
        options: [
          { value: "DEUTSCHLAND", label: { de: "Deutschland" } },
          { value: "SCHLESWIG_HOLSTEIN", label: { de: "Schleswig-Holstein" } },
          { value: "HAMBURG", label: { de: "Hamburg" } },
          { value: "NIEDERSACHSEN", label: { de: "Niedersachsen" } },
          { value: "BREMEN", label: { de: "Bremen" } },
          {
            value: "NORDRHEIN_WESTFALEN",
            label: { de: "Nordrhein-Westfalen" }
          },
          { value: "HESSEN", label: { de: "Hessen" } },
          { value: "RHEINLAND_PFALZ", label: { de: "Rheinland-Pfalz" } },
          { value: "BADEN_WUERTTEMBERG", label: { de: "Baden-Württemberg" } },
          { value: "BAYERN", label: { de: "Bayern" } },
          { value: "SAARLAND", label: { de: "Saarland" } },
          { value: "BERLIN", label: { de: "Berlin" } },
          { value: "BRANDENBURG", label: { de: "Brandenburg" } },
          {
            value: "MECKLENBURG_VORPOMMERN",
            label: { de: "Mecklenburg-Vorpommern" }
          },
          { value: "SACHSEN", label: { de: "Sachsen" } },
          { value: "SACHSEN_ANHALT", label: { de: "Sachsen-Anhalt" } },
          { value: "THUERINGEN", label: { de: "Thüringen" } }
        ],
        validations: ["NOT_EMPTY"]
      },
      {
        name: "granularity",
        type: "SELECT",
        label: {
          de: "Granularität"
        },
        defaultValue: "FEDERAL_STATE",
        options: [
          { value: "FEDERAL_STATE", label: { de: "Bundesland" } },
          { value: "DISTRICT", label: { de: "Kreise" } },
          { value: "ZIP_CODE", label: { de: "PLZ" } },
          { value: "KV", label: { de: "KV" } }
        ],
        validations: ["NOT_EMPTY"]
      },
      {
        name: "queryGroup",
        type: "RESULT_GROUP",
        label: {
          de: "Versichertengruppe"
        },
        dropzoneLabel: {
          de: "Füge eine Versichertengruppe aus einer bestehenden Anfrage hinzu"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "features",
        type: "CONCEPT_LIST",
        label: {
          de: "Konzepte"
        },
        isTwoDimensional: true,
        conceptDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        },
        conceptColumnDropzoneLabel: {
          de: "Füge ein Konzept oder eine Konzeptliste hinzu"
        },
        validations: ["NOT_EMPTY"]
      },
      {
        name: "isRelative",
        type: "CHECKBOX",
        label: {
          de: "Relative Darstellung"
        }
      },
      {
        name: "resolution",
        type: "SELECT",
        label: {
          de: "Stratifizierung Beobachtungszeitraum"
        },
        defaultValue: "COMPLETE_ONLY",
        options: [
          {
            label: {
              de: "Gesamter Zeitraum"
            },
            value: "COMPLETE_ONLY"
          },
          {
            label: {
              de: "Quartale"
            },
            value: "QUARTER_WISE"
          },
          {
            label: {
              de: "Jahre"
            },
            value: "YEAR_WISE"
          }
        ],
        validations: ["NOT_EMPTY"]
      },
      {
        name: "dateRange",
        type: "DATE_RANGE",
        label: {
          de: "Beobachtungszeitraum"
        },
        validations: ["NOT_EMPTY"]
      }
    ]
  }
];

export default config;
