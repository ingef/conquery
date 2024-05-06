import styled from "@emotion/styled";
import { ComponentProps } from "react";
import { useTranslation } from "react-i18next";
import { exists } from "../../../common/helpers/exists";
import { nodeIsInvalid } from "../../../model/node";
import ToggleButton from "../../../ui-components/ToggleButton";
import { ConceptListField as ConceptListFieldT } from "../../config-types";
import FormConceptGroup from "../../form-concept-group/FormConceptGroup";
import { FormConceptGroupT } from "../../form-concept-group/formConceptGroupState";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

const SxToggleButton = styled(ToggleButton)`
  margin-bottom: 5px;
`;

export const ConceptListField = ({
  field,
  defaultValue,
  commonProps: { formType, control, locale, setValue },
}: {
  field: ConceptListFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  const { t } = useTranslation();

  return (
    <ConnectedField
      control={control}
      formField={field}
      defaultValue={defaultValue}
    >
      {({ ref, ...fieldProps }) => (
        <FormConceptGroup
          fieldName={field.name}
          value={fieldProps.value as FormConceptGroupT[]}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          label={field.label[locale] || ""}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          conceptDropzoneText={
            field.conceptDropzoneLabel
              ? field.conceptDropzoneLabel[locale] || ""
              : t("externalForms.default.conceptDropzoneLabel")
          }
          attributeDropzoneText={
            field.conceptColumnDropzoneLabel
              ? field.conceptColumnDropzoneLabel[locale] || ""
              : t("externalForms.default.conceptDropzoneLabel")
          }
          formType={formType}
          disallowMultipleColumns={!field.isTwoDimensional}
          isSingle={field.isSingle}
          blocklistedTables={field.blocklistedConnectors}
          allowlistedTables={field.allowlistedConnectors}
          blocklistedSelects={field.blocklistedSelects}
          allowlistedSelects={field.allowlistedSelects}
          defaults={field.defaults}
          isValidConcept={(item) =>
            !nodeIsInvalid(
              item,
              field.blocklistedConceptIds,
              field.allowlistedConceptIds,
            )
          }
          // What follows is VERY custom
          // Concept Group supports rendering a prefix field
          // That's specifically required by one of the forms: "PSM Form"
          // So the following looks like it wants to be generic,
          // but it's really implemented for one field
          newValue={
            field.rowPrefixField
              ? {
                  concepts: [],
                  connector: "OR",
                  [field.rowPrefixField.name]:
                    field.rowPrefixField.defaultValue,
                }
              : { concepts: [], connector: "OR" }
          }
          rowPrefixFieldname={field.rowPrefixField?.name}
          renderRowPrefix={
            exists(field.rowPrefixField)
              ? ({ value: fieldValue, onChange, row, i }) => (
                  <SxToggleButton
                    options={field.rowPrefixField!.options.map((option) => ({
                      label: option.label[locale] || "",
                      value: option.value,
                    }))}
                    value={
                      /* Because we're essentially adding an extra dynamic field to FormConceptGroupT
                            with the key `field.rowPrefixField.name` */
                      (row as unknown as Record<string, string>)[
                        field.rowPrefixField!.name
                      ]
                    }
                    onChange={(value) =>
                      onChange([
                        ...fieldValue.slice(0, i),
                        {
                          ...fieldValue[i],
                          [field.rowPrefixField!.name]: value,
                        },
                        ...fieldValue.slice(i + 1),
                      ])
                    }
                  />
                )
              : undefined
          }
        />
      )}
    </ConnectedField>
  );
};
