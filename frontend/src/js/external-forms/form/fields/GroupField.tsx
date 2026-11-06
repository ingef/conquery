import type { ComponentProps } from "react";
import type { Group } from "../../config-types";
import { Description } from "../../form-components/Description";
import { Headline } from "../../form-components/Headline";
import { getFieldKey } from "../../helper";
import Field from "../Field";

export const GroupField = ({
  field,
  commonProps,
}: {
  field: Group;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  return (
    <>
      {field.label && <Headline>{field.label[commonProps.locale]}</Headline>}
      {field.description && (
        <Description>{field.description[commonProps.locale]}</Description>
      )}
      <div
        className="flex flex-row flex-wrap"
        style={{
          display: field.style?.display || "flex",
          gap: field.style?.display === "grid" ? "7px 12px" : "7px 8px",
          gridTemplateColumns: `repeat(${field.style?.gridColumns || 1}, 1fr)`,
        }}
      >
        {field.fields.map((f, i) => {
          const key = getFieldKey(commonProps.formType, f, i);

          return <Field key={key} field={f} {...commonProps} />;
        })}
      </div>
    </>
  );
};
