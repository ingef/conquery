import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import ProgressBar from "../common/components/ProgressBar";
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

const SxProgressBar = styled(ProgressBar)`
  margin: 10px 0;
`;

type PropsT = {
  trees: TreesT;
};

const ConceptsProgressBar = ({ trees }: PropsT) => {
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
      <SxProgressBar donePercent={donePercent} />
    </Root>
  );
};

export default ConceptsProgressBar;
