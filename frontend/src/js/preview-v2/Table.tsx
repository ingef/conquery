import styled from "@emotion/styled";
import { Table as ArrowTable, Vector } from "apache-arrow";
import RcTable from "rc-table";
import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import {
  CurrencyConfigT,
  GetQueryResponseDoneT,
  GetQueryResponseT,
} from "../api/types";
import { StateT } from "../app/reducers";
import { NUMBER_TYPES, formatDate, formatNumber } from "./util";

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
    padding: 10px;
    background: ${({ theme }) => theme.col.grayVeryLight};
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};
    border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
    font-weight: normal;
    text-align: left;
  }

  td {
    padding: 10px;
    border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};

    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 45ch;
  }

  th:last-of-type,
  td:last-of-type {
    border-right: none;
  }
`;

export default function Table({ data, queryData }: Props) {
  const components = {
    table: StyledTable,
  };

  const { t } = useTranslation();
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );

  function getRenderFunction(
    cellType: string,
  ): ((value: string | Vector) => string) | undefined {
    if (cellType.indexOf("LIST") == 0) {
      const listType = cellType.match(/LIST\[(?<listtype>.*)\]/)?.groups?.[
        "listtype"
      ];
      if (listType) {
        const listTypeRenderFunction = getRenderFunction(listType);
        return (value) =>
          value
            ? (value as Vector)
                .toArray()
                .map((listItem: string) =>
                  listTypeRenderFunction
                    ? listTypeRenderFunction(listItem)
                    : listItem,
                )
                .join(", ")
            : null;
      }
    } else if (NUMBER_TYPES.includes(cellType)) {
      return (value) => {
        const num = parseFloat(value as string);
        return isNaN(num) ? value.toString() : formatNumber(num);
      };
    } else if (cellType == "DATE") {
      return (value) => formatDate(value as string);
    } else if (cellType == "DATE_RANGE") {
      return (value) => {
        const dateRange = (value as Vector).toJSON() as unknown as {
          min: Date;
          max: Date;
        };
        const min = dateRange.min.toLocaleDateString();
        const max = dateRange.max.toLocaleDateString();
        return min == max ? min : `${min} - ${max}`;
      };
    } else if (cellType == "MONEY") {
      return (value) => {
        const num = parseFloat(value as string);
        return isNaN(num)
          ? value.toString()
          : `${formatNumber(num)} ${currencyConfig.unit}`;
      };
    } else if (cellType == "BOOLEAN") {
      return (value) => (value ? t("common.true") : t("common.false"));
    }
  }

  const getRenderFunctionByFieldName = (
    fieldName: string,
  ): ((value: string | Vector) => string) | undefined => {
    const cellType = (
      queryData as GetQueryResponseDoneT
    ).columnDescriptions?.find((x) => x.label == fieldName)?.type;
    if (cellType) {
      return getRenderFunction(cellType);
    }
  };

  const columns = data.schema.fields.map((field) => ({
    title: field.name.charAt(0).toUpperCase() + field.name.slice(1),
    dataIndex: field.name,
    key: field.name,
    render: (value: string | Vector) => {
      const rendered = getRenderFunctionByFieldName(field.name)?.(value);
      return rendered ? <span title={rendered}>{rendered}</span> : value;
    },
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
      const maxScroll =
        (div.parentElement?.scrollHeight || div.scrollHeight) -
        window.innerHeight;
      const thresholdTriggered =
        (div.parentElement?.scrollTop || div.scrollTop) / maxScroll > 0.9;
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
        scroll={{ x: true }}
      />
    </Root>
  );
}
