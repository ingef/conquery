import styled from "@emotion/styled";
import { memo } from "react";
import { UseFormReturn } from "react-hook-form";

import type { SelectOptionT } from "../../api/types";
import { useActiveLang } from "../../localization/useActiveLang";
import FormHeader from "../FormHeader";
import type { Form as FormType } from "../config-types";
import { isFormField, isOptionalField } from "../helper";

import Field from "./Field";

const SxFormHeader = styled(FormHeader)`
  margin: 5px 0 15px;
`;

interface Props {
  config: FormType;
  datasetOptions: SelectOptionT[];
  methods: UseFormReturn<DynamicFormValues>;
}

export interface DynamicFormValues {
  [fieldname: string]: unknown;
}

const Form = memo(({ config, datasetOptions, methods }: Props) => {
  const activeLang = useActiveLang();

  return (
    <div>
      {config.description && config.description[activeLang] && (
        <SxFormHeader description={config.description[activeLang]!} />
      )}
      {config.fields.map((field, i) => {
        const key =
          isFormField(field) && field.type !== "GROUP"
            ? field.name
            : field.type + i;
        const optional = isOptionalField(field);

        return (
          <Field
            key={key}
            formType={config.type}
            register={methods.register}
            control={methods.control}
            field={field}
            setValue={methods.setValue}
            availableDatasets={datasetOptions}
            locale={activeLang}
            optional={optional}
          />
        );
      })}
    </div>
  );
});

export default Form;
