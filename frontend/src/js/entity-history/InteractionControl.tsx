import { faChevronRight, faHome } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Icon } from "../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { SidebarAction } from "./SidebarControl";

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
        <SidebarAction aria-label={t("history.closeAll")} onPress={onCloseAll}>
          <Icon icon={faHome} />
        </SidebarAction>
        <Tooltip placement="right">{t("history.closeAll")}</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <SidebarAction aria-label={t("history.openAll")} onPress={onOpenAll}>
          <Icon icon={faChevronRight} />
        </SidebarAction>
        <Tooltip placement="right">{t("history.openAll")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(InteractionControl);
