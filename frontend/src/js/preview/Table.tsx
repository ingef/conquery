import styled from "@emotion/styled";
import {
  Table as ArrowTable,
  AsyncRecordBatchStreamReader,
  RecordBatch,
  Vector,
} from "apache-arrow";
import RcTable from "rc-table";
import { memo, useMemo, useRef } from "react";
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
      arrowReader.schema?.fields.map((field) => {
        const renderer = getRenderFunctionByFieldName(field.name);

        return {
          title: field.name.charAt(0).toUpperCase() + field.name.slice(1),
          dataIndex: field.name,
          key: field.name,
          render: (value: string | Vector) => {
            const rendered = renderer(value);
            return <span title={rendered as string}>{rendered}</span>;
          },
        };
      }),
    [arrowReader.schema, getRenderFunctionByFieldName],
  );

  const loadedTableData = useMemo(
    () => new ArrowTable(initialTableData.value).toArray(),
    [initialTableData],
  );

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
});
