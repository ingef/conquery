import { faEye, faEyeSlash } from "@fortawesome/free-regular-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Icon } from "../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { SidebarToggle } from "./SidebarControl";

const VisibilityControl = ({
  blurred,
  toggleBlurred,
}: {
  blurred?: boolean;
  toggleBlurred: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-center">
      <TooltipTrigger>
        <SidebarToggle
          aria-label={t("history.blurred")}
          isSelected={!!blurred}
          onChange={toggleBlurred}
        >
          <Icon icon={blurred ? faEyeSlash : faEye} />
        </SidebarToggle>
        <Tooltip placement="right">{t("history.blurred")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(VisibilityControl);
