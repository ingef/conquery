import {
  Table as ArrowTable,
  type AsyncRecordBatchStreamReader,
  type RecordBatch,
  type Vector,
} from "apache-arrow";
import RcTable from "rc-table";
import { type ComponentProps, memo, useMemo, useRef } from "react";
import { tv } from "tailwind-variants";
import type { GetQueryResponseDoneT, GetQueryResponseT } from "../api/types";
import { useCustomTableRenderers } from "./tableUtils";

interface Props {
  arrowReader: AsyncRecordBatchStreamReader;
  initialTableData: IteratorResult<RecordBatch>;
  queryData: GetQueryResponseT;
}

// flipped so the horizontal scrollbar sits on top; the inner table flips back
const root = tv({
  base: [
    "grow",
    "shadow-[0_0_10px_0_rgba(0,0,0,0.2)]",
    "[transform:rotateX(180deg)]",
    "[&_table]:[transform:rotateX(-180deg)]",
  ],
});

const table = tv({
  base: [
    "w-full",
    "border-spacing-0",
    "[&_th]:bg-gray-50 [&_th]:text-left [&_th]:font-normal",
    "[&_td]:max-w-[25ch] [&_td]:overflow-hidden [&_td]:text-ellipsis [&_td]:whitespace-nowrap",
    "[&_th]:p-[10px] [&_td]:p-[10px]",
    "[&_th]:border-r [&_th]:border-b [&_th]:border-gray-400",
    "[&_td]:border-r [&_td]:border-b [&_td]:border-gray-400",
    "[&_th:last-of-type]:border-r-0 [&_td:last-of-type]:border-r-0",
    "[&_.rc-table-measure-cell]:py-0 [&_.rc-table-measure-cell]:border-y-0",
    "[&_.rc-table-measure-cell-content]:invisible [&_.rc-table-measure-cell-content]:pointer-events-none",
    "[&_.rc-table-measure-cell-content]:h-0 [&_.rc-table-measure-cell-content]:overflow-hidden",
  ],
});

export const StyledTable = ({
  className,
  ...props
}: ComponentProps<"table">) => (
  <table className={table({ className })} {...props} />
);

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
            return <span title={rendered}>{rendered}</span>;
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
    <div ref={rootRef} className={root()}>
      <RcTable
        columns={columns}
        data={loadedTableData}
        rowKey={(_, index) => `previewtable_row_${index}`}
        components={{
          table: StyledTable,
        }}
        scroll={{ x: true }}
      />
    </div>
  );
});
