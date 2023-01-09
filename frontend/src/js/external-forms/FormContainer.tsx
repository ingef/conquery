import styled from "@emotion/styled";
import { ComponentProps, createRef, memo } from "react";

import { exists } from "../common/helpers/exists";

import FormConfigLoader from "./FormConfigLoader";
import type { Form as FormType } from "./config-types";
import Form from "./form/Form";

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
`;

type Props = Omit<Omit<ComponentProps<typeof Form>, "config">, "containerRef"> & {
  config: FormType | null;
};

const FormContainer = ({ config, ...props }: Props) => {
  let ref = createRef<HTMLDivElement>();
  return (
    <Root ref={ref}>
      {exists(config) && (
        <FormConfigLoader datasetOptions={props.datasetOptions}>
          {() => <Form config={config} containerRef={ref} {...props} />}
        </FormConfigLoader>
      )}
    </Root>
  );
};

export default memo(FormContainer);
