import { ChartData, ChartOptions } from "chart.js";
import { addMonths, format } from "date-fns";
import { useMemo } from "react";
import { Bar, Line } from "react-chartjs-2";

import { BarStatistics, DateStatistics, PreviewStatistics } from "../api/types";
import { parseStdDate } from "../common/helpers/dateHelper";
import { hexToRgbA } from "../entity-history/TimeStratifiedChart";

import { Theme, useTheme } from "@emotion/react";
import {
  formatNumber,
  previewStatsIsBarStats,
  previewStatsIsDateStats,
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
  // loop over all dates in date range
  // check if month is present in stats
  // if yes add months value to data
  // if no add quater values to data for the whole quater (for each month)

  const labels: string[] = [];
  const values: number[] = [];
  const start = parseStdDate(stats.span.min);
  const end = parseStdDate(stats.span.max);
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
    // check month exists
    const month = format(pointer, "yyyy-MM");
    const monthLabel = format(pointer, "MMM yyyy");
    if (month in monthCounts) {
      labels.push(monthLabel);
      values.push(monthCounts[month]);
    } else {
      // add zero values
      labels.push(monthLabel);
      values.push(0);
    }

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

function getValueForIndex<T>(
  data: ChartData | undefined,
  index: number,
): T | undefined {
  const labels = data?.labels;
  if (!labels) {
    return undefined;
  }
  return labels[index] as T | undefined;
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

  const options = useMemo(() => {
    if (previewStatsIsBarStats(stat)) {
      return {
        type: "bar",
        responsive: true,
        interaction: {
          mode: "index" as const,
          intersect: false,
        },
        maintainAspectRatio: false,
        layout: {
          padding: 0,
        },
        scales: {
          y: {
            beginAtZero: true,
          },
        },
        plugins: {
          title: {
            display: true,
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
      } as ChartOptions<"bar">;
    }

    if (previewStatsIsDateStats(stat)) {
      return {
        type: "line",
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: "index" as const,
          intersect: false,
        },
        layout: {
          padding: 0,
        },
        elements: {
          point: {
            radius: 0,
          },
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: (value: number) => {
                return formatNumber(value);
              },
            },
          },
          x: {
            suggestedMin: stat.span.min,
            suggestedMax: stat.span.max,
            ticks: {
              callback: (valueIndex: number, index: number) => {
                return index % 2 === 0
                  ? getValueForIndex(data, valueIndex)
                  : "";
              },
            },
          },
        },
        plugins: {
          title: {
            display: true,
            text: stat.label,
          },
          tooltip: {
            usePointStyle: true,
            backgroundColor: "rgba(255, 255, 255, 0.9)",
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
      } as ChartOptions<"line">;
    }

    throw new Error("Unknown stats type");
  }, [stat, data]);

  // TODO fall back if no data is present
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
