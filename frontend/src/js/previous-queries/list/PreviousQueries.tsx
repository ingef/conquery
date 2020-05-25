import React from "react";
import styled from "@emotion/styled";
import ReactList from "react-list";

import PreviousQueryDragContainer from "./PreviousQueryDragContainer";
import { PreviousQueryT } from "./reducer";

interface PropsT {
  datasetId: string;
  queries: PreviousQueryT[];
}

const Root = styled("div")`
  flex: 1;
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;
const Container = styled("div")`
  margin: 4px 0;
`;

const PreviousQueries: React.FC<PropsT> = ({ datasetId, queries }) => {
  function renderQuery(index: number, key: string | number) {
    return (
      <Container key={key}>
        <PreviousQueryDragContainer
          query={queries[index]}
          datasetId={datasetId}
        />
      </Container>
    );
  }

  return (
    <Root>
      <ReactList
        itemRenderer={renderQuery}
        length={queries.length}
        type="variable"
      />
    </Root>
  );
};

export default PreviousQueries;
