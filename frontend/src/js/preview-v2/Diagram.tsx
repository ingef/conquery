import { ChartData, ChartOptions } from "chart.js";
import { useMemo } from "react";
import { Bar, Line } from "react-chartjs-2";

import { theme } from "../../app-theme";
import {
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";
import { hexToRgbA } from "../entity-history/TimeStratifiedChart";
import pdfast, { createPdfastOptions } from "pdfast";
import { formatNumber } from "./util";

type DiagramProps = {
  stat: PreviewStatistics;
  className?: string;
  onClick?: () => void;
  height?: string|number;
  width?: string | number;
};

const PDF_POINTS = 40;
const DIGITS_OF_PRECISION = 3;

function previewStatsIsStringStats(
  stats: PreviewStatistics,
): stats is StringStatistics {
  return stats.type === "STRING";
}

export function previewStatsIsNumberStats(
  stats: PreviewStatistics,
): stats is NumberStatistics {
  return stats.type !== "STRING" && "stddev" in stats;
}

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
    width: 2 // TODO calculate this?!?!
  };
  const pdf: {x: number, y: number}[] = pdfast.create(stats.samples.sort((a,b) => a-b), options);
  const labels = pdf.map((p) => p.x.toPrecision(DIGITS_OF_PRECISION));
  const values = pdf.map((p) => p.y);
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
  const options: ChartOptions<"bar"> | ChartOptions<"line"> = useMemo(() => {
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
        },
        scales: {
          y: {
            beginAtZero: true,
          },
          x: {
            min: stat.min,
            max: stat.max,
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
              label: (context: any) => {
                console.log(context);
                console.log(typeof context.dataset.label)
                const label = formatNumber(context.parsed.x) || context.dataset.label || context.label || "";
                return `${label}: ${formatNumber(context.raw as number)}`;
              },
            },
            caretSize: 0,
            caretPadding: 0,
          },
        },
      };
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
              label: (context: any) => {
                const label = context.dataset.label || context.label || "";
                return `${label}: ${formatNumber(context.raw as number)}`;
              },
            },
            caretSize: 0,
            caretPadding: 0,
          },
        },
      };
    }
    throw new Error("Unknown stats type");
  }, [stat]);

  const data = useMemo(() => {
    if (previewStatsIsStringStats(stat)) {
      return transformStringStatsToData(stat);
    }
    if (previewStatsIsNumberStats(stat)) {
      return transformNumberStatsToData(stat);
    }
  }, [stat]);

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
