import styled from "@emotion/styled";
import { FC } from "react";

import QueryClearButton from "./QueryClearButton";

const Container = styled("div")`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 5px;
`;

const QueryHeader: FC = () => {
  return (
    <Container>
      <QueryClearButton />
    </Container>
  );
};

export default QueryHeader;
