import { FolderIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Icon } from "../../ui-components/Icon";
import { ToggleButton } from "../../ui-components/ToggleButton";

import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";

const FoldersToggleButton = ({
  active,
  onClick,
}: {
  active?: boolean;
  onClick: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <TooltipTrigger>
      <ToggleButton
        aria-label={t("previousQueriesFolderButton.tooltip")}
        intent="secondary"
        onChange={onClick}
        isSelected={active}
      >
        <Icon icon={FolderIcon} />
      </ToggleButton>
      <Tooltip>{t("previousQueriesFolderButton.tooltip")}</Tooltip>
    </TooltipTrigger>
  );
};
export default FoldersToggleButton;
