import { faChevronRight, faHome } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const InteractionControl = ({
  onCloseAll,
  onOpenAll,
}: {
  onCloseAll: () => void;
  onOpenAll: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-center">
      <TooltipTrigger>
        <IconButton onClick={onCloseAll} icon={faHome} />
        <Tooltip placement="right">{t("history.closeAll")}</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <IconButton onClick={onOpenAll} icon={faChevronRight} />
        <Tooltip placement="right">{t("history.openAll")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(InteractionControl);
