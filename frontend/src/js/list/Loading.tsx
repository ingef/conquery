import React from "react";
import styled from "@emotion/styled";
import FaIcon from "../icon/FaIcon";

const Root = styled("p")`
  margin: 2px 10px;
`;
const Spinner = styled("span")`
  margin-right: 5px;
`;

interface PropsT {
  message: React.ReactNode;
}

const Loading: React.FC<PropsT> = ({ message }) => {
  return (
    <Root>
      <Spinner>
        <FaIcon icon="spinner" />
      </Spinner>
      <span>{message}</span>
    </Root>
  );
};

export default Loading;
