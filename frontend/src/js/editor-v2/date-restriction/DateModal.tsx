import styled from "@emotion/styled";
import { faCalendarMinus } from "@fortawesome/free-regular-svg-icons";
import { faUndo } from "@fortawesome/free-solid-svg-icons";
import { useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";

import { DateRangeT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { DateStringMinMax } from "../../common/helpers/dateHelper";
import { Icon } from "../../icon/FaIcon";
import Modal from "../../modal/Modal";
import InputCheckbox from "../../ui-components/InputCheckbox";
import InputDateRange from "../../ui-components/InputDateRange";

const Col = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 32px;
`;

const SectionHeadline = styled("p")`
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 10px;
  font-size: ${({ theme }) => theme.font.md};
  font-weight: 400;
`;

const ResetAll = styled(IconButton)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  margin-left: 20px;
`;

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
      <ResetAll bare onClick={onResetDates} icon={faUndo}>
        {t("queryNodeEditor.reset")}
      </ResetAll>
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
      <Col>
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
          <SectionHeadline>
            <Icon icon={faCalendarMinus} red />
            {t("queryNodeEditor.excludeTimestamps")}
            <InputCheckbox
              label=""
              onChange={setExcludeFromDates}
              value={excludeFromDates}
            />
          </SectionHeadline>
        </div>
      </Col>
    </Modal>
  );
};
