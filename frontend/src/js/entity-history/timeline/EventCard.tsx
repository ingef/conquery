import {
  faEuroSign,
  faFingerprint,
  faInfo,
} from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { type InputAttributes, NumericFormat } from "react-number-format";
import { tv } from "tailwind-variants";
import type {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
} from "../../api/types";
import { Highlighter } from "../../common/components/Highlighter";
import { exists } from "../../common/helpers/exists";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";
import type { ContentFilterValue } from "../ContentControl";
import { RowDates } from "../RowDates";
import type { DateRow, EntityEvent } from "../reducer";
import { useTimelineSearch } from "../timeline-search/timelineSearchState";
import GroupedContent from "./GroupedContent";
import { RawDataBadge } from "./RawDataBadge";
import { TinyLabel } from "./TinyLabel";
import type { ColumnBuckets } from "./util/useColumnInformation";
import { isDateColumn, isSourceColumn } from "./util/util";

const card = tv({
  base: [
    "relative",
    "grid grid-cols-[auto_45px_1fr]",
    "gap-[3px]",
    "py-[5px]",
    "text-xs",
  ],
});

const eventItemContent = tv({
  base: [
    "mt-[5px]",
    "rounded",
    "shadow-[0_0_1px_1px_var(--color-gray-100)]",
    "bg-white",
    "overflow-hidden",
    "[&>div:first-of-type]:pt-[14px]",
  ],
});

const colBucket = tv({
  base: [
    "grid grid-cols-3 min-[1800px]:grid-cols-4 min-[2500px]:grid-cols-5",
    "gap-x-[10px] gap-y-[3px]",
    "w-full",
    "px-1 py-px",
    "text-black",
  ],
});

const flex = tv({
  base: [
    "flex items-start",
    "gap-[5px]",
    "pt-3 pr-[15px] pb-[10px] pl-[6px]",
    "text-sm",
  ],
});

const rawDataBadge = tv({
  base: ["absolute top-[4px] left-[55px]", "z-1"],
});

const bucketIcon = tv({
  // w-6! beats FaIcon's own w-[initial]! via merge, like the old !important did
  base: ["w-6!", "text-center", "mx-[5px] my-2", "text-base"],
});

const bullet = tv({
  base: [
    "h-[10px] w-[10px]",
    "my-[2px]",
    "bg-primary-500",
    "rounded-full",
    "shrink-0",
  ],
});

const EventCard = ({
  row,
  columns,
  dateColumn,
  sourceColumn,
  columnBuckets,
  currencyConfig,
  contentFilter,
  rootConceptIdsByColumn,
  groupedRows,
  groupedRowsKeysWithDifferentValues,
}: {
  row: EntityEvent;
  columns: Record<string, ColumnDescription>;
  dateColumn: ColumnDescription;
  sourceColumn: ColumnDescription;
  columnBuckets: ColumnBuckets;
  contentFilter: ContentFilterValue;
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  groupedRows?: EntityEvent[];
  groupedRowsKeysWithDifferentValues?: string[];
}) => {
  const { searchTerm } = useTimelineSearch();
  const { t } = useTranslation();

  const applicableGroupableIds = columnBuckets.groupableIds.filter(
    (column) =>
      exists(row[column.label]) &&
      !isDateColumn(column) && // Because they're already displayed somewhere else
      !isSourceColumn(column), // Because they're already displayed somewhere else
  );
  const groupableIdsTooltip = t("history.content.fingerprint");

  const applicableMoney = columnBuckets.money.filter((column) =>
    exists(row[column.label]),
  );
  const moneyTooltip = t("history.content.money");

  const applicableRest = columnBuckets.rest.filter((column) =>
    exists(row[column.label]),
  );
  const restTooltip = t("history.content.rest");

  return (
    <div className={card()}>
      <div className={bullet()} />
      <RowDates dates={row[dateColumn.label] as DateRow} />
      <RawDataBadge
        className={rawDataBadge()}
        event={row}
        sourceColumn={sourceColumn}
      />
      <div className={eventItemContent()}>
        {contentFilter.money && applicableMoney.length > 0 && (
          <div className={flex()}>
            <WithTooltip text={moneyTooltip}>
              <span>
                <FaIcon
                  className={bucketIcon()}
                  icon={faEuroSign}
                  active
                  large
                />
              </span>
            </WithTooltip>
            <div className={colBucket()}>
              {applicableMoney.map((column) => (
                <div key={column.label}>
                  <TinyLabel>{column.defaultLabel}</TinyLabel>
                  <code>
                    <NumericFormat<InputAttributes>
                      thousandSeparator={currencyConfig.thousandSeparator}
                      decimalSeparator={currencyConfig.decimalSeparator}
                      decimalScale={currencyConfig.decimalScale}
                      suffix={` ${currencyConfig.unit}`}
                      displayType="text"
                      value={parseFloat(row[column.label] as string)}
                    />
                  </code>
                </div>
              ))}
            </div>
          </div>
        )}
        {groupedRowsKeysWithDifferentValues && groupedRows && (
          <GroupedContent
            columns={columns}
            contentFilter={contentFilter}
            groupedRows={groupedRows}
            groupedRowsKeysWithDifferentValues={
              groupedRowsKeysWithDifferentValues
            }
            currencyConfig={currencyConfig}
            rootConceptIdsByColumn={rootConceptIdsByColumn}
          />
        )}
        {contentFilter.rest && applicableRest.length > 0 && (
          <div className={flex()}>
            <WithTooltip text={restTooltip}>
              <span>
                <FaIcon className={bucketIcon()} icon={faInfo} active large />
              </span>
            </WithTooltip>
            <div className={colBucket()}>
              {applicableRest.map((column) => (
                <div key={column.label}>
                  <TinyLabel>{column.defaultLabel}</TinyLabel>
                  <span>
                    {searchTerm && searchTerm.length > 0 ? (
                      <Highlighter
                        searchWords={searchTerm.split(" ")}
                        textToHighlight={row[column.label] as string}
                      />
                    ) : (
                      (row[column.label] as string)
                    )}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
        {contentFilter.groupId && applicableGroupableIds.length > 0 && (
          <div className={flex()}>
            <WithTooltip text={groupableIdsTooltip}>
              <span>
                <FaIcon
                  className={bucketIcon()}
                  icon={faFingerprint}
                  active
                  large
                />
              </span>
            </WithTooltip>
            <div className={colBucket()}>
              {applicableGroupableIds.map((column) => (
                <div key={column.label}>
                  <TinyLabel>{column.defaultLabel}</TinyLabel>
                  <span>
                    {searchTerm && searchTerm.length > 0 ? (
                      <Highlighter
                        searchWords={searchTerm.split(" ")}
                        textToHighlight={row[column.label] as string}
                      />
                    ) : (
                      (row[column.label] as string)
                    )}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default EventCard;
