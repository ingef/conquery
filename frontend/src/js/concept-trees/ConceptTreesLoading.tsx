import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import { Icon } from "../ui-components/Icon";

const ConceptTreesLoading = () => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-row items-center px-3 py-[5px]">
      <Icon icon={faSpinner} className="mr-[10px]" />
      <span>{t("conceptTreeList.loading")}</span>
    </div>
  );
};

export default memo(ConceptTreesLoading);
