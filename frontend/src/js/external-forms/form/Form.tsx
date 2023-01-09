import styled from "@emotion/styled";
import { memo, RefObject } from "react";
import { UseFormReturn } from "react-hook-form";

import type { SelectOptionT } from "../../api/types";
import { useActiveLang } from "../../localization/useActiveLang";
import FormHeader from "../FormHeader";
import type { Form as FormType } from "../config-types";
import { getFieldKey, isOptionalField } from "../helper";

import Field from "./Field";

const FormContent = styled("div")`
  width: 100%;
`;

const SxFormHeader = styled(FormHeader)`
  margin: 5px 0 15px;
`;

interface Props {
  config: FormType;
  datasetOptions: SelectOptionT[];
  methods: UseFormReturn<DynamicFormValues>;
  containerRef: RefObject<HTMLDivElement>;
}

export interface DynamicFormValues {
  [fieldname: string]: unknown;
}

const Form = memo(
  ({ config, datasetOptions, methods, containerRef }: Props) => {
    const activeLang = useActiveLang();

    return (
      <FormContent>
        {config.description && config.description[activeLang] && (
          <SxFormHeader
            description={config.description[activeLang]!}
            manualUrl={config.manualUrl}
          />
        )}
        {config.fields.map((field, i) => {
          const key = getFieldKey(config.type, field, i);
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
              containerRef={containerRef}
            />
          );
        })}
      </FormContent>
    );
  },
);

export default Form;
