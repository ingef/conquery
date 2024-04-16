import {
  faAdd,
  faChevronDown,
  faChevronRight,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { ComponentProps, useState } from "react";
import { useFieldArray } from "react-hook-form";
import tw from "tailwind-styled-components";
import IconButton from "../../../button/IconButton";
import { TransparentButton } from "../../../button/TransparentButton";
import { exists } from "../../../common/helpers/exists";
import FaIcon from "../../../icon/FaIcon";
import InfoTooltip from "../../../tooltip/InfoTooltip";
import { Disclosure } from "../../config-types";
import {
  getFieldKey,
  getInitialValue,
  isFormFieldWithValue,
} from "../../helper";
import Field from "../Field";

const Summary = tw("summary")`
  relative
  cursor-pointer
  flex
  items-center
  justify-between
  gap-3
  py-3
  pl-3
  pr-10
  bg-white
  text-sm
  font-normal
`;

const DisclosureField = ({
  field,
  index,
  remove,
  canRemove,
  commonProps,
}: {
  field: Disclosure;
  index: number;
  remove: (index: number) => void;
  canRemove?: boolean;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  const [isOpen, setOpen] = useState(false);

  if (field.fields.length === 0) return null;

  const { formType, locale } = commonProps;

  return (
    <details
      className="overflow-hidden rounded border border-gray-400"
      open={isOpen}
      onToggle={() => setOpen(!isOpen)}
    >
      <Summary>
        <div className="flex items-center gap-3">
          <span className="w-5">
            <FaIcon icon={isOpen ? faChevronDown : faChevronRight} />
          </span>
          {field.label[locale]}
          {exists(field.tooltip) && (
            <InfoTooltip text={field.tooltip[locale]} />
          )}
        </div>
        {field.creatable && canRemove && (
          <IconButton
            className="absolute right-0 top-1/2 -translate-y-1/2"
            icon={faTimes}
            onClick={() => remove(index)}
          />
        )}
      </Summary>
      <div className="flex flex-col gap-2 bg-bg-50 border-t border-gray-300 p-3">
        {field.fields.map((f, i) => {
          const key = getFieldKey(formType, f, i);
          const childField = isFormFieldWithValue(f)
            ? { ...f, name: `${field.name}[${index}].${f.name}` }
            : f;

          return <Field key={key} field={childField} {...commonProps} />;
        })}
      </div>
    </details>
  );
};

export const DisclosureListField = ({
  field,
  defaultValue,
  commonProps,
  datasetId,
}: {
  field: Disclosure;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
  datasetId: string | null;
}) => {
  const { fields, append, remove } = useFieldArray({
    control: commonProps.control,
    // @ts-expect-error TODO: figure out how to deal with a dynamic name
    name: field.name,
  });
  console.log(field, defaultValue);
  console.log(fields);

  if (field.fields.length === 0) return null;

  const { locale } = commonProps;

  return (
    <div className="space-y-2">
      {fields.map((fd, index) => (
        <DisclosureField
          key={fd.id}
          field={field}
          index={index}
          remove={remove}
          canRemove={fields.length > 1}
          commonProps={commonProps}
        />
      ))}
      {field.creatable && (
        <TransparentButton
          className="w-full"
          small
          onClick={() =>
            append(
              Object.fromEntries(
                field.fields.filter(isFormFieldWithValue).map((f) => [
                  f.name,
                  getInitialValue(f, {
                    activeLang: locale,
                    availableDatasets: commonProps.availableDatasets,
                    datasetId,
                  }),
                ]),
              ),
            )
          }
        >
          <FontAwesomeIcon icon={faAdd} />
        </TransparentButton>
      )}
    </div>
  );
};
