import styled from "@emotion/styled";
import {
  Table as ArrowTable,
  AsyncRecordBatchStreamReader,
  RecordBatch,
  Vector,
} from "apache-arrow";
import RcTable from "rc-table";
import { DefaultRecordType } from "rc-table/lib/interface";
import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { GetQueryResponseDoneT, GetQueryResponseT } from "../api/types";
import { useCustomTableRenderers } from "./tableUtils";

interface Props {
  arrowReader: AsyncRecordBatchStreamReader;
  initialTableData: IteratorResult<RecordBatch>;
  queryData: GetQueryResponseT;
}

const Root = styled("div")`
  flex-grow: 1;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
  transform: rotateX(180deg);

  table {
    transform: rotateX(-180deg);
  }
`;

export const StyledTable = styled("table")`
  width: 100%;
  border-spacing: 0;

  th {
    background: ${({ theme }) => theme.col.grayVeryLight};
    font-weight: normal;
    text-align: left;
  }

  td {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 25ch;
  }

  th,
  td {
    padding: 10px;
    border-bottom: 1px solid ${({ theme }) => theme.col.grayMediumLight};
    border-right: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  }

  th:last-of-type,
  td:last-of-type {
    border-right: none;
  }
`;

export default memo(function Table({
  arrowReader,
  initialTableData,
  queryData,
}: Props) {
  const rootRef = useRef<HTMLDivElement>(null);
  const { getRenderFunctionByFieldName } = useCustomTableRenderers(
    queryData as GetQueryResponseDoneT,
  );

  const columns = useMemo(
    () =>
      arrowReader.schema?.fields.map((field) => ({
        title: field.name.charAt(0).toUpperCase() + field.name.slice(1),
        dataIndex: field.name,
        key: field.name,
        render: (value: string | Vector) => {
          return typeof value === "string" ? (
            <span title={value as string}>{value}</span>
          ) : (
            value
          );
        },
      })),
    [arrowReader.schema],
  );

  const parseTableRows = useCallback(
    (data: Vector[]) => {
      const nextRows = [] as DefaultRecordType[];
      data.forEach((dataEntry: Vector) => {
        const parsedValues = Object.fromEntries(
          Object.entries(dataEntry.toJSON()).map(([key, value]) => {
            const parsedValue =
              getRenderFunctionByFieldName(key)?.(value) ?? value;
            return [key, parsedValue];
          }),
        );
        nextRows.push(parsedValues);
      });

      return nextRows;
    },
    [getRenderFunctionByFieldName],
  );

  const loadedTableData = useMemo(
    () => parseTableRows(new ArrowTable(initialTableData.value).toArray()),
    [initialTableData, parseTableRows],
  );
  const [visibleTableRows, setVisibleTableRows] = useState(50);

  useEffect(() => {
    const eventFunction = async () => {
      const div = rootRef.current;
      if (!div) {
        return;
      }
      const maxScroll =
        (div.parentElement?.scrollHeight || div.scrollHeight) -
        window.innerHeight;
      const thresholdTriggered =
        (div.parentElement?.scrollTop || div.scrollTop) / maxScroll > 0.9;
      if (thresholdTriggered) {
        setVisibleTableRows((rowCount) =>
          Math.min(rowCount + 50, loadedTableData.length),
        );
      }
    };

    window.addEventListener("scroll", eventFunction, true);
    return () => window.removeEventListener("scroll", eventFunction, true);
  }, [loadedTableData, visibleTableRows, arrowReader]);

  return (
    <Root ref={rootRef}>
      <RcTable
        columns={columns}
        data={loadedTableData.slice(0, visibleTableRows)}
        rowKey={(_, index) => `previewtable_row_${index}`}
        components={{
          table: StyledTable,
        }}
        scroll={{ x: true }}
      />
    </Root>
  );
});