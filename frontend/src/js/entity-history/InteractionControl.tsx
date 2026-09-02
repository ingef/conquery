import { faChevronRight, faHome } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../ui-components/WithTooltip";

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
      <WithTooltip text={t("history.closeAll")}>
        <IconButton onClick={onCloseAll} icon={faHome} />
      </WithTooltip>
      <WithTooltip text={t("history.openAll")}>
        <IconButton onClick={onOpenAll} icon={faChevronRight} />
      </WithTooltip>
    </div>
  );
};

export default memo(InteractionControl);
