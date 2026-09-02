import type { ReactNode } from "react";
import {
  type Control,
  type ControllerRenderProps,
  useController,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { exists } from "../../common/helpers/exists";
import type { Field, Tabs } from "../config-types";
import { getErrorForField } from "../validators";
import type { DynamicFormValues } from "./Form";

// TODO: REFINE COLORS
// const useColorByField = (fieldType: FormField["type"]) => {
//   const theme = useTheme();

//   const COLOR_BY_FIELD_TYPE: Record<FormField["type"], string> = useMemo(
//     () => ({
//       STRING: theme.col.palette[8],
//       DATE_RANGE: theme.col.palette[0],
//       NUMBER: theme.col.palette[1],
//       CONCEPT_LIST: theme.col.palette[2],
//       SELECT: theme.col.palette[3],
//       DATASET_SELECT: theme.col.palette[4],
//       CHECKBOX: theme.col.palette[7],
//       RESULT_GROUP: theme.col.palette[5],
//       TABS: theme.col.palette[9],
//     }),
//     [theme],
//   );

//   return COLOR_BY_FIELD_TYPE[fieldType];
// };

type Props<T> = T & {
  children: (props: ControllerRenderProps<DynamicFormValues>) => ReactNode;
  control: Control<DynamicFormValues>;
  formField: Field | Tabs;
  defaultValue?: unknown;
  noContainer?: boolean;
  noLabel?: boolean;
};
const fieldContainer = tv({
  base: [
    "flex flex-col",
    "gap-[5px]",
    "bg-white",
    "rounded",
    "border border-gray-100",
  ],
  variants: {
    noLabel: {
      true: "px-[10px] py-[7px]",
      false: "pt-[2px] px-[10px] pb-[7px]",
    },
    hasError: { true: "", false: "" },
    red: { true: "", false: "" },
  },
  compoundVariants: [
    { hasError: true, red: false, class: "border-primary-500" },
    { hasError: true, red: true, class: "border-red" },
  ],
});

const errorContainer = tv({
  base: ["font-bold", "text-sm"],
  variants: {
    red: {
      true: "text-red",
      false: "text-primary-500",
    },
  },
});

export const setValueConfig = {
  shouldValidate: true,
  shouldDirty: true,
  shouldTouch: true,
};

export const ConnectedField = <T extends object>({
  children,
  control,
  formField,
  defaultValue,
  noContainer,
  noLabel,
  ...props
}: Props<T>) => {
  const { t } = useTranslation();
  const { field, fieldState } = useController<DynamicFormValues>({
    name: formField.name,
    defaultValue,
    control,
    rules: {
      validate: (value) => getErrorForField(t, formField, value) || true,
    },
    shouldUnregister: false,
  });

  // TODO: REFINE COLORS
  // const color = useColorByField(formField.type);

  const requiredMsg = t("externalForms.formValidation.isRequired");
  const isRedError = fieldState.error?.message !== requiredMsg;

  return noContainer ? (
    <div>{children({ ...field, ...props })}</div>
  ) : (
    <div
      className={fieldContainer({
        noLabel: !!noLabel,
        hasError: exists(fieldState.error),
        red: isRedError,
      })}
    >
      {children({ ...field, ...props })}
      <div className={errorContainer({ red: isRedError })}>
        {fieldState.error?.message}
      </div>
    </div>
  );
};
