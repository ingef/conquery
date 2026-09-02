import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import FaIcon from "../icon/FaIcon";

const ConceptTreesLoading = () => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-row items-center px-3 py-[5px]">
      <FaIcon className="mr-[10px]" icon={faSpinner} />
      <span>{t("conceptTreeList.loading")}</span>
    </div>
  );
};

export default memo(ConceptTreesLoading);
