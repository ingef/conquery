import styled from "@emotion/styled";
import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
} from "../../api/types";
import FaIcon from "../../icon/FaIcon";
import { ContentFilterValue } from "../ContentControl";
import { DetailLevel } from "../DetailControl";
import { ColumnBuckets } from "../Timeline";
import { EntityEvent } from "../reducer";

import EventCard from "./EventCard";
import { SmallHeading } from "./SmallHeading";

const EventTimeline = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
`;
const EventItemList = styled("div")`
  width: calc(100% + 10px);
  margin-left: -10px;
`;

const VerticalLine = styled("div")`
  height: calc(100% - 20px);
  width: 2px;
  background-color: ${({ theme }) => theme.col.blueGrayVeryLight};
  margin: 10px 4px;
`;

const QuarterGroup = styled("div")``;
const QuarterHead = styled("div")<{ empty?: boolean }>`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, empty }) =>
    empty ? theme.col.grayLight : theme.col.gray};
  position: sticky;
  top: 0;
  z-index: 2;
  background-color: ${({ theme }) => theme.col.bgAlt};
  margin-left: -6px;
  line-height: 1;
  width: calc(100% + 8px);
`;

const InlineGrid = styled("div")`
  display: inline-grid;
  grid-template-columns: 20px 20px 110px 1fr;
  align-items: center;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 6px 10px;
  &:hover {
    border: 1px solid ${({ theme }) => theme.col.blueGray};
  }
`;

const Boxes = styled("div")`
  display: flex;
  align-items: center;
`;
const Box = styled("div")`
  width: 2px;
  height: 16px;
  margin-left: 1px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
`;

const SxSmallHeading = styled(SmallHeading)`
  line-height: 1;
`;

const Quarter = ({
  quarter,
  year,
  totalEventsPerQuarter,
  isOpen,
  detailLevel,
  groupedEvents,
  toggleOpenQuarter,
  differences,
  columns,
  dateColumn,
  sourceColumn,
  columnBuckets,
  currencyConfig,
  rootConceptIdsByColumn,
  contentFilter,
}: {
  year: number;
  quarter: number;
  totalEventsPerQuarter: number;
  isOpen: boolean;
  groupedEvents: EntityEvent[][];
  detailLevel: DetailLevel;
  toggleOpenQuarter: (year: number, quarter: number) => void;
  differences: string[][];
  columns: Record<string, ColumnDescription>;
  dateColumn: ColumnDescription;
  sourceColumn: ColumnDescription;
  columnBuckets: ColumnBuckets;
  contentFilter: ContentFilterValue;
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
}) => {
  const { t } = useTranslation();

  const areEventsShown =
    (isOpen || detailLevel !== "summary") && totalEventsPerQuarter > 0;

  return (
    <QuarterGroup key={quarter}>
      <QuarterHead empty={totalEventsPerQuarter === 0}>
        <InlineGrid onClick={() => toggleOpenQuarter(year, quarter)}>
          <FaIcon large gray icon={isOpen ? faCaretDown : faCaretRight} />
          <SxSmallHeading>Q{quarter} </SxSmallHeading>
          <span>
            – {totalEventsPerQuarter}{" "}
            {t("history.events", {
              count: totalEventsPerQuarter,
            })}
          </span>
          {detailLevel === "summary" && (
            <MemoizedBoxes totalEventsPerQuarter={totalEventsPerQuarter} />
          )}
        </InlineGrid>
      </QuarterHead>
      {areEventsShown && (
        <EventTimeline>
          <VerticalLine />
          <EventItemList>
            {groupedEvents.map((group, index) => {
              if (group.length === 0) return null;

              const groupDifferences = [
                ...new Set([
                  ...differences[index],
                  ...columnBuckets.concepts
                    .filter((c) => !!group[0][c.label])
                    .map((c) => c.label),
                ]),
              ];

              if (detailLevel === "full") {
                return group.map((evt, evtIdx) => (
                  <EventCard
                    key={`${index}-${evtIdx}`}
                    columns={columns}
                    dateColumn={dateColumn}
                    sourceColumn={sourceColumn}
                    columnBuckets={columnBuckets}
                    contentFilter={contentFilter}
                    rootConceptIdsByColumn={rootConceptIdsByColumn}
                    row={evt}
                    currencyConfig={currencyConfig}
                  />
                ));
              } else {
                const firstRowWithoutDifferences = Object.fromEntries(
                  Object.entries(group[0]).filter(([key]) => {
                    if (key === dateColumn.label) {
                      return true; // always show dates, despite it being part of groupDifferences
                    }

                    return !groupDifferences.includes(key);
                  }),
                ) as EntityEvent;

                return (
                  <EventCard
                    key={index}
                    columns={columns}
                    dateColumn={dateColumn}
                    sourceColumn={sourceColumn}
                    columnBuckets={columnBuckets}
                    contentFilter={contentFilter}
                    rootConceptIdsByColumn={rootConceptIdsByColumn}
                    row={firstRowWithoutDifferences}
                    currencyConfig={currencyConfig}
                    groupedRows={group}
                    groupedRowsKeysWithDifferentValues={groupDifferences}
                  />
                );
              }
            })}
          </EventItemList>
        </EventTimeline>
      )}
    </QuarterGroup>
  );
};

const MemoizedBoxes = memo(
  ({ totalEventsPerQuarter }: { totalEventsPerQuarter: number }) => {
    return (
      <Boxes>
        {new Array(totalEventsPerQuarter).fill(0).map((_, i) => (
          <Box key={i} />
        ))}
      </Boxes>
    );
  },
);

export default memo(Quarter);
