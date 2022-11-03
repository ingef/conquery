import styled from "@emotion/styled";
import { memo } from "react";

import QueryClearButton from "./QueryClearButton";

const Container = styled("div")`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 5px;
`;

const QueryHeader = () => {
  return (
    <Container>
      <QueryClearButton />
    </Container>
  );
};

export default memo(QueryHeader);
