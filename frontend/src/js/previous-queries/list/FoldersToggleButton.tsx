import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
import WithTooltip from "../../tooltip/WithTooltip";

const FoldersToggleButton = ({
  className,
  active,
  onClick,
}: {
  className?: string;
  active?: boolean;
  onClick: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <WithTooltip
      text={t("previousQueriesFolderButton.tooltip")}
      className={className}
    >
      <IconButton
        className="mr-[5px] px-[6px] py-[9px]"
        onClick={onClick}
        icon={faFolder}
        active={active}
        frame
      />
    </WithTooltip>
  );
};
export default FoldersToggleButton;
