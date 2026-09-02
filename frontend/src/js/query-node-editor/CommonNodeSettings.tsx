import { memo } from "react";
import { useTranslation } from "react-i18next";

import InputCheckbox from "../ui-components/InputCheckbox";

interface Props {
  excludeTimestamps?: boolean;
  onToggleTimestamps?: (excludeTimestamps: boolean) => void;
  excludeFromSecondaryId?: boolean;
  onToggleSecondaryIdExclude?: (excludeFromSecondaryId: boolean) => void;
}

const CommonNodeSettings = ({
  excludeTimestamps,
  onToggleTimestamps,
  excludeFromSecondaryId,
  onToggleSecondaryIdExclude,
}: Props) => {
  const { t } = useTranslation();

  return (
    <div className="mx-[10px] my-[15px]">
      {onToggleTimestamps && (
        <div className="mb-[10px] max-w-[300px]">
          <InputCheckbox
            label={t("queryNodeEditor.excludeTimestamps")}
            tooltip={t("help.excludeTimestamps")}
            tooltipLazy
            value={excludeTimestamps}
            onChange={onToggleTimestamps}
          />
        </div>
      )}
      {onToggleSecondaryIdExclude && (
        <div className="mb-[10px] max-w-[300px]">
          <InputCheckbox
            label={t("queryNodeEditor.excludeFromSecondaryId")}
            tooltip={t("help.excludeFromSecondaryId")}
            tooltipLazy
            value={excludeFromSecondaryId}
            onChange={onToggleSecondaryIdExclude}
          />
        </div>
      )}
    </div>
  );
};

export default memo(CommonNodeSettings);
