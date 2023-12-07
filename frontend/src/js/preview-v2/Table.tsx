import styled from "@emotion/styled";
import { Table as ArrowTable } from "apache-arrow";
import RcTable from "rc-table";
import { useEffect, useRef, useState } from "react";

interface Props {
  data: ArrowTable;
}

const Root = styled("div")`
  flex-grow: 1;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
`;

export const StyledTable = styled("table")`
  width: 100%;
  border-spacing: 0;

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

  const rootRef = useRef<HTMLDivElement>(null);
  const tableData = data.toArray();
  const stepCount = 50;
  const [loadingAmount, setLoadingAmount] = useState(stepCount);

  useEffect(() => {
    const eventFunction = () => {
      const div = rootRef.current;
      if (!div) {
        return;
      }

      const scrollAmount = (div.getBoundingClientRect().y - div.offsetTop) * -1;
      const maxScroll = div.getBoundingClientRect().height - div.offsetTop;
      const thresholdTriggered = scrollAmount / maxScroll > 0.9;
      if (thresholdTriggered) {
        setLoadingAmount((amount) =>
          Math.min(amount + stepCount, tableData.length),
        );
      }
    };

    window.addEventListener("scroll", eventFunction, true);
    return () => window.removeEventListener("scroll", eventFunction, true);
  }, [tableData.length]);

  return (
    <Root ref={rootRef}>
      <RcTable
        columns={columns}
        data={tableData.slice(0, loadingAmount)}
        rowKey={(_, index) => `row_${index}`}
        components={components}
      />
    </Root>
  );
}
