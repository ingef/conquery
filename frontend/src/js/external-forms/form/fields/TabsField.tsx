import styled from "@emotion/styled";
import { ComponentProps } from "react";
import { Tabs } from "../../config-types";
import FormTabNavigation from "../../form-tab-navigation/FormTabNavigation";
import { getFieldKey } from "../../helper";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

const Spacer = styled("div")`
  height: 14px;
`;

const NestedFields = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 7px;
  background-color: ${({ theme }) => theme.col.bg};
  padding: 12px 10px 12px;
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

export const TabsField = ({
  field,
  commonProps,
  defaultValue,
}: {
  field: Tabs;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
  defaultValue: unknown;
}) => {
  return (
    <ConnectedField
      control={commonProps.control}
      formField={field}
      defaultValue={defaultValue}
      noContainer
    >
      {({ ref, ...fieldProps }) => {
        const tabToShow = field.tabs.find(
          (tab) => tab.name === fieldProps.value,
        );

        return (
          <>
            <FormTabNavigation
              selectedTab={fieldProps.value as string}
              onSelectTab={(tab) =>
                commonProps.setValue(field.name, tab, setValueConfig)
              }
              options={field.tabs.map((tab) => ({
                label: () => tab.title[commonProps.locale] || "",
                value: tab.name,
                tooltip: tab.tooltip
                  ? tab.tooltip[commonProps.locale]
                  : undefined,
              }))}
            />
            {tabToShow && tabToShow.fields.length > 0 ? (
              <NestedFields>
                {tabToShow.fields.map((f, i) => {
                  const key = getFieldKey(commonProps.formType, f, i);

                  return <Field key={key} field={f} {...commonProps} />;
                })}
              </NestedFields>
            ) : (
              <Spacer />
            )}
          </>
        );
      }}
    </ConnectedField>
  );
};
