import styled from "@emotion/styled";
import { ReactNode } from "react";
import { Control, ControllerRenderProps, useController } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { exists } from "../../common/helpers/exists";
import { Field, Tabs } from "../config-types";
import { getErrorForField } from "../validators";
import { DynamicFormValues } from "./Form";

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
const FieldContainer = styled("div")<{
  noLabel?: boolean;
  hasError?: boolean;
  red?: boolean;
}>`
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: ${({ noLabel }) => (noLabel ? "7px 10px" : "2px 10px 7px")};
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid
    ${({ theme, hasError, red }) =>
      hasError
        ? red
          ? theme.col.red
          : theme.col.blueGrayDark
        : theme.col.grayLight};
`;

const ErrorContainer = styled("div")<{ red?: boolean }>`
  color: ${({ theme, red }) => (red ? theme.col.red : theme.col.blueGrayDark)};
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.sm};
`;

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
    <FieldContainer
      noLabel={noLabel}
      hasError={exists(fieldState.error)}
      red={isRedError}
    >
      {children({ ...field, ...props })}
      <ErrorContainer red={isRedError}>
        {fieldState.error?.message}
      </ErrorContainer>
    </FieldContainer>
  );
};
