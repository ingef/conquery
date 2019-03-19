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

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
`;
const Root = styled(Row)`
  padding-left: 20px;
`;

const Date = styled("p")`
  margin: 0;
  padding-right: 6px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const ConceptDateRangeTooltip = styled(Row)`
  margin: 0 40px 0 25px;
`;

const Text = styled("p")`
  margin: 0 0 5px;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, zero }) => (zero ? theme.col.red : "inherit")};
`;

const StyledFaIcon = styled(FaIcon)`
  padding-right: 15px;
`;

const Info = styled("div")`
  flex-shrink: 0;
`;

const Number = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme, zero }) => (zero ? theme.col.red : "inherit")};
`;

const Digits = styled("span")`
  padding-right: 2px;
`;

const Prefix = styled("span")`
  display: inline-block;
  width: 40px;
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
      <Row>
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
      </Row>
      {dateRange && (
        <ConceptDateRangeTooltip>
          <StyledFaIcon icon="calendar" />
          <Info>
            <Date>
              <Prefix>{T.translate("tooltip.date.from") + ":"}</Prefix>
              {formatDate(
                parseDate(dateRange.min, displayDateFormat),
                dateFormat
              )}
            </Date>
            <Date>
              <Prefix>{T.translate("tooltip.date.to") + ":"}</Prefix>
              {formatDate(
                parseDate(dateRange.max, displayDateFormat),
                dateFormat
              )}
            </Date>
          </Info>
        </ConceptDateRangeTooltip>
      )}
    </Root>
  );
};

export default TooltipEntries;
