import React, { FC } from "react";
import styled from "@emotion/styled";
import QueryClearButton from "./QueryClearButton";
import SecondaryIdSelector from "./SecondaryIdSelector";

const Container = styled("div")`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
`;

const QueryHeader: FC = () => {
  return (
    <Container>
      <SecondaryIdSelector />
      <QueryClearButton />
    </Container>
  );
};

export default QueryHeader;
