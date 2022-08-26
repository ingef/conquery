import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const InteractionControl = ({ onCloseAll }: { onCloseAll: () => void }) => {
  const { t } = useTranslation();

  return (
    <WithTooltip text={t("history.closeAll")}>
      <IconButton onClick={onCloseAll} icon="home" />
    </WithTooltip>
  );
};

export default memo(InteractionControl);
