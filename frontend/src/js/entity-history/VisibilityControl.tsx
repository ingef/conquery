import styled from "@emotion/styled";
import { faEye, faEyeSlash } from "@fortawesome/free-regular-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

const SxIconButton = styled(IconButton)`
  padding: 8px 10px;
`;

const VisibilityControl = ({
  blurred,
  toggleBlurred,
}: {
  blurred?: boolean;
  toggleBlurred: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <Root>
      <WithTooltip text={t("history.blurred")}>
        <SxIconButton
          onClick={toggleBlurred}
          icon={blurred ? faEyeSlash : faEye}
        />
      </WithTooltip>
    </Root>
  );
};

export default memo(VisibilityControl);
