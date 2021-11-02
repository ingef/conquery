import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import InputPlain from "../ui-components/InputPlain";

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
  onSetTimebasedConditionMinDays?: (value: number | null) => void;
  onSetTimebasedConditionMaxDays?: (value: number | null) => void;
}

const TimebasedConditionDayRange = ({
  minDays,
  maxDays,
  onSetTimebasedConditionMinDays,
  onSetTimebasedConditionMaxDays,
}: PropsType) => {
  const { t } = useTranslation();

  return (
    <Container>
      {minDays !== undefined && !!onSetTimebasedConditionMinDays && (
        <SxInputPlain
          inputType="number"
          value={minDays}
          onChange={(value) => {
            onSetTimebasedConditionMinDays(value as number | null);
          }}
          inputProps={{ min: 1, pattern: "^(?!-)\\d*$" }}
          placeholder={t("common.timeUnitDays")}
          label={t("timebasedQueryEditor.minDaysLabel")}
          tinyLabel
        />
      )}
      {maxDays !== undefined && !!onSetTimebasedConditionMaxDays && (
        <SxInputPlain
          inputType="number"
          value={maxDays}
          onChange={(value) => {
            onSetTimebasedConditionMaxDays(value as number | null);
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
