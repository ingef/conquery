import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

const InteractionControl = ({
  onCloseAll,
  onOpenAll,
}: {
  onCloseAll: () => void;
  onOpenAll: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <Root>
      <WithTooltip text={t("history.closeAll")}>
        <IconButton onClick={onCloseAll} icon="home" />
      </WithTooltip>
      <WithTooltip text={t("history.openAll")}>
        <IconButton onClick={onOpenAll} icon="chevron-right" />
      </WithTooltip>
    </Root>
  );
};

export default memo(InteractionControl);
