import { ChartData, ChartOptions } from "chart.js";
import { addMonths, format } from "date-fns";
import pdfast, { createPdfastOptions } from "pdfast";
import { useMemo } from "react";
import { Bar, Line } from "react-chartjs-2";

import {
  DateStatistics,
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";
import { parseStdDate } from "../common/helpers/dateHelper";
import { hexToRgbA } from "../entity-history/TimeStratifiedChart";

import {
  formatNumber,
  previewStatsIsDateStats,
  previewStatsIsNumberStats,
  previewStatsIsStringStats,
} from "./util";
import { t } from "i18next";
import { useTheme } from "@emotion/react";

type DiagramProps = {
  stat: PreviewStatistics;
  className?: string;
  onClick?: () => void;
  height?: string | number;
  width?: string | number;
};

const PDF_POINTS = 100;


export default function Diagram({
  stat,
  className,
  onClick,
  height,
  width,
}: DiagramProps) {
  const theme = useTheme();

  function transformStringStatsToData(stats: StringStatistics): ChartData<"bar"> {
    return {
      labels: Object.keys(stats.histogram),
      datasets: [
        {
          data: Object.values(stats.histogram),
          backgroundColor: `rgba(${hexToRgbA(theme.col.blueGrayDark)}, 1)`,
          borderWidth: 1,
        },
      ],
    };
  }
  
  function transformNumberStatsToData(
    stats: NumberStatistics,
  ): ChartData<"line"> {
    const options: createPdfastOptions = {
      min: stats.min,
      max: stats.max,
      size: PDF_POINTS,
      width: 5, // TODO calculate this?
    };
    const pdf: { x: number; y: number }[] = pdfast.create(
      stats.samples.sort((a, b) => a - b),
      options,
    );
    const labels = pdf.map((p) => p.x);
    const values = pdf.map((p) => p.y*100); // Percentage
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
  
  function transformDateStatsToData(stats: DateStatistics): ChartData<"line"> {
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
  
  function getValueForIndex<T>(index: number):T|undefined {
    const labels = data?.labels;
    if(!labels) {
      return undefined;
    }
    return labels[index] as T|undefined;
  }

  const data = useMemo(() => {
    if (previewStatsIsStringStats(stat)) {
      return transformStringStatsToData(stat);
    }
    if (previewStatsIsNumberStats(stat)) {
      return transformNumberStatsToData(stat);
    }
    if (previewStatsIsDateStats(stat)) {
      return transformDateStatsToData(stat);
    }
  }, [stat]);

  const options = useMemo(() => {
    if (previewStatsIsNumberStats(stat)) {
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
          line: {
            cubicInterpolationMode: "default",
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            //grace: "%"
          },
          x: {
            beginAtZero: true,
            suggestedMin: stat.min,
            suggestedMax: stat.max,
            ticks: {
              callback: (index: number) => {
                // How can I return the scale here?!
                return (getValueForIndex<number>(index)||0).toLocaleString();
              },
            }
          },
        },
        plugins: {
          title: {
            display: true,
            text: `${t("preview.densityPlot")}: ${stat.label}`,
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
              // To remove the title from the tooltip a null is needed. 
              // This does not work with the typescript definition of chart.js
              // -> cast to unknown and then to undefined
              title: () => null as unknown as undefined,
              label: (context) => {
                return `${formatNumber(getValueForIndex(context.parsed.x)||0)}: ${formatNumber(context.raw as number)}%`;
              },
            },
            caretSize: 0,
            caretPadding: 0,
          },
        },
      } as ChartOptions<"line">;
    }
    if (previewStatsIsStringStats(stat)) {
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
              title: () => null as unknown as undefined,
              label: (context) => {
                const label = context.dataset.label || context.label || "";
                return `${label}: ${formatNumber(context.raw as number)}`;
              },
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
            }
          },
          x: {
            suggestedMin: stat.span.min,
            suggestedMax: stat.span.max,
            ticks: {
              callback: (valueIndex: number, index: number) => {
                return index % 2 === 0 ? getValueForIndex(valueIndex) : "";
              },
            }
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
              title: () => null as unknown as undefined,
              label: (context) => {
                return `${context.label}: ${formatNumber(context.raw as number)}`;
              },
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
      {previewStatsIsStringStats(stat) ? (
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
