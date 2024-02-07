import { Vector } from "apache-arrow";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { CurrencyConfigT, GetQueryResponseDoneT } from "../api/types";
import { StateT } from "../app/reducers";
import {
  NUMBER_TYPES,
  formatDate,
  formatNumber,
  toFullLocaleDateString,
} from "./util";

export type CellValue = string | Vector;

export function useCustomTableRenderers(queryData: GetQueryResponseDoneT) {
  const { t } = useTranslation();
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );

  const getRenderFunction = useCallback(
    (cellType: string): ((value: CellValue) => string) | undefined => {
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
          return isNaN(num) ? "" : formatNumber(num);
        };
      } else if (cellType == "DATE") {
        return (value) =>
          value instanceof Date
            ? toFullLocaleDateString(value)
            : formatDate(value as string);
      } else if (cellType == "DATE_RANGE") {
        return (value) => {
          const dateRange = (value as Vector).toJSON() as unknown as {
            min: Date;
            max: Date;
          };
          const min = toFullLocaleDateString(dateRange.min);
          const max = toFullLocaleDateString(dateRange.max);
          return min == max ? min : `${min} - ${max}`;
        };
      } else if (cellType == "MONEY") {
        return (value) => {
          // parse cent string
          const num = parseFloat(value as string) / 100;
          return isNaN(num)
            ? ""
            : `${formatNumber(num)} ${currencyConfig.unit}`;
        };
      } else if (cellType == "BOOLEAN") {
        return (value) => (value ? t("common.true") : t("common.false"));
      }
    },
    [currencyConfig.unit, t],
  );

  const getRenderFunctionByFieldName = useCallback(
    (fieldName: string): ((value: CellValue) => string) | undefined => {
      const cellType = (
        queryData as GetQueryResponseDoneT
      ).columnDescriptions?.find((x) => x.label == fieldName)?.type;
      if (cellType) {
        return getRenderFunction(cellType);
      }
    },
    [getRenderFunction, queryData],
  );

  return { getRenderFunction, getRenderFunctionByFieldName };
}
