import styled from "@emotion/styled";
import {
  faArrowsLeftRightToLine,
  faHashtag,
  faUser,
} from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import { HTMLAttributes } from "react";
import type { DateRangeT } from "../api/types";
import { numberToThreeDigitArray } from "../common/helpers/commonHelper";
import { formatDate, parseDate } from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";

const Root = styled("div")``;

const Date = styled("p")`
  margin: 0;
  padding-right: 8px;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.sm};
  display: flex;
  align-items: center;
  white-space: nowrap;
`;

const Text = styled("p")<{ zero?: boolean }>`
  margin: 0;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, zero }) => (zero ? theme.col.red : theme.col.gray)};
  text-transform: uppercase;
  font-weight: 400;
`;

const StyledFaIcon = styled(FaIcon)`
  font-size: 30px;
  color: ${({ theme }) => theme.col.grayMediumLight};
  justify-self: center;
`;

const Info = styled("div")`
  flex-shrink: 0;
`;

const Number = styled("p")<{ zero?: boolean }>`
  font-weight: 700;
  margin: 0;
  font-size: ${({ theme }) => theme.font.lg};
  color: ${({ theme, zero }) => (zero ? theme.col.red : "inherit")};
  line-height: 1;
`;

const Digits = styled("span")`
  &:after {
    color: ${({ theme }) => theme.col.gray};
    content: ".";
  }
  &:last-of-type {
    &:after {
      content: "";
    }
  }
`;

const Suffix = styled("span")`
  color: ${({ theme }) => theme.col.gray};
  font-weight: 400;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
  margin-left: 5px;
`;

interface Props extends HTMLAttributes<HTMLDivElement> {
  matchingEntries?: number | null;
  matchingEntities?: number | null;
  dateRange?: DateRangeT;
}

const TooltipEntries = (props: Props) => {
  const { t } = useTranslation();
  const { matchingEntries, matchingEntities, dateRange } = props;

  const isZero = props.matchingEntries === 0;
  const isZeroEntities = props.matchingEntities === 0;

  const dateFormat = "yyyy-MM-dd";
  const displayDateFormat = t("inputDateRange.dateFormat");

  const parsedFromDate =
    dateRange && dateRange.min ? parseDate(dateRange.min, dateFormat) : null;
  const fromDate = parsedFromDate
    ? formatDate(parsedFromDate, displayDateFormat)
    : "- - - - - - -";

  const parsedToDate =
    dateRange && dateRange.max ? parseDate(dateRange.max, dateFormat) : null;
  const toDate = parsedToDate
    ? formatDate(parsedToDate, displayDateFormat)
    : "- - - - - - -";

  return (
    <Root {...props}>
      <StyledFaIcon icon={faHashtag} />
      <Info>
        <Number zero={isZero}>
          {exists(matchingEntries) ? (
            numberToThreeDigitArray(matchingEntries).map((threeDigits, i) => (
              <Digits key={i}>{threeDigits}</Digits>
            ))
          ) : (
            <Digits>-</Digits>
          )}
        </Number>
        <Text zero={isZero}>
          {t(
            "tooltip.entriesFound",
            { count: matchingEntries || 2 }, // For pluralization
          )}
        </Text>
      </Info>
      <StyledFaIcon icon={faUser} />
      <Info>
        <Number zero={isZeroEntities}>
          {exists(matchingEntities) ? (
            numberToThreeDigitArray(matchingEntities).map((threeDigits, i) => (
              <Digits key={i}>{threeDigits}</Digits>
            ))
          ) : (
            <Digits>-</Digits>
          )}
        </Number>
        <Text zero={isZeroEntities}>
          {t(
            "tooltip.entitiesFound",
            { count: matchingEntities || 2 }, // For pluralization
          )}
        </Text>
      </Info>
      <StyledFaIcon icon={faArrowsLeftRightToLine} />
      <Info>
        <Date>
          {fromDate}
          <Suffix>{`${t("tooltip.date.from")}`}</Suffix>
        </Date>
        <Date>
          {toDate}
          <Suffix>{`${t("tooltip.date.to")}`}</Suffix>
        </Date>
      </Info>
    </Root>
  );
};

export default TooltipEntries;
