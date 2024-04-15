import { Vector } from "apache-arrow";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { CurrencyConfigT, GetQueryResponseDoneT } from "../api/types";
import { StateT } from "../app/reducers";
import { NUMBER_TYPES, currencyFromSymbol } from "./util";

export type CellValue = string | Vector;

export function useCustomTableRenderers(queryData: GetQueryResponseDoneT) {
  const { t } = useTranslation();
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );

  const getRenderFunction = useCallback(
    (cellType: string): ((value: CellValue) => string) => {
      const dateFormatter = new Intl.DateTimeFormat(navigator.language, {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });

      const currencyFormatter = new Intl.NumberFormat(navigator.language, {
        style: "currency",
        currency: currencyFromSymbol(currencyConfig.unit),
      });

      if (cellType.indexOf("LIST") == 0) {
        const listType = cellType.match(/LIST\[(?<listtype>.*)\]/)?.groups?.[
          "listtype"
        ];
        if (listType) {
          const listTypeRenderFunction = getRenderFunction(listType);
          return (value) =>
            value
              ? (value as Vector)
                  .toArray() // This is somewhat slow, but for-loop produces bogus values
                  .map(listTypeRenderFunction)
                  .join(", ")
              : null;
        }
      } else if (NUMBER_TYPES.includes(cellType)) {
        const numnberFormatter = new Intl.NumberFormat(navigator.language, {
          maximumFractionDigits: 2,
          minimumFractionDigits: cellType == "INTEGER" ? 0 : 2,
        });

        return (value) => {
          if (value && !isNaN(value as unknown as number)) {
            return numnberFormatter.format(value as unknown as number);
          }
          return "";
        };
      } else if (cellType == "DATE") {
        return (value) => dateFormatter.format(value as unknown as Date);
      } else if (cellType == "DATE_RANGE") {
        return (value) => {
          const vector = value as unknown as { min: Date; max: Date };

          const min = dateFormatter.format(vector.min);
          const max = dateFormatter.format(vector.max);

          if (min == max) {
            return min;
          }

          return `${min} - ${max}`;
        };
      } else if (cellType == "MONEY") {
        return (value) => {
          if (value && !isNaN(value as unknown as number)) {
            return currencyFormatter.format(value as unknown as number);
          }
          return "";
        };
      } else if (cellType == "BOOLEAN") {
        return (value) => (value ? t("common.true") : t("common.false"));
      }

      return (value) => (value ? (value as string) : "");
    },
    [currencyConfig.unit, t],
  );

  const getRenderFunctionByFieldName = useCallback(
    (fieldName: string): ((value: CellValue) => string) => {
      const cellType = (
        queryData as GetQueryResponseDoneT
      ).columnDescriptions?.find((x) => x.label == fieldName)?.type;

      if (cellType) {
        return getRenderFunction(cellType);
      }

      return (value) => (value ? (value as string) : "");
    },
    [getRenderFunction, queryData],
  );

  return { getRenderFunction, getRenderFunctionByFieldName };
}
