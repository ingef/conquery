import { memo } from "react";
import type { UseFormReturn } from "react-hook-form";

import type { SelectOptionT } from "../../api/types";
import { useActiveLang } from "../../localization/useActiveLang";
import type { Form as FormType } from "../config-types";
import FormHeader from "../FormHeader";
import { getFieldKey, getH1Index } from "../helper";

import Field from "./Field";

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
      {config.description?.[activeLang] && (
        <FormHeader
          className="mt-[5px] mb-[15px]"
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
