import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import NumberFormat from "react-number-format";

import type {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
  DatasetT,
} from "../../api/types";
import { exists } from "../../common/helpers/exists";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";
import type { ContentFilterValue } from "../ContentControl";
import { RowDates } from "../RowDates";
import type { EntityEvent } from "../reducer";

import GroupedContent from "./GroupedContent";
import { RawDataBadge } from "./RawDataBadge";
import { TinyLabel } from "./TinyLabel";

const Card = styled("div")`
  display: grid;
  grid-template-columns: auto 45px 1fr;
  gap: 3px;
  font-size: ${({ theme }) => theme.font.xs};
  padding: 5px 0;
  position: relative;
`;

const EventItemContent = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  box-shadow: 0 0 1px 1px ${({ theme }) => theme.col.grayLight};
  margin-top: 5px;
  background-color: white;
  overflow: hidden;
`;
const MainContent = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 15px 8px 8px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const ColBucket = styled("div")`
  color: black;
  padding: 1px 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 5px 10px;
`;

const Flex = styled("div")`
  display: flex;
  align-items: flex-start;
  gap: 5px;
`;

const SxRawDataBadge = styled(RawDataBadge)`
  position: absolute;
  z-index: 1;
  top: 4px;
  left: 55px;
`;

const SxFaIcon = styled(FaIcon)`
  width: 24px !important;
  text-align: center;
  margin: 8px 5px;
  font-size: ${({ theme }) => theme.font.md};
`;

const Bullet = styled("div")`
  width: 10px;
  height: 10px;
  margin: 2px 0;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 50%;
  flex-shrink: 0;
`;

interface ColumnBuckets {
  money: ColumnDescription[];
  concepts: ColumnDescription[];
  secondaryIds: ColumnDescription[];
  rest: ColumnDescription[];
}

interface Props {
  row: EntityEvent;
  columns: Record<string, ColumnDescription>;
  columnBuckets: ColumnBuckets;
  datasetId: DatasetT["id"];
  contentFilter: ContentFilterValue;
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  groupedRows?: EntityEvent[];
  groupedRowsKeysWithDifferentValues?: string[];
}

const EventCard = ({
  row,
  columns,
  columnBuckets,
  datasetId,
  currencyConfig,
  contentFilter,
  rootConceptIdsByColumn,
  groupedRows,
  groupedRowsKeysWithDifferentValues,
}: Props) => {
  const { t } = useTranslation();

  const applicableSecondaryIds = columnBuckets.secondaryIds.filter((column) =>
    exists(row[column.label]),
  );
  const secondaryIdsTooltip = t("history.content.secondaryId");

  const applicableMoney = columnBuckets.money.filter((column) =>
    exists(row[column.label]),
  );
  const moneyTooltip = t("history.content.money");

  const applicableRest = columnBuckets.rest.filter((column) =>
    exists(row[column.label]),
  );
  const restTooltip = t("history.content.rest");

  return (
    <Card>
      <Bullet />
      <RowDates dates={row.dates} />
      <SxRawDataBadge event={row} />
      <EventItemContent>
        <MainContent>
          {contentFilter.money && applicableMoney.length > 0 && (
            <Flex>
              <WithTooltip text={moneyTooltip}>
                <span>
                  <SxFaIcon icon="euro-sign" active large />
                </span>
              </WithTooltip>
              <ColBucket>
                {applicableMoney.map((column) => (
                  <div>
                    <TinyLabel>{column.defaultLabel}</TinyLabel>
                    <code>
                      <NumberFormat
                        thousandSeparator={currencyConfig.thousandSeparator}
                        decimalSeparator={currencyConfig.decimalSeparator}
                        decimalScale={currencyConfig.decimalScale}
                        suffix={currencyConfig.prefix}
                        displayType="text"
                        value={parseInt(row[column.label]) / 100}
                      />
                    </code>
                  </div>
                ))}
              </ColBucket>
            </Flex>
          )}
          {contentFilter.rest && applicableRest.length > 0 && (
            <Flex>
              <WithTooltip text={restTooltip}>
                <span>
                  <SxFaIcon icon="info" active large />
                </span>
              </WithTooltip>
              <ColBucket>
                {applicableRest.map((column) => (
                  <div>
                    <TinyLabel>{column.defaultLabel}</TinyLabel>
                    <span>{row[column.label]}</span>
                  </div>
                ))}
              </ColBucket>
            </Flex>
          )}
          {contentFilter.secondaryId && applicableSecondaryIds.length > 0 && (
            <Flex>
              <WithTooltip text={secondaryIdsTooltip}>
                <span>
                  <SxFaIcon icon="microscope" active large />
                </span>
              </WithTooltip>
              <ColBucket>
                {applicableSecondaryIds.map((column) => (
                  <div>
                    <TinyLabel>{column.defaultLabel}</TinyLabel>
                    {row[column.label]}
                  </div>
                ))}
              </ColBucket>
            </Flex>
          )}
        </MainContent>
        {groupedRowsKeysWithDifferentValues && groupedRows && (
          <GroupedContent
            datasetId={datasetId}
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
      </EventItemContent>
    </Card>
  );
};

export default EventCard;
