import { LoaderCircleIcon } from "lucide-react";
import { memo } from "react";
import { useTranslation } from "react-i18next";

const ConceptTreesLoading = () => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-row items-center px-3 py-[5px]">
      <LoaderCircleIcon className="mr-[10px]" />
      <span>{t("conceptTreeList.loading")}</span>
    </div>
  );
};

export default memo(ConceptTreesLoading);
