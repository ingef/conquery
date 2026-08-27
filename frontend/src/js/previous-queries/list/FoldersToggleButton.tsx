import styled from "@emotion/styled";
import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
import WithTooltip from "../../tooltip/WithTooltip";

const FoldersButton = styled(IconButton)`
  padding: 9px 6px;
  margin-right: 5px;
`;

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
      <FoldersButton onClick={onClick} icon={faFolder} active={active} frame />
    </WithTooltip>
  );
};
export default FoldersToggleButton;
