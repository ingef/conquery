// @flow

import React, { Component } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import ReactList from "react-list";

import PreviousQuery from "./PreviousQuery";

type PropsType = {
  datasetId: string,
  queries: [],
  loading: boolean,
  loadQueries: () => void
};

const Root = styled("div")`
  flex: 1;
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px 0 20px;
`;
const Container = styled("div")`
  margin: 4px 0;
`;
const Loading = styled("p")`
  margin: 2px 10px;
`;
const Spinner = styled("span")`
  margin-right: 5px;
`;

class PreviousQueries extends Component<PropsType> {
  componentDidMount() {
    this.props.loadQueries();
  }

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
    const { loading } = this.props;

    return (
      <Root>
        {loading && (
          <Loading>
            <Spinner>
              <i className="fa fa-spinner" />
            </Spinner>
            <span>{T.translate("previousQueries.loading")}</span>
          </Loading>
        )}
        {this.props.queries.length === 0 &&
          !loading &&
          T.translate("previousQueries.noQueriesFound")}
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
