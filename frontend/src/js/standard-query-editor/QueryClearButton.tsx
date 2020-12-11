import React, { FC } from "react";
import styled from "@emotion/styled";

import IconButton from "../button/IconButton";

import { clearQuery } from "./actions";
import { useDispatch } from "react-redux";

const Root = styled("div")`
  padding: 0 20px 0 10px;
`;

const QueryClearButton: FC = () => {
  const dispatch = useDispatch();
  const onClearQuery = () => dispatch(clearQuery());

  return (
    <Root>
      <IconButton
        frame
        onClick={onClearQuery}
        regular
        icon="trash-alt"
      ></IconButton>
    </Root>
  );
};

export default QueryClearButton;
