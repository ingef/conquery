import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import FaIcon from "../icon/FaIcon";

import type { TreesT } from "./reducer";

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
  height: 100%;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

type PropsT = {
  trees: TreesT;
};

const ProgressBar = ({ trees }: PropsT) => {
  const { t } = useTranslation();

  const treeIds = Object.entries(trees);
  const doneCount = treeIds.filter(([, tree]) => tree.success).length;

  const donePercent = (doneCount / treeIds.length) * 100;

  return (
    <Root>
      <Row>
        <FaIcon icon="spinner" />
        <Text>
          {t("conceptTreeList.loading")} {doneCount} / {treeIds.length}
        </Text>
      </Row>
      <Bar>
        <BarProgress style={{ width: `${donePercent}%` }} />
      </Bar>
    </Root>
  );
};

export default ProgressBar;
