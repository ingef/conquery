import type { Vector } from "apache-arrow";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import type { CurrencyConfigT, GetQueryResponseDoneT } from "../api/types";
import type { StateT } from "../app/reducers";
import { currencyFromSymbol, NUMBER_TYPES } from "./util";

export type CellValue = string | Vector;

type RenderFunction = (value: CellValue) => string;

function getListRenderFunction(
  cellType: string,
  getRenderFunction: (cellType: string) => RenderFunction,
): RenderFunction | null {
  const listType = cellType.match(/LIST\[(?<listtype>.*)\]/)?.groups?.listtype;

  if (!listType) {
    return null;
  }

  const listTypeRenderFunction = getRenderFunction(listType);
  return (value) =>
    value
      ? (value as Vector)
          .toArray() // This is somewhat slow, but for-loop produces bogus values
          .map(listTypeRenderFunction)
          .join(", ")
      : null;
}

function getNumberRenderFunction(cellType: string): RenderFunction {
  const numberFormatter = new Intl.NumberFormat(navigator.language, {
    maximumFractionDigits: 2,
    minimumFractionDigits: cellType === "INTEGER" ? 0 : 2,
  });

  return (value) => {
    if (value && !Number.isNaN(Number(value))) {
      return numberFormatter.format(value as unknown as number);
    }
    return "";
  };
}

function getDateRenderFunction(): RenderFunction {
  const dateFormatter = new Intl.DateTimeFormat(navigator.language, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  return (value) => dateFormatter.format(value as unknown as Date);
}

function getDateRangeRenderFunction(): RenderFunction {
  const dateFormatter = new Intl.DateTimeFormat(navigator.language, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  return (value) => {
    const vector = value as unknown as { min: Date; max: Date };

    const min = dateFormatter.format(vector.min);
    const max = dateFormatter.format(vector.max);

    if (min === max) {
      return min;
    }

    return `${min} - ${max}`;
  };
}

function getMoneyRenderFunction(
  currencyUnit: CurrencyConfigT["unit"],
): RenderFunction {
  const currencyFormatter = new Intl.NumberFormat(navigator.language, {
    style: "currency",
    currency: currencyFromSymbol(currencyUnit),
  });

  return (value) => {
    if (value && !Number.isNaN(Number(value))) {
      return currencyFormatter.format((value as unknown as number) / 100); // MONEY is sent as cent
    }
    return "";
  };
}

function getBooleanRenderFunction(
  getTrueLabel: () => string,
  getFalseLabel: () => string,
): RenderFunction {
  return (value) => (value ? getTrueLabel() : getFalseLabel());
}

function getDefaultRenderFunction(): RenderFunction {
  return (value) => (value ? (value as string) : "");
}

export function useCustomTableRenderers(queryData: GetQueryResponseDoneT) {
  const { t } = useTranslation();
  const currencyConfig = useSelector<StateT, CurrencyConfigT>(
    (state) => state.startup.config.currency,
  );

  const getRenderFunction = useCallback(
    (cellType: string): RenderFunction => {
      if (cellType.indexOf("LIST") === 0) {
        const listRenderFunction = getListRenderFunction(
          cellType,
          getRenderFunction,
        );
        if (listRenderFunction) {
          return listRenderFunction;
        }
      } else if (NUMBER_TYPES.includes(cellType)) {
        return getNumberRenderFunction(cellType);
      } else if (cellType === "DATE") {
        return getDateRenderFunction();
      } else if (cellType === "DATE_RANGE") {
        return getDateRangeRenderFunction();
      } else if (cellType === "MONEY") {
        return getMoneyRenderFunction(currencyConfig.unit);
      } else if (cellType === "BOOLEAN") {
        return getBooleanRenderFunction(
          () => t("common.true"),
          () => t("common.false"),
        );
      }

      return getDefaultRenderFunction();
    },
    [currencyConfig.unit, t],
  );

  const getRenderFunctionByFieldName = useCallback(
    (fieldName: string): RenderFunction => {
      const cellType = (
        queryData as GetQueryResponseDoneT
      ).columnDescriptions?.find((x) => x.label === fieldName)?.type;

      if (cellType) {
        return getRenderFunction(cellType);
      }

      return getDefaultRenderFunction();
    },
    [getRenderFunction, queryData],
  );

  return { getRenderFunction, getRenderFunctionByFieldName };
}
