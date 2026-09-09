import { memo } from "react";
import { useTranslation } from "react-i18next";

import { Checkbox } from "../ui-components/Checkbox";

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
          <Checkbox
            tooltip={t("help.excludeTimestamps")}
            isSelected={excludeTimestamps}
            onChange={onToggleTimestamps}
          >
            {t("queryNodeEditor.excludeTimestamps")}
          </Checkbox>
        </div>
      )}
      {onToggleSecondaryIdExclude && (
        <div className="mb-[10px] max-w-[300px]">
          <Checkbox
            tooltip={t("help.excludeFromSecondaryId")}
            isSelected={excludeFromSecondaryId}
            onChange={onToggleSecondaryIdExclude}
          >
            {t("queryNodeEditor.excludeFromSecondaryId")}
          </Checkbox>
        </div>
      )}
    </div>
  );
};

export default memo(CommonNodeSettings);
