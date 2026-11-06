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
    <div className="flex flex-col gap-2 p-2">
      {onToggleTimestamps && (
        <div className="max-w-[300px]">
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
        <div className="max-w-[300px]">
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
