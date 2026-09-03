import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
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
      <IconButton
        className="mr-[5px] px-[6px] py-[9px]"
        onClick={onClick}
        icon={faFolder}
        active={active}
        frame
      />
      <Tooltip>{t("previousQueriesFolderButton.tooltip")}</Tooltip>
    </TooltipTrigger>
  );
};
export default FoldersToggleButton;
