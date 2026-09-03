import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { Button } from "../../ui-components/Button";
import { Icon } from "../../ui-components/Icon";

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
      <Button
        aria-label={t("previousQueriesFolderButton.tooltip")}
        intent="secondary"
        onPress={onClick}
        aria-pressed={active}
      >
        <Icon icon={faFolder} />
      </Button>
      <Tooltip>{t("previousQueriesFolderButton.tooltip")}</Tooltip>
    </TooltipTrigger>
  );
};
export default FoldersToggleButton;
