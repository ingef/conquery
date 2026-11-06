import type { Vector } from "apache-arrow";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import type { GetQueryResponseDoneT } from "../api/types";
import { currencyFractionDigits } from "../common/helpers/currency";
import { useCurrency, useMoneyFormatter } from "../common/useMoneyFormatter";
import { NUMBER_TYPES } from "./util";

type CellValue = string | Vector | null;

type RenderFunction = (value: CellValue) => string;

function getListRenderFunction(
  elementRenderFunction: RenderFunction,
): RenderFunction {
  return (value) =>
    value
      ? (value as Vector)
          .toArray() // This is somewhat slow, but for-loop produces bogus values
          .map(elementRenderFunction)
          .join(", ")
      : "";
}

function getNumberRenderFunction(cellType: string): RenderFunction {
  const numberFormatter = new Intl.NumberFormat(navigator.language, {
    maximumFractionDigits: 2,
    minimumFractionDigits: cellType === "INTEGER" ? 0 : 2,
  });

  return (value) => {
    if (!value) {
      return "";
    }

    const number = Number(value);

    if (Number.isNaN(number)) {
      return "";
    }

    return numberFormatter.format(number);
  };
}

function getDateRenderFunction(): RenderFunction {
  const dateFormatter = new Intl.DateTimeFormat(navigator.language, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  return (value) =>
    value ? dateFormatter.format(value as unknown as Date) : "";
}

function getDateRangeRenderFunction(): RenderFunction {
  const dateFormatter = new Intl.DateTimeFormat(navigator.language, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

  return (value) => {
    if (value === null) {
      return "";
    }

    const vector = value as unknown as {
      min: Date | null;
      max: Date | null;
    };

    const min = vector.min ? dateFormatter.format(vector.min) : "-∞";
    const max = vector.max ? dateFormatter.format(vector.max) : "+∞";

    if (min === max) {
      return min;
    }

    return `${min} - ${max}`;
  };
}

// arrow money values are integers in the currency's minor unit
function getMoneyRenderFunction(
  currencyFormatter: Intl.NumberFormat,
  fractionDigits: number,
): RenderFunction {
  return (value) => {
    if (!value) {
      return "";
    }

    const number = Number(value);

    if (Number.isNaN(number)) {
      return "";
    }

    return currencyFormatter.format(number / 10 ** fractionDigits);
  };
}

function getBooleanRenderFunction(
  trueLabel: string,
  falseLabel: string,
): RenderFunction {
  return (value) => {
    if (value === null) {
      return "";
    }

    return value ? trueLabel : falseLabel;
  };
}

function getDefaultRenderFunction(): RenderFunction {
  return (value) => (value ? (value as string) : "");
}

export function useCustomTableRenderers(queryData: GetQueryResponseDoneT) {
  const { t } = useTranslation();
  const moneyFormatter = useMoneyFormatter();
  const fractionDigits = currencyFractionDigits(useCurrency());

  const getRenderFunction = useCallback(
    (cellType: string): RenderFunction => {
      if (cellType.indexOf("LIST") === 0) {
        const innerType = cellType.match(/^LIST\[(?<nestedtype>.*)\]$/)?.groups
          ?.nestedtype;

        const renderFunction = innerType
          ? getRenderFunction(innerType)
          : getDefaultRenderFunction();
        return getListRenderFunction(renderFunction);
      } else if (NUMBER_TYPES.includes(cellType)) {
        return getNumberRenderFunction(cellType);
      } else if (cellType === "DATE") {
        return getDateRenderFunction();
      } else if (cellType === "DATE_RANGE") {
        return getDateRangeRenderFunction();
      } else if (cellType === "MONEY") {
        return getMoneyRenderFunction(moneyFormatter, fractionDigits);
      } else if (cellType === "BOOLEAN") {
        return getBooleanRenderFunction(t("common.true"), t("common.false"));
      }

      return getDefaultRenderFunction();
    },
    [moneyFormatter, fractionDigits, t],
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
