import { memo } from "react";
import { useTranslation } from "react-i18next";

import { CheckboxField } from "../ui-components/CheckboxField";

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
          <CheckboxField
            tooltip={t("help.excludeTimestamps")}
            isSelected={excludeTimestamps}
            onChange={onToggleTimestamps}
          >
            {t("queryNodeEditor.excludeTimestamps")}
          </CheckboxField>
        </div>
      )}
      {onToggleSecondaryIdExclude && (
        <div className="mb-[10px] max-w-[300px]">
          <CheckboxField
            tooltip={t("help.excludeFromSecondaryId")}
            isSelected={excludeFromSecondaryId}
            onChange={onToggleSecondaryIdExclude}
          >
            {t("queryNodeEditor.excludeFromSecondaryId")}
          </CheckboxField>
        </div>
      )}
    </div>
  );
};

export default memo(CommonNodeSettings);
