import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { numberToThreeDigitArray } from "../common/helpers/commonHelper";
import { formatDate, parseDate } from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";

type PropsType = {
  className?: string;
  matchingEntries?: number | null;
  matchingEntities?: number | null;
  dateRange?: Object | null;
};

const Root = styled("div")``;
const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-bottom: 10px;
`;

const Date = styled("p")`
  margin: 0;
  padding-right: 8px;
  font-size: ${({ theme }) => theme.font.sm};
  letter-spacing: 1px;
  font-weight: 700;
`;

const ConceptDateRangeTooltip = styled(Row)``;

const Text = styled("p")<{ zero?: boolean }>`
  margin: 0 0 5px;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, zero }) => (zero ? theme.col.red : theme.col.gray)};
  text-transform: uppercase;
  font-weight: 400;
`;

const StyledFaIcon = styled(FaIcon)`
  font-size: 38px;
  color: ${({ theme }) => theme.col.grayMediumLight};
`;

const EntitiesIcon = styled(StyledFaIcon)`
  padding-right: 24px;
`;
const StatsIcon = styled(StyledFaIcon)`
  padding-right: 15px;
`;
const CalIcon = styled(StyledFaIcon)`
  padding-right: 20px;
`;

const Info = styled("div")`
  flex-shrink: 0;
`;

const Number = styled("p")<{ zero?: boolean }>`
  margin: 0;
  font-size: ${({ theme }) => theme.font.lg};
  color: ${({ theme, zero }) => (zero ? theme.col.red : "inherit")};
  font-weight: 700;
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
  letter-spacing: 0;
  font-weight: 400;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
  margin-left: 5px;
`;

const TooltipEntries = (props: PropsType) => {
  const { t } = useTranslation();
  const { matchingEntries, matchingEntities, dateRange } = props;

  const isZero = props.matchingEntries === 0;
  const isZeroEntities = props.matchingEntities === 0;

  const dateFormat = "yyyy-MM-dd";
  const displayDateFormat = t("inputDateRange.dateFormat");

  return (
    <Root className={props.className}>
      <Row>
        <StatsIcon icon="chart-bar" />
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
      </Row>
      <Row>
        <EntitiesIcon icon="id-badge" />
        <Info>
          <Number zero={isZeroEntities}>
            {exists(matchingEntities) ? (
              numberToThreeDigitArray(matchingEntities).map(
                (threeDigits, i) => <Digits key={i}>{threeDigits}</Digits>,
              )
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
      </Row>
      <ConceptDateRangeTooltip>
        <CalIcon regular icon="calendar" />
        <Info>
          <Date>
            {dateRange && dateRange.min
              ? formatDate(
                  parseDate(dateRange.min, dateFormat),
                  displayDateFormat,
                )
              : "- - - - - - - -"}
            <Suffix>{`${t("tooltip.date.from")}`}</Suffix>
          </Date>
          <Date>
            {dateRange && dateRange.max
              ? formatDate(
                  parseDate(dateRange.max, dateFormat),
                  displayDateFormat,
                )
              : "- - - - - - - -"}
            <Suffix>{`${t("tooltip.date.to")}`}</Suffix>
          </Date>
        </Info>
      </ConceptDateRangeTooltip>
    </Root>
  );
};

export default TooltipEntries;
