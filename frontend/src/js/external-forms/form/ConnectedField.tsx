import type { ReactNode } from "react";
import {
  type Control,
  type ControllerRenderProps,
  useController,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { exists } from "../../common/helpers/exists";
import { C2 } from "../../ui-components/Typography";
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
  children: (
    props: ControllerRenderProps<DynamicFormValues> & { errorMessage?: string },
  ) => ReactNode;
  control: Control<DynamicFormValues>;
  formField: Field | Tabs;
  defaultValue?: unknown;
  noContainer?: boolean;
  /** the field shows the error below its input itself */
  errorInField?: boolean;
};
const fieldContainer = tv({
  base: [
    "flex flex-col",
    "gap-[5px]",
    "px-[10px] py-2",
    "bg-white",
    "rounded",
    "border border-gray-100",
  ],
  variants: {
    hasError: { true: "", false: "" },
    red: { true: "", false: "" },
  },
  compoundVariants: [
    { hasError: true, red: false, class: "border-primary-500" },
    { hasError: true, red: true, class: "border-red" },
  ],
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
  errorInField,
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

  const errorMessage = fieldState.error?.message;
  const requiredMsg = t("externalForms.formValidation.isRequired");
  const isRedError = errorMessage !== requiredMsg;

  return noContainer ? (
    <div>{children({ ...field, ...props, errorMessage })}</div>
  ) : (
    <div
      className={fieldContainer({
        hasError: exists(fieldState.error),
        red: isRedError,
      })}
    >
      {children({ ...field, ...props, errorMessage })}
      {!errorInField && (
        <C2 tone={isRedError ? "danger" : "primary"} strong={isRedError}>
          {errorMessage}
        </C2>
      )}
    </div>
  );
};
