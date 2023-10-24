import { Table as arrowTable } from 'apache-arrow';
import { ColumnType } from "rc-table";
import RcTable from "rc-table";

interface Props {
  columns: ColumnType<any>[];
  data: arrowTable;
}

export default function Table({
  data,
  columns,
}: Props) {
  console.log(data.slice(0, data.numRows).toArray());

  return (
    <RcTable
      columns={columns}
      data={data.toArray()}
    />
  )
}
