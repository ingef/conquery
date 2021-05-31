import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";

import InputPlain from "../form-components/InputPlain";

const Container = styled("div")`
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
`;

const SxInputPlain = styled(InputPlain)`
  padding: 0 5px;
`;

interface PropsType {
  minDays?: number | string | null;
  maxDays?: number | string | null;
  onSetTimebasedConditionMinDays: Function;
  onSetTimebasedConditionMaxDays: Function;
}

const TimebasedConditionDayRange = (props: PropsType) => {
  const { t } = useTranslation();

  return (
    <Container>
      {props.minDays !== undefined && (
        <SxInputPlain
          inputType="number"
          input={{
            value: props.minDays,
            onChange: (value) => props.onSetTimebasedConditionMinDays(value),
          }}
          inputProps={{ min: 1, pattern: "^(?!-)\\d*$" }}
          placeholder={t("common.timeUnitDays")}
          label={t("timebasedQueryEditor.minDaysLabel")}
          tinyLabel
        />
      )}
      {props.maxDays !== undefined && (
        <SxInputPlain
          inputType="number"
          input={{
            value: props.maxDays,
            onChange: (value) => props.onSetTimebasedConditionMaxDays(value),
          }}
          inputProps={{ min: 1, pattern: "^(?!-)\\d*$" }}
          placeholder={t("common.timeUnitDays")}
          label={t("timebasedQueryEditor.maxDaysLabel")}
          tinyLabel
        />
      )}
    </Container>
  );
};

export default TimebasedConditionDayRange;
