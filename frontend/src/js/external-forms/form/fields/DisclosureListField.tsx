import {
  faAdd,
  faChevronDown,
  faChevronRight,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { ComponentProps, useEffect, useState } from "react";
import { useFieldArray } from "react-hook-form";
import tw from "tailwind-styled-components";
import IconButton from "../../../button/IconButton";
import { TransparentButton } from "../../../button/TransparentButton";
import { exists } from "../../../common/helpers/exists";
import FaIcon from "../../../icon/FaIcon";
import InfoTooltip from "../../../tooltip/InfoTooltip";
import { DisclosureListField as DisclosureListFieldT } from "../../config-types";
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
  isOpen,
  toggleOpen,
  remove,
  canRemove,
  commonProps,
}: {
  field: DisclosureListFieldT;
  index: number;
  isOpen: boolean;
  toggleOpen: () => void;
  remove: (index: number) => void;
  canRemove?: boolean;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  if (field.fields.length === 0) return null;

  const { formType, locale } = commonProps;

  return (
    <details
      className="overflow-hidden rounded border border-gray-400"
      open={isOpen}
      onToggle={(e) => {
        if (
          (isOpen && e.currentTarget.open) ||
          (!isOpen && !e.currentTarget.open)
        ) {
          // Without this, we're getting open/close flickering
          return;
        }

        toggleOpen();
      }}
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

const useOpenState = ({
  defaultOpen,
  onlyOneOpenAtATime = false,
}: {
  defaultOpen?: string;
  onlyOneOpenAtATime?: boolean;
}) => {
  const [isOpen, setIsOpen] = useState<Record<string, boolean>>(
    defaultOpen ? { [defaultOpen]: true } : {},
  );

  const toggleOpen = (id: string) => {
    setIsOpen((prev) => ({
      ...(onlyOneOpenAtATime ? {} : prev),
      [id]: !prev[id],
    }));
  };

  return { isOpen, toggleOpen };
};

export const DisclosureListField = ({
  field,
  defaultValue,
  commonProps,
  datasetId,
}: {
  field: DisclosureListFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
  datasetId: string | null;
}) => {
  const { fields, append, remove, replace } = useFieldArray({
    // gets `control` through context
    name: field.name,
  });

  const { isOpen, toggleOpen } = useOpenState({
    onlyOneOpenAtATime: field.onlyOneOpenAtATime,
    defaultOpen: field.defaultOpen ? fields[0]?.id : undefined,
  });

  useEffect(
    function applyDefaultValue() {
      if (
        fields.length === 0 &&
        exists(defaultValue) &&
        (defaultValue as unknown[]).length > 0
      ) {
        // TODO: Actually, the defaultValue SHOULD get picked up by
        // the useFieldArray hook's name and the defaultValues passed
        // to useForm above. But somehow, it doesn't. So we have to
        // manually apply the default value here.
        replace(defaultValue);
        setTimeout(() => commonProps.trigger(), 100);
      }
    },
    [fields.length, replace, defaultValue, commonProps],
  );

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
          isOpen={isOpen[fd.id]}
          toggleOpen={() => toggleOpen(fd.id)}
          canRemove={fields.length > 1}
          commonProps={commonProps}
        />
      ))}
      {field.creatable && (
        <TransparentButton
          className="w-full flex items-center justify-center gap-2"
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
          {field.createNewLabel ? field.createNewLabel[locale] : undefined}
        </TransparentButton>
      )}
    </div>
  );
};
