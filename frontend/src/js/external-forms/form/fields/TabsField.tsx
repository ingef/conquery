import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";
import type { Tabs } from "../../config-types";
import FormTabNavigation from "../../form-tab-navigation/FormTabNavigation";
import { getFieldKey } from "../../helper";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

const nestedFields = tv({
  base: [
    "flex flex-col",
    "gap-[7px]",
    "bg-bg-50",
    "px-[10px] py-3",
    "border border-gray-500",
    "rounded",
  ],
});

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
              onSelectTab={(tab) => {
                commonProps.setValue(field.name, tab, setValueConfig);
                setTimeout(() => {
                  commonProps.trigger();
                }, 100);
              }}
              options={field.tabs.map((tab) => ({
                label: () => tab.title[commonProps.locale] || "",
                value: tab.name,
                tooltip: tab.tooltip
                  ? tab.tooltip[commonProps.locale]
                  : undefined,
              }))}
            />
            {tabToShow && tabToShow.fields.length > 0 ? (
              <div className={nestedFields()}>
                {tabToShow.fields.map((f, i) => {
                  const key = getFieldKey(commonProps.formType, f, i);

                  return <Field key={key} field={f} {...commonProps} />;
                })}
              </div>
            ) : (
              <div className="h-[14px]" />
            )}
          </>
        );
      }}
    </ConnectedField>
  );
};
