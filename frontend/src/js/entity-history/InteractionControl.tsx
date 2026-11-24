import { ChevronRightIcon, HouseIcon } from "lucide-react";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../ui-components/Button";
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
          <HouseIcon />
        </Button>
        <Tooltip placement="right">{t("history.closeAll")}</Tooltip>
      </TooltipTrigger>
      <TooltipTrigger>
        <Button
          intent="tertiary"
          aria-label={t("history.openAll")}
          onPress={onOpenAll}
        >
          <ChevronRightIcon />
        </Button>
        <Tooltip placement="right">{t("history.openAll")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(InteractionControl);
