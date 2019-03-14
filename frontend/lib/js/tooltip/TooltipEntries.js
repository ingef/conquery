// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import {
  formatDate,
  parseDate,
  numberToThreeDigitArray
} from "../common/helpers";

import FaIcon from "../icon/FaIcon";

type PropsType = {
  className?: string,
  matchingEntries?: ?number,
  dateRange?: ?Object
};

const Root = styled("div")`
  padding-left: 20px;
  display: flex;
  flex-direction: row;
  align-items: center;
`;

const DateContainer = styled("div")`
  display: inline-block;
`;

const Date = styled("p")`
  margin: 0;
  padding-right: 6px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const ConceptDateRangeTooltip = styled("div")`
  margin: 0 40px 0 25px;
`;

const Text = styled("p")`
  margin: 0 0 5px;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, zero }) => (zero ? theme.col.red : "inherit")};
`;

const StyledFaIcon = styled(FaIcon)`
  padding-right: 15px;
  display: inline-block;
  vertical-align: middle;
`;

const Info = styled("div")`
  display: inline-block;
  vertical-align: middle;
`;

const Number = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme, zero }) => (zero ? theme.col.red : "inherit")};
`;

const Digits = styled("span")`
  padding-right: 2px;
`;

const TooltipEntries = (props: PropsType) => {
  if (
    typeof props.matchingEntries === "undefined" ||
    props.matchingEntries === null
  )
    return null;

  const { matchingEntries, dateRange } = props;

  const isZero = props.matchingEntries === 0;

  const dateFormat = T.translate("inputDateRange.dateFormat");
  const displayDateFormat = "yyyy-MM-dd";

  return (
    <Root className={props.className}>
      <div>
        <StyledFaIcon icon="bar-chart" />
        <Info>
          <Number zero={isZero}>
            {numberToThreeDigitArray(matchingEntries).map((threeDigits, i) => (
              <Digits key={i}>{threeDigits}</Digits>
            ))}
          </Number>
          <Text zero={isZero}>
            {T.translate(
              "tooltip.entriesFound",
              { context: matchingEntries } // For pluralization
            )}
          </Text>
        </Info>
      </div>
      {dateRange && (
        <ConceptDateRangeTooltip>
          <StyledFaIcon icon="calendar" />
          <Info>
            <DateContainer>
              <Date>{T.translate("tooltip.date.from") + ":"}</Date>
              <Date>{T.translate("tooltip.date.to") + ":"}</Date>
            </DateContainer>
            <DateContainer>
              <Date>
                {formatDate(
                  parseDate(dateRange.min, displayDateFormat),
                  dateFormat
                )}
              </Date>
              <Date>
                {formatDate(
                  parseDate(dateRange.max, displayDateFormat),
                  dateFormat
                )}
              </Date>
            </DateContainer>
            <Text>{T.translate("tooltip.date.daterange")}</Text>
          </Info>
        </ConceptDateRangeTooltip>
      )}
    </Root>
  );
};

export default TooltipEntries;
