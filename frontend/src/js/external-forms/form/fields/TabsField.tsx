import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";
import { Tab, TabList, TabPanel, Tabs } from "../../../ui-components/Tabs";
import type { Tabs as TabsFieldT } from "../../config-types";
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
  field: TabsFieldT;
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
        return (
          <Tabs
            variant="secondary"
            selectedKey={fieldProps.value as string}
            onSelectionChange={(tab) => {
              commonProps.setValue(field.name, tab, setValueConfig);
              setTimeout(() => {
                commonProps.trigger();
              }, 100);
            }}
          >
            <TabList aria-label={field.name}>
              {field.tabs.map((tab) => (
                <Tab
                  key={tab.name}
                  id={tab.name}
                  tooltip={
                    tab.tooltip ? tab.tooltip[commonProps.locale] : undefined
                  }
                >
                  {tab.title[commonProps.locale] || ""}
                </Tab>
              ))}
            </TabList>
            {field.tabs.map((tab) => (
              <TabPanel key={tab.name} id={tab.name}>
                {tab.fields.length > 0 ? (
                  <div className={nestedFields()}>
                    {tab.fields.map((f, i) => {
                      const key = getFieldKey(commonProps.formType, f, i);

                      return <Field key={key} field={f} {...commonProps} />;
                    })}
                  </div>
                ) : (
                  <div className="h-[14px]" />
                )}
              </TabPanel>
            ))}
          </Tabs>
        );
      }}
    </ConnectedField>
  );
};
