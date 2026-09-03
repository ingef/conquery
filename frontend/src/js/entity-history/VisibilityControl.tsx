import { faEye, faEyeSlash } from "@fortawesome/free-regular-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

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
        <Button
          aria-label={t("history.blurred")}
          intent="tertiary"
          onPress={toggleBlurred}
        >
          <Icon icon={blurred ? faEyeSlash : faEye} />
        </Button>
        <Tooltip placement="right">{t("history.blurred")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(VisibilityControl);
