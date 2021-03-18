import React from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import InputText from "../form-components/InputText";

const Container = styled("div")`
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
`;

const StyledInputText = styled(InputText)`
  padding: 0 5px;
`;

type PropsType = {
  minDays?: number | string | null;
  maxDays?: number | string | null;
  onSetTimebasedConditionMinDays: Function;
  onSetTimebasedConditionMaxDays: Function;
};

const TimebasedConditionDayRange = (props: PropsType) => {
  const { t } = useTranslation();

  return (
    <Container>
      {props.minDays !== undefined && (
        <StyledInputText
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
        <StyledInputText
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
