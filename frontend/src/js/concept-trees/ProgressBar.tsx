import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import FaIcon from "../icon/FaIcon";

import type { TreesT } from "./reducer";
import { getConceptById } from "./globalTreeStoreHelper";

const Root = styled("div")`
  margin: 10px;
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

const Text = styled("p")`
  margin: 0 10px;
`;

const Bar = styled("div")`
  width: 100%;
  height: 7px;
  margin: 10px 0;
  background-color: #ccc;
  border-radius: ${({ theme }) => theme.borderRadius};
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.1);
`;

const BarProgress = styled("div")`
  width: ${({ donePercent }) => `${donePercent}%`};
  height: 100%;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

type PropsT = {
  trees: TreesT;
};

export default ({ trees }: PropsT) => {
  const treeIds = Object.keys(trees);
  const doneCount = treeIds.map(getConceptById).filter(c => !!c).length;

  const donePercent = (doneCount / treeIds.length) * 100;

  return (
    <Root>
      <Row>
        <FaIcon icon="spinner" />
        <Text>
          {T.translate("conceptTreeList.loading")} {doneCount} /{" "}
          {treeIds.length}
        </Text>
      </Row>
      <Bar>
        <BarProgress donePercent={donePercent} />
      </Bar>
    </Root>
  );
};
