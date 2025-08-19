import styled from "@emotion/styled";
import { memo } from "react";
import { UseFormReturn } from "react-hook-form";

import type { SelectOptionT } from "../../api/types";
import { useActiveLang } from "../../localization/useActiveLang";
import FormHeader from "../FormHeader";
import type { Form as FormType } from "../config-types";
import { getFieldKey, getH1Index } from "../helper";

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
    <div className="w-full flex flex-col gap-2">
      {config.description && config.description[activeLang] && (
        <SxFormHeader
          description={config.description[activeLang]!}
          manualUrl={config.manualUrl}
        />
      )}
      {config.fields.map((field, i) => {
        const key = getFieldKey(config.type, field, i);
        const h1Index = getH1Index(config.fields, field);

        return (
          <Field
            key={key}
            formType={config.type}
            h1Index={h1Index}
            register={methods.register}
            control={methods.control}
            trigger={methods.trigger}
            field={field}
            setValue={methods.setValue}
            availableDatasets={datasetOptions}
            locale={activeLang}
          />
        );
      })}
    </div>
  );
});

export default Form;
