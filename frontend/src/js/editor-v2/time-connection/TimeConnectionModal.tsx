import { memo, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { exists } from "../../common/helpers/exists";
import Modal from "../../modal/Modal";
import BaseInput from "../../ui-components/BaseInput";
import InputSelect from "../../ui-components/InputSelect/InputSelect";
import type { TimeOperator, TimeTimestamp, TreeChildrenTime } from "../types";
import { useGetNodeLabel } from "../util";

const content = tv({
  base: ["flex flex-col", "gap-[15px]", "min-w-[350px]"],
});

const row = tv({
  base: ["flex items-center", "gap-[15px]"],
});

const inputSelect = tv({
  base: ["min-w-[150px]", "basis-0"],
  variants: {
    disabled: { true: "opacity-50" },
  },
});

const conceptName = tv({
  base: ["grow", "whitespace-nowrap", "font-bold", "text-primary-500"],
});

export const TimeConnectionModal = memo(
  ({
    conditions,
    onChange,
    onClose,
  }: {
    conditions: TreeChildrenTime;
    onChange: (conditions: TreeChildrenTime) => void;
    onClose: () => void;
  }) => {
    const conditionsRef = useRef(conditions);
    conditionsRef.current = conditions;

    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;

    const { t } = useTranslation();
    const TIMESTAMP_OPTIONS = useMemo(
      () => [
        { value: "ANY", label: t("editorV2.ANY") },
        { value: "LATEST", label: t("editorV2.LATEST") },
        { value: "EARLIEST", label: t("editorV2.EARLIEST") },
        { value: "ALL", label: t("editorV2.ALL") },
      ],
      [t],
    );
    const OPERATOR_OPTIONS = useMemo(
      () => [
        { value: "BEFORE", label: t("editorV2.BEFORE") },
        { value: "AFTER", label: t("editorV2.AFTER") },
        { value: "WHILE", label: t("editorV2.WHILE") },
      ],
      [t],
    );

    const INTERVAL_OPTIONS = useMemo(
      () => [
        { value: "ANY", label: t("editorV2.intervalSome") },
        { value: "dayInterval", label: t("editorV2.dayInterval") },
      ],
      [t],
    );

    const [aTimestamp, setATimestamp] = useState(conditions.timestamps[0]);
    const [bTimestamp, setBTimestamp] = useState(conditions.timestamps[1]);
    const [operator, setOperator] = useState(conditions.operator);
    const [interval, setTheInterval] = useState(conditions.interval);

    const getNodeLabel = useGetNodeLabel();
    const a = getNodeLabel(conditions.items[0]);
    const b = getNodeLabel(conditions.items[1]);

    useEffect(() => {
      onChangeRef.current({
        ...conditionsRef.current,
        timestamps: [aTimestamp, bTimestamp],
      });
    }, [aTimestamp, bTimestamp]);

    useEffect(() => {
      onChangeRef.current({
        ...conditionsRef.current,
        operator,
      });
    }, [operator]);

    useEffect(() => {
      onChangeRef.current({
        ...conditionsRef.current,
        interval,
      });
    }, [interval]);

    return (
      <Modal onClose={onClose} headline={t("editorV2.editTimeConnection")}>
        <div className={content()}>
          <div className={row()}>
            <InputSelect
              className={inputSelect()}
              options={TIMESTAMP_OPTIONS}
              value={TIMESTAMP_OPTIONS.find((o) => o.value === aTimestamp)!}
              onChange={(opt) => {
                if (opt) {
                  setATimestamp(opt.value as TimeTimestamp);
                }
              }}
            />
            <span className="whitespace-nowrap">
              {t("editorV2.dateRangeFrom")}
            </span>
            <span className={conceptName()}>{a}</span>
          </div>
          <div className={row()}>
            <BaseInput
              className="w-[100px]"
              inputType="number"
              placeholder={operator === "WHILE" ? "0" : "1"}
              inputProps={{
                min: 0,
              }}
              value={exists(interval) ? interval.min : null}
              disabled={!interval || operator === "WHILE"}
              onChange={(val) => {
                setTheInterval({
                  min: val as number,
                  max: interval ? interval.max : null,
                });
              }}
            />
            <span>–</span>
            <BaseInput
              className="w-[100px]"
              inputType="number"
              placeholder={operator === "WHILE" ? "0" : "∞"}
              inputProps={{
                min: 0,
              }}
              value={exists(interval) ? interval.max : null}
              disabled={!interval || operator === "WHILE"}
              onChange={(val) => {
                setTheInterval({
                  max: val as number | null,
                  min: interval ? interval.min : null,
                });
              }}
            />
            <InputSelect
              className={inputSelect({ disabled: operator === "WHILE" })}
              options={INTERVAL_OPTIONS}
              value={!interval ? INTERVAL_OPTIONS[0] : INTERVAL_OPTIONS[1]}
              disabled={operator === "WHILE"}
              onChange={(opt) => {
                if (opt?.value === "ANY") {
                  setTheInterval(undefined);
                } else {
                  setTheInterval({ min: 1, max: null });
                }
              }}
            />
            <InputSelect
              className={inputSelect()}
              options={OPERATOR_OPTIONS}
              value={OPERATOR_OPTIONS.find((o) => o.value === operator)!}
              onChange={(opt) => {
                if (opt) {
                  setOperator(opt.value as TimeOperator);
                  if (opt.value === "WHILE") {
                    // Timeout to avoid race condition on effect update above
                    setTimeout(() => setTheInterval(undefined), 10);
                  }
                }
              }}
            />
          </div>
          <div className={row()}>
            <InputSelect
              className={inputSelect()}
              options={TIMESTAMP_OPTIONS}
              value={TIMESTAMP_OPTIONS.find((o) => o.value === bTimestamp)!}
              onChange={(opt) => {
                if (opt) {
                  setBTimestamp(opt.value as TimeTimestamp);
                }
              }}
            />
            <span className="whitespace-nowrap">
              {t("editorV2.dateRangeFrom")}
            </span>
            <span className={conceptName()}>{b}</span>
          </div>
        </div>
      </Modal>
    );
  },
);
