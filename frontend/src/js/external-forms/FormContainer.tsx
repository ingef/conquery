import { type ComponentProps, memo } from "react";

import { exists } from "../common/helpers/exists";
import type { Form as FormType } from "./config-types";
import FormConfigLoader from "./FormConfigLoader";
import Form from "./form/Form";

const FormContainer = ({
  config,
  ...props
}: Omit<ComponentProps<typeof Form>, "config"> & {
  config: FormType | null;
}) => {
  return (
    <div className="grow overflow-y-auto [-webkit-overflow-scrolling:touch]">
      {exists(config) && (
        <FormConfigLoader datasetOptions={props.datasetOptions}>
          {() => <Form config={config} {...props} />}
        </FormConfigLoader>
      )}
    </div>
  );
};

export default memo(FormContainer);
