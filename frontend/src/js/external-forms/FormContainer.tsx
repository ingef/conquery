import styled from "@emotion/styled";
import { ComponentProps, memo } from "react";

import { exists } from "../common/helpers/exists";

import FormConfigLoader from "./FormConfigLoader";
import type { Form as FormType } from "./config-types";
import Form from "./form/Form";

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
`;

const FormContainer = ({
  config,
  ...props
}: Omit<ComponentProps<typeof Form>, "config"> & {
  config: FormType | null;
}) => {
  return (
    <Root>
      {exists(config) && (
        <FormConfigLoader datasetOptions={props.datasetOptions}>
          {() => <Form config={config} {...props} />}
        </FormConfigLoader>
      )}
    </Root>
  );
};

export default memo(FormContainer);
