import React, { Component } from "react";
import styled from "@emotion/styled";
import ReactList from "react-list";

import PreviousQuery from "./PreviousQuery";

type PropsType = {
  datasetId: string;
  queries: [];
};

const Root = styled("div")`
  flex: 1;
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;
const Container = styled("div")`
  margin: 4px 0;
`;

class PreviousQueries extends Component<PropsType> {
  _renderQuery = (index, key) => {
    return (
      <Container key={key}>
        <PreviousQuery
          query={this.props.queries[index]}
          datasetId={this.props.datasetId}
        />
      </Container>
    );
  };

  render() {
    return (
      <Root>
        {
          <ReactList
            itemRenderer={this._renderQuery}
            length={this.props.queries.length}
            type="variable"
          />
        }
      </Root>
    );
  }
}

export default PreviousQueries;
