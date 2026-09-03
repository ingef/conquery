import type { ComponentProps } from "react";
import { useTranslation } from "react-i18next";
import { exists } from "../../../common/helpers/exists";
import { nodeIsInvalid } from "../../../model/node";
import { ToggleButton } from "../../../ui-components/ToggleButton";
import { ToggleButtonGroup } from "../../../ui-components/ToggleButtonGroup";
import type { ConceptListField as ConceptListFieldT } from "../../config-types";
import FormConceptGroup from "../../form-concept-group/FormConceptGroup";
import type { FormConceptGroupT } from "../../form-concept-group/formConceptGroupState";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

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
                  <div className="mb-[5px]">
                    <ToggleButtonGroup
                      segmented
                      size="sm"
                      selectionMode="single"
                      disallowEmptySelection
                      selectedKeys={
                        /* Because we're essentially adding an extra dynamic field to FormConceptGroupT
                            with the key `field.rowPrefixField.name` */
                        [
                          (row as unknown as Record<string, string>)[
                            field.rowPrefixField!.name
                          ],
                        ]
                      }
                      onSelectionChange={(keys) => {
                        const [key] = keys;
                        if (typeof key !== "string") return;
                        onChange([
                          ...fieldValue.slice(0, i),
                          {
                            ...fieldValue[i],
                            [field.rowPrefixField!.name]: key,
                          },
                          ...fieldValue.slice(i + 1),
                        ]);
                      }}
                    >
                      {field.rowPrefixField!.options.map((option) => (
                        <ToggleButton key={option.value} id={option.value}>
                          {option.label[locale] || ""}
                        </ToggleButton>
                      ))}
                    </ToggleButtonGroup>
                  </div>
                )
              : undefined
          }
        />
      )}
    </ConnectedField>
  );
};
