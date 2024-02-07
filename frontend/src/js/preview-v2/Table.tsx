import styled from "@emotion/styled";
import { Table as ArrowTable, Vector } from "apache-arrow";
import RcTable from "rc-table";
import { DefaultRecordType } from "rc-table/lib/interface";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { GetQueryResponseDoneT, GetQueryResponseT } from "../api/types";
import { useCustomTableRenderers } from "./tableUtils";

interface Props {
  data: ArrowTable;
  queryData: GetQueryResponseT;
}

const Root = styled("div")`
  flex-grow: 1;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
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

export default function Table({ data, queryData }: Props) {
  const rootRef = useRef<HTMLDivElement>(null);
  const { getRenderFunctionByFieldName } = useCustomTableRenderers(
    queryData as GetQueryResponseDoneT,
  );
  const tableData = useMemo(() => data.toArray(), [data]);

  const columns = useMemo(
    () =>
      data.schema.fields.map((field) => ({
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
    [data.schema.fields],
  );

  // parse rows outside of rc-table to cache them
  const getNextTableRows = useCallback(
    (startIndex: number = 0) => {
      const stepCount = 50;
      const nextRows = [] as DefaultRecordType[];
      tableData
        .slice(startIndex, startIndex + stepCount)
        .forEach((dataEntry: Vector) => {
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
    [tableData, getRenderFunctionByFieldName],
  );

  const [loadedTableData, setLoadedTableData] = useState(getNextTableRows());

  useEffect(() => {
    const eventFunction = () => {
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
        setLoadedTableData([
          ...loadedTableData,
          ...getNextTableRows(loadedTableData.length),
        ]);
      }
    };

    window.addEventListener("scroll", eventFunction, true);
    return () => window.removeEventListener("scroll", eventFunction, true);
  }, [loadedTableData, getNextTableRows]);

  return (
    <Root ref={rootRef}>
      <RcTable
        columns={columns}
        data={loadedTableData}
        rowKey={(_, index) => `previewtable_row_${index}`}
        components={{
          table: StyledTable,
        }}
        scroll={{ x: true }}
      />
    </Root>
  );
}
