import { faUndo } from "@fortawesome/free-solid-svg-icons";
import { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { DateRangeT } from "../../api/types";
import type { DateStringMinMax } from "../../common/helpers/dateHelper";
import { Button } from "../../ui-components/Button";
import { Checkbox } from "../../ui-components/Checkbox";
import { Icon } from "../../ui-components/Icon";
import InputDateRange from "../../ui-components/InputDateRange";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../../ui-components/Modal";

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

  const minDate = dateRange ? dateRange.min || null : null;
  const maxDate = dateRange ? dateRange.max || null : null;
  const hasActiveDate = !!(minDate || maxDate);

  const labelSuffix = useMemo(() => {
    return hasActiveDate ? (
      <span className="ml-5">
        <Button intent="link" onPress={onResetDates}>
          <Icon icon={faUndo} />
          {t("queryNodeEditor.reset")}
        </Button>
      </span>
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
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalHeader>{t("queryGroupModal.explanation")}</ModalHeader>
      <ModalBody>
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
          <Checkbox
            isSelected={excludeFromDates}
            onChange={setExcludeFromDates}
          >
            {t("queryNodeEditor.excludeTimestamps")}
          </Checkbox>
        </div>
      </ModalBody>
      <ModalFooter>
        <Button slot="close">{t("common.done")}</Button>
      </ModalFooter>
    </Modal>
  );
};
