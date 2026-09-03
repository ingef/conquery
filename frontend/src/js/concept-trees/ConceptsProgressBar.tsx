import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import ProgressBar from "../common/components/ProgressBar";
import FaIcon from "../icon/FaIcon";

import type { TreesT } from "./reducer";

type PropsT = {
  trees: TreesT;
};

const ConceptsProgressBar = ({ trees }: PropsT) => {
  const { t } = useTranslation();

  const treeIds = Object.entries(trees);
  const doneCount = treeIds.filter(([, tree]) => tree.success).length;

  const donePercent = (doneCount / treeIds.length) * 100;

  return (
    <div className="m-[10px]">
      <div className="flex items-center">
        <FaIcon icon={faSpinner} />
        <p className="mx-[10px]">
          {t("conceptTreeList.loading")} {doneCount} / {treeIds.length}
        </p>
      </div>
      <ProgressBar className="my-[10px]" donePercent={donePercent} />
    </div>
  );
};

export default ConceptsProgressBar;
