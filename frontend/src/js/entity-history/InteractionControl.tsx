import { faChevronRight, faHome } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";
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
        <Button
          intent="tertiary"
          aria-label={t("history.closeAll")}
          onPress={onCloseAll}
        >
          <Icon icon={faHome} />
        </Button>
        <Tooltip placement="right">{t("history.closeAll")}</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <Button
          intent="tertiary"
          aria-label={t("history.openAll")}
          onPress={onOpenAll}
        >
          <Icon icon={faChevronRight} />
        </Button>
        <Tooltip placement="right">{t("history.openAll")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(InteractionControl);
