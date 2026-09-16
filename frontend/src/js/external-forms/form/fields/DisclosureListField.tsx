import { faAdd, faTimes } from "@fortawesome/free-solid-svg-icons";
import { type ComponentProps, useCallback, useEffect, useState } from "react";
import type { Key } from "react-aria-components";
import { useFieldArray } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { exists } from "../../../common/helpers/exists";
import { usePrevious } from "../../../common/helpers/usePrevious";
import { Button } from "../../../ui-components/Button";
import {
  Disclosure,
  DisclosureGroup,
  DisclosurePanel,
  DisclosureTitle,
} from "../../../ui-components/Disclosure";
import { Icon } from "../../../ui-components/Icon";
import type { DisclosureListField as DisclosureListFieldT } from "../../config-types";
import {
  getFieldKey,
  getInitialValue,
  isFormFieldWithValue,
} from "../../helper";
import Field from "../Field";

const DisclosureField = ({
  id,
  field,
  index,
  remove,
  canRemove,
  commonProps,
}: {
  id: string;
  field: DisclosureListFieldT;
  index: number;
  remove: (index: number) => void;
  canRemove?: boolean;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  const { t } = useTranslation();

  if (field.fields.length === 0) return null;

  const { formType, locale } = commonProps;

  return (
    <Disclosure id={id}>
      <DisclosureTitle
        info={exists(field.tooltip) ? field.tooltip[locale] : undefined}
        actions={
          field.creatable &&
          canRemove && (
            <Button
              size="sm"
              intent="tertiary"
              aria-label={t("common.delete")}
              onPress={() => remove(index)}
            >
              <Icon icon={faTimes} />
            </Button>
          )
        }
      >
        {field.label[locale]}
      </DisclosureTitle>
      <DisclosurePanel>
        <div className="flex flex-col gap-2">
          {field.fields.map((f, i) => {
            const key = getFieldKey(formType, f, i);
            const childField = isFormFieldWithValue(f)
              ? { ...f, name: `${field.name}[${index}].${f.name}` }
              : f;

            return <Field key={key} field={childField} {...commonProps} />;
          })}
        </div>
      </DisclosurePanel>
    </Disclosure>
  );
};

// which sections are open; the group keeps a single one when only one may be
const useExpandedKeys = ({
  defaultOpen,
  onlyOneOpenAtATime = false,
}: {
  defaultOpen?: string;
  onlyOneOpenAtATime?: boolean;
}) => {
  const [expandedKeys, setExpandedKeys] = useState<Set<Key>>(
    () => new Set(defaultOpen ? [defaultOpen] : []),
  );

  const open = useCallback(
    (id: string) => {
      setExpandedKeys((prev) =>
        onlyOneOpenAtATime ? new Set([id]) : new Set(prev).add(id),
      );
    },
    [onlyOneOpenAtATime],
  );

  return { expandedKeys, setExpandedKeys, open };
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

  const prevFieldsLength = usePrevious(fields.length);

  const { expandedKeys, setExpandedKeys, open } = useExpandedKeys({
    onlyOneOpenAtATime: field.onlyOneOpenAtATime,
    defaultOpen: field.defaultOpen ? fields[0]?.id : undefined,
  });

  useEffect(
    function openFirstFieldIfNecessary() {
      if (prevFieldsLength === 0 && fields.length > 0 && field.defaultOpen) {
        const id = fields[0]?.id;
        if (id) open(id);
      }
    },
    [prevFieldsLength, fields, open, field.defaultOpen],
  );

  useEffect(
    function openLastFieldAfterAppending() {
      if (
        exists(prevFieldsLength) &&
        prevFieldsLength > 0 &&
        prevFieldsLength < fields.length &&
        field.defaultOpen
      ) {
        commonProps.trigger();

        const id = fields[fields.length - 1]?.id;
        if (id) open(id);
      }
    },
    [prevFieldsLength, fields, open, field.defaultOpen, commonProps],
  );

  if (field.fields.length === 0) return null;

  const { locale } = commonProps;

  return (
    <div className="flex flex-col gap-2">
      <DisclosureGroup
        allowsMultipleExpanded={!field.onlyOneOpenAtATime}
        expandedKeys={expandedKeys}
        onExpandedChange={setExpandedKeys}
      >
        {fields.map((fd, index) => (
          <DisclosureField
            key={fd.id}
            id={fd.id}
            field={field}
            index={index}
            remove={remove}
            canRemove={fields.length > 1}
            commonProps={commonProps}
          />
        ))}
      </DisclosureGroup>
      {field.creatable && (
        <div className="grid">
          <Button
            intent="secondary"
            size="sm"
            onPress={() => {
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
              );
            }}
          >
            <Icon icon={faAdd} />
            {field.createNewLabel ? field.createNewLabel[locale] : undefined}
          </Button>
        </div>
      )}
    </div>
  );
};
