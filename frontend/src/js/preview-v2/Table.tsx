import styled from "@emotion/styled";
import { Table as ArrowTable } from "apache-arrow";
import RcTable from "rc-table";

interface Props {
  data: ArrowTable;
}

const Root = styled("div")`
  flex-grow: 1;
  min-height: 500px;
  position: relative;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
  overflow: auto;
`;

const StyledTable = styled("table")`
  width: 100%;
  border-spacing: 0;

  thead {
    position: sticky;
    top: 0;
  }

  th {
    padding: 10px;
    background: ${({ theme }) => theme.col.grayVeryLight};
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};
    border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
    font-weight: normal;
    text-align: left;
  }
  th:last-of-type {
    border-right: none;
  }

  td {
    padding: 10px;
    border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
  }
`;

export default function Table({ data }: Props) {
  const components = {
    table: StyledTable,
  };

  const columns = data.schema.fields.map((field) => ({
    title: field.name.charAt(0).toUpperCase() + field.name.slice(1),
    dataIndex: field.name,
    key: field.name,
  }));

  return (
    <Root>
      <RcTable
        columns={columns}
        data={data.toArray()}
        rowKey={(_, index) => `row_${index}`}
        components={components}
      />
    </Root>
  );
}
