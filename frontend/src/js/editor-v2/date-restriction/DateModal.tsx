import { faCalendarMinus } from "@fortawesome/free-regular-svg-icons";
import { faUndo } from "@fortawesome/free-solid-svg-icons";
import { useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { DateRangeT } from "../../api/types";
import IconButton from "../../button/IconButton";
import type { DateStringMinMax } from "../../common/helpers/dateHelper";
import FaIcon from "../../icon/FaIcon";
import Modal from "../../modal/Modal";
import InputCheckbox from "../../ui-components/InputCheckbox";
import InputDateRange from "../../ui-components/InputDateRange";

const sectionHeadline = tv({
  base: [
    "flex items-center",
    "gap-[10px]",
    "mb-[10px]",
    "text-base",
    "font-normal",
  ],
});

const resetAll = tv({
  base: ["ml-5", "text-primary-500", "font-bold"],
});

export const DateModal = ({
  onClose,
  dateRange = {},
  headline,
  excludeFromDates,
  setExcludeFromDates,
  setDateRange,
  onResetDates,
}: {
  onClose: () => void;
  excludeFromDates?: boolean;
  setExcludeFromDates: (exclude: boolean) => void;
  dateRange?: DateRangeT;
  setDateRange: (range: DateRangeT) => void;
  headline: string;
  onResetDates: () => void;
}) => {
  const { t } = useTranslation();

  useHotkeys("esc", onClose, [onClose]);

  const minDate = dateRange ? dateRange.min || null : null;
  const maxDate = dateRange ? dateRange.max || null : null;
  const hasActiveDate = !!(minDate || maxDate);

  const labelSuffix = useMemo(() => {
    return hasActiveDate ? (
      <IconButton
        className={resetAll()}
        bare
        onClick={onResetDates}
        icon={faUndo}
      >
        {t("queryNodeEditor.reset")}
      </IconButton>
    ) : null;
  }, [t, hasActiveDate, onResetDates]);

  const onChange = useCallback(
    (date: DateStringMinMax) => {
      if (!date.min && !date.max) return;

      setDateRange({
        min: date.min || undefined,
        max: date.max || undefined,
      });
    },
    [setDateRange],
  );

  return (
    <Modal
      onClose={onClose}
      doneButton
      headline={t("queryGroupModal.explanation")}
    >
      <div className="flex flex-col gap-8">
        <div>{headline}</div>
        <InputDateRange
          large
          inline
          autoFocus
          label={t("queryGroupModal.dateRange")}
          labelSuffix={labelSuffix}
          onChange={onChange}
          value={{
            min: minDate,
            max: maxDate,
          }}
        />
        <div>
          <p className={sectionHeadline()}>
            <FaIcon icon={faCalendarMinus} red />
            {t("queryNodeEditor.excludeTimestamps")}
            <InputCheckbox
              label=""
              onChange={setExcludeFromDates}
              value={excludeFromDates}
            />
          </p>
        </div>
      </div>
    </Modal>
  );
};
