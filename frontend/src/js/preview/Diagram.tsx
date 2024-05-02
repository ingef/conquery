import { ChartData, ChartOptions } from "chart.js";
import { addMonths, format } from "date-fns";
import { useMemo } from "react";
import { Bar, Line } from "react-chartjs-2";

import { BarStatistics, DateStatistics, PreviewStatistics } from "../api/types";
import { parseDate, parseStdDate } from "../common/helpers/dateHelper";
import { hexToRgbA } from "../entity-history/TimeStratifiedChart";

import { Theme, useTheme } from "@emotion/react";
import { useTranslation } from "react-i18next";
import {
  formatNumber,
  previewStatsIsBarStats,
  previewStatsIsDateStats,
  useDateTickHandler,
} from "./util";

type DiagramProps = {
  stat: PreviewStatistics;
  className?: string;
  onClick?: () => void;
  height?: string | number;
  width?: string | number;
};
function transformBarStatsToData(
  stats: BarStatistics,
  theme: Theme,
): ChartData<"bar"> {
  return {
    labels: stats.entries.map((entry) => entry.label),
    datasets: [
      {
        data: stats.entries.map((entry) => entry.value),
        backgroundColor: `rgba(${hexToRgbA(theme.col.blueGrayDark)}, 1)`,
        borderWidth: 1,
      },
    ],
  };
}

function transformDateStatsToData(
  stats: DateStatistics,
  theme: Theme,
): ChartData<"line"> {
  const labels: string[] = [];
  const values: number[] = [];
  const minDate = parseStdDate(stats.span.min);
  const maxDate = parseStdDate(stats.span.max);
  const start = parseStdDate(`${minDate?.getFullYear()}-01-01`);
  const end = parseStdDate(`${maxDate?.getFullYear()}-12-01`);

  if (start === null || end === null) {
    return {
      labels,
      datasets: [
        {
          data: values,
          borderColor: `rgba(${hexToRgbA(theme.col.blueGrayDark)}, 1)`,
          borderWidth: 1,
          fill: false,
        },
      ],
    };
  }
  const { monthCounts } = stats;
  let pointer = start;
  while (pointer <= end) {
    const month = format(pointer, "yyyy-MM");
    const monthLabel = format(pointer, "dd.MM.yyyy");

    labels.push(monthLabel);
    values.push(monthCounts[month] ?? 0);

    pointer = addMonths(pointer, 1);
  }
  return {
    labels,
    datasets: [
      {
        data: values,
        borderColor: `rgba(${hexToRgbA(theme.col.blueGrayDark)}, 1)`,
        borderWidth: 1,
        fill: false,
      },
    ],
  };
}

export default function Diagram({
  stat,
  className,
  onClick,
  height,
  width,
}: DiagramProps) {
  const theme = useTheme();
  const data = useMemo(() => {
    if (previewStatsIsBarStats(stat)) {
      return transformBarStatsToData(stat, theme);
    }
    if (previewStatsIsDateStats(stat)) {
      return transformDateStatsToData(stat, theme);
    }
  }, [stat, theme]);
  const { t } = useTranslation();
  const { shouldTickRender } = useDateTickHandler(stat);

  const options = useMemo(() => {
    const baseOptions = {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: "index" as const,
        intersect: false,
      },
      layout: {
        padding: 0,
      },
      plugins: {
        title: {
          display: true,
          font: {
            weight: "normal",
            size: 14,
          },
          position: "bottom",
          text: stat.label,
        },
        tooltip: {
          usePointStyle: true,
          backgroundColor: "rgba(255, 255, 255, 0.9)",
          titleColor: "rgba(0, 0, 0, 1)",
          bodyColor: "rgba(0, 0, 0, 1)",
          borderColor: "rgba(0, 0, 0, 0.2)",
          borderWidth: 0.5,
          padding: 10,
          callbacks: {
            title: (title) => title[0].label,
            label: (context) => formatNumber(context.raw as number),
          },
          caretSize: 0,
          caretPadding: 0,
        },
      },
    } as Partial<ChartOptions>;

    const yScaleTitle = {
      display: true,
      text: t("preview.chartYLabel"),
      font: {
        weight: "normal",
        size: 14,
      },
    };

    if (previewStatsIsBarStats(stat)) {
      return {
        ...baseOptions,
        type: "bar",
        scales: {
          y: {
            title: yScaleTitle,
            beginAtZero: true,
          },
        },
      } as ChartOptions<"bar">;
    }

    if (previewStatsIsDateStats(stat)) {
      return {
        ...baseOptions,
        type: "line",
        elements: {
          point: {
            radius: 0,
          },
        },
        scales: {
          y: {
            title: yScaleTitle,
            beginAtZero: true,
            ticks: {
              callback: (value: number) => {
                return formatNumber(value);
              },
            },
          },
          x: {
            ticks: {
              autoSkip: false,
              callback: (valueIndex: number) => {
                const label = data?.labels?.[valueIndex];
                if (label) {
                  const date = parseDate(label as string, "dd.MM.yyyy");
                  if (date && shouldTickRender(date)) {
                    return label as string;
                  }
                }
                return "";
              },
            },
          },
        },
      } as ChartOptions<"line">;
    }

    throw new Error("Unknown stats type");
  }, [data?.labels, stat, t, shouldTickRender]);

  return (
    <div className={className}>
      {previewStatsIsBarStats(stat) ? (
        <Bar
          options={options as ChartOptions<"bar">}
          data={data as ChartData<"bar">}
          onClick={() => onClick && onClick()}
          height={height}
          width={width}
        />
      ) : (
        <Line
          options={options as ChartOptions<"line">}
          data={data as ChartData<"line">}
          onClick={() => onClick && onClick()}
          height={height}
          width={width}
        />
      )}
    </div>
  );
}
