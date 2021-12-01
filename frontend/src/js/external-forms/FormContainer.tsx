import styled from "@emotion/styled";
import { ComponentProps, memo } from "react";

import { exists } from "../common/helpers/exists";

import type { Form as FormType } from "./config-types";
import Form from "./form/Form";

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px 20px 10px;
`;

type Props = Omit<ComponentProps<typeof Form>, "config"> & {
  config: FormType | null;
};

const FormContainer = ({ config, ...props }: Props) => {
  return <Root>{exists(config) && <Form config={config} {...props} />}</Root>;
};

export default memo(FormContainer);
