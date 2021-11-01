import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC, memo } from "react";
import { useSelector } from "react-redux";

import type { Form as FormType } from "./config-types";
import Form from "./form/Form";
import { selectFormConfig } from "./stateSelectors";

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px 20px 10px;
`;

const FormsContainer: FC = () => {
  const formConfig = useSelector<StateT, FormType | null>(selectFormConfig);

  return <Root>{!!formConfig && <Form config={formConfig} />}</Root>;
};

export default memo(FormsContainer);
