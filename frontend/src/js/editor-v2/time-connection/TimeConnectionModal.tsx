import styled from "@emotion/styled";
import { memo, useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import { exists } from "../../common/helpers/exists";
import Modal from "../../modal/Modal";
import BaseInput from "../../ui-components/BaseInput";
import InputSelect from "../../ui-components/InputSelect/InputSelect";
import { TimeOperator, TimeTimestamp, TreeChildrenTime } from "../types";
import { useGetNodeLabel } from "../util";

const Content = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 15px;
  min-width: 350px;
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
  gap: 15px;
`;

const SxBaseInput = styled(BaseInput)`
  width: 100px;
`;

const SxInputSelect = styled(InputSelect)<{ disabled?: boolean }>`
  min-width: 150px;
  flex-basis: 0;
  opacity: ${({ disabled }) => (disabled ? 0.5 : 1)};
`;

const DateRangeFrom = styled("span")`
  white-space: nowrap;
`;

const ConceptName = styled("span")`
  white-space: nowrap;
  font-weight: bold;
  color: ${({ theme }) => theme.col.blueGrayDark};
  flex-grow: 1;
`;

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
        { value: "some", label: t("editorV2.some") },
        { value: "latest", label: t("editorV2.latest") },
        { value: "earliest", label: t("editorV2.earliest") },
        { value: "every", label: t("editorV2.every") },
      ],
      [t],
    );
    const OPERATOR_OPTIONS = useMemo(
      () => [
        { value: "before", label: t("editorV2.before") },
        { value: "after", label: t("editorV2.after") },
        { value: "while", label: t("editorV2.while") },
      ],
      [t],
    );

    const INTERVAL_OPTIONS = useMemo(
      () => [
        { value: "some", label: t("editorV2.intervalSome") },
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
        <Content>
          <Row>
            <SxInputSelect
              options={TIMESTAMP_OPTIONS}
              value={TIMESTAMP_OPTIONS.find((o) => o.value === aTimestamp)!}
              onChange={(opt) => {
                if (opt) {
                  setATimestamp(opt.value as TimeTimestamp);
                }
              }}
            />
            <DateRangeFrom>{t("editorV2.dateRangeFrom")}</DateRangeFrom>
            <ConceptName>{a}</ConceptName>
          </Row>
          <Row>
            <SxBaseInput
              inputType="number"
              placeholder={operator === "while" ? "0" : "1"}
              inputProps={{
                min: 0,
              }}
              value={exists(interval) ? interval.min : null}
              disabled={!interval || operator === "while"}
              onChange={(val) => {
                setTheInterval({
                  min: val as number,
                  max: interval ? interval.max : null,
                });
              }}
            />
            <span>–</span>
            <SxBaseInput
              inputType="number"
              placeholder={operator === "while" ? "0" : "∞"}
              inputProps={{
                min: 0,
              }}
              value={exists(interval) ? interval.max : null}
              disabled={!interval || operator === "while"}
              onChange={(val) => {
                setTheInterval({
                  max: val as number | null,
                  min: interval ? interval.min : null,
                });
              }}
            />
            <SxInputSelect
              options={INTERVAL_OPTIONS}
              value={!interval ? INTERVAL_OPTIONS[0] : INTERVAL_OPTIONS[1]}
              disabled={operator === "while"}
              onChange={(opt) => {
                if (opt?.value === "some") {
                  setTheInterval(undefined);
                } else {
                  setTheInterval({ min: 1, max: null });
                }
              }}
            />
            <SxInputSelect
              options={OPERATOR_OPTIONS}
              value={OPERATOR_OPTIONS.find((o) => o.value === operator)!}
              onChange={(opt) => {
                if (opt) {
                  setOperator(opt.value as TimeOperator);
                  if (opt.value === "while") {
                    // Timeout to avoid race condition on effect update above
                    setTimeout(() => setTheInterval(undefined), 10);
                  }
                }
              }}
            />
          </Row>
          <Row>
            <SxInputSelect
              options={TIMESTAMP_OPTIONS}
              value={TIMESTAMP_OPTIONS.find((o) => o.value === bTimestamp)!}
              onChange={(opt) => {
                if (opt) {
                  setBTimestamp(opt.value as TimeTimestamp);
                }
              }}
            />
            <DateRangeFrom>{t("editorV2.dateRangeFrom")}</DateRangeFrom>
            <ConceptName>{b}</ConceptName>
          </Row>
        </Content>
      </Modal>
    );
  },
);
