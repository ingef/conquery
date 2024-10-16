import styled from "@emotion/styled";
import {Fragment, memo} from "react";
import {useSelector} from "react-redux";

import {CurrencyConfigT, EntityInfo, TimeStratifiedInfo} from "../api/types";
import type {StateT} from "../app/reducers";

import {ContentFilterValue} from "./ContentControl";
import type {DetailLevel} from "./DetailControl";
import {EntityCard} from "./EntityCard";
import type {EntityHistoryStateT} from "./reducer";
import {TimelineSearch} from "./timeline-search/TimelineSearch";
import {useTimelineSearch} from "./timeline-search/timelineSearchState";
import {TimelineEmptyPlaceholder} from "./timeline/TimelineEmptyPlaceholder";
import Year from "./timeline/Year";
import {useColumnInformation} from "./timeline/util/useColumnInformation";
import {useTimeBucketedSortedData} from "./timeline/util/useTimeBucketedSortedData";

const Root = styled("div")<{ isEmpty?: boolean }>`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px 20px 10px;
  display: inline-grid;
  grid-template-columns: 280px auto;
  grid-auto-rows: ${({isEmpty}) =>
    isEmpty ? "1fr" : "minmax(min-content, max-content) 1fr"};
  gap: 20px 4px;
  width: 100%;
  height: 100%;
`;

const Divider = styled("div")`
  grid-column: 1 / span 2;
  height: 1px;
  background: ${({theme}) => theme.col.grayLight};
`;

const SxTimelineEmptyPlaceholder = styled(TimelineEmptyPlaceholder)`
  grid-column: span 2;
  height: 100%;
`;

export const Timeline = memo(
    ({
         className,
         currentEntityInfos,
         currentEntityTimeStratifiedInfos,
         detailLevel,
         sources,
         contentFilter,
         getIsOpen,
         toggleOpenYear,
         toggleOpenQuarter,
         blurred,
     }: {
        className?: string;
        currentEntityInfos: EntityInfo[];
        currentEntityTimeStratifiedInfos: TimeStratifiedInfo[];
        detailLevel: DetailLevel;
        sources: Set<string>;
        contentFilter: ContentFilterValue;
        getIsOpen: (year: number, quarter?: number) => boolean;
        toggleOpenYear: (year: number) => void;
        toggleOpenQuarter: (year: number, quarter: number) => void;
        blurred?: boolean;
    }) => {
        const data = useSelector<StateT, EntityHistoryStateT["currentEntityData"]>(
            (state) => state.entityHistory.currentEntityData,
        );
        const currencyConfig = useSelector<StateT, CurrencyConfigT>(
            (state) => state.startup.config.currency,
        );

        const {searchTerm} = useTimelineSearch();

        const {
            columns,
            dateColumn,
            sourceColumn,
            columnBuckets,
            rootConceptIdsByColumn,
        } = useColumnInformation();

        const {matches, eventsByQuarterWithGroups} = useTimeBucketedSortedData(
            data,
            {
                columnBuckets,
                rootConceptIdsByColumn,
                sourceColumn,
                dateColumn,
                sources,
                secondaryIds: columnBuckets.secondaryIds,
            },
        );

        const isEmpty =
            eventsByQuarterWithGroups.length === 0 || !dateColumn || !sourceColumn;

        return (
            <div className="overflow-hidden w-full flex flex-col">
                <TimelineSearch matches={matches}/>
                <Root className={className} isEmpty={isEmpty}>
                    {!isEmpty && !searchTerm && (
                        <EntityCard
                            className="col-span-2"
                            blurred={blurred}
                            infos={currentEntityInfos}
                            timeStratifiedInfos={currentEntityTimeStratifiedInfos}
                        />
                    )}
                    {isEmpty && <SxTimelineEmptyPlaceholder searchTerm={searchTerm}/>}
                    {dateColumn &&
                        sourceColumn &&
                        eventsByQuarterWithGroups.map(({year, quarterwiseData}, i) => (
                            <Fragment key={year}>
                                <Year
                                    year={year}
                                    quarterwiseData={quarterwiseData}
                                    timeStratifiedInfos={currentEntityTimeStratifiedInfos}
                                    getIsOpen={getIsOpen}
                                    toggleOpenYear={toggleOpenYear}
                                    toggleOpenQuarter={toggleOpenQuarter}
                                    detailLevel={detailLevel}
                                    currencyConfig={currencyConfig}
                                    rootConceptIdsByColumn={rootConceptIdsByColumn}
                                    columnBuckets={columnBuckets}
                                    contentFilter={contentFilter}
                                    columns={columns}
                                    dateColumn={dateColumn}
                                    sourceColumn={sourceColumn}
                                />
                                {i < eventsByQuarterWithGroups.length - 1 && <Divider/>}
                            </Fragment>
                        ))}
                </Root>
            </div>
        );
    },
);
