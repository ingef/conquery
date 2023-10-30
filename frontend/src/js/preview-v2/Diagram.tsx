import { ChartData, ChartOptions } from "chart.js";
import { useMemo } from "react";
import { Bar, Line } from "react-chartjs-2";

import { theme } from "../../app-theme";
import {
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";
import { exists } from "../common/helpers/exists";
import { hexToRgbA } from "../entity-history/TimeStratifiedChart";
import { formatCurrency } from "../entity-history/timeline/util";

type DiagramProps = {
  stat: PreviewStatistics;
  className?: string;
};

const NORMAL_DISTRIBUTION_STEPS = 40;
const DIGITS_OF_PRECISION = 3;

function previewStatsIsStringStats(
  stats: PreviewStatistics,
): stats is StringStatistics {
  return stats.type === "STRING";
}

function previewStatsIsNumberStats(
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

function interpolateNormalDistribution(
  mean: number,
  stddev: number,
  steps: number,
): { labels: string[]; values: number[] } {
  // each value is given by the function f(x) = 1/(σ * sqrt(2π)) * e^(-(x-μ)^2/(2σ^2))
  // with μ = mean and σ = stddev
  // we calculate the value for each step between mean - 3σ and mean + 3σ
  const values = [];
  const labels = [];
  const stepSize = (6 * stddev) / steps;
  let x = mean - 3 * stddev;
  for (let i = 0; i < steps; i++) {
    values.push(
      (1 / (stddev * Math.sqrt(2 * Math.PI))) *
        Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(stddev, 2))),
    );
    labels.push(x.toPrecision(DIGITS_OF_PRECISION));
    x += stepSize;
  }

  return { labels, values };
}
// TODOS:
// - some diagrams don't display data (nomal distribution)
// - fix diagram layout

function transformNumberStatsToData(
  stats: NumberStatistics,
): ChartData<"line"> {
  const { labels, values } = interpolateNormalDistribution(
    stats.mean,
    stats.stddev,
    NORMAL_DISTRIBUTION_STEPS,
  );
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

export default function Diagram({ stat, className }: DiagramProps) {
  const options: ChartOptions<"bar"> | ChartOptions<"line"> = useMemo(() => {
    if (previewStatsIsNumberStats(stat)) {
      return {
        type: "line",
        responsive: true,
        interaction: {
          mode: "index" as const,
          intersect: false,
        },
//        maintainAspectRatio: false,
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
                const label = context.dataset.label || context.label || "";
                return `${label}: ${(context.raw as number).toFixed(DIGITS_OF_PRECISION) }`;
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
 //       maintainAspectRatio: false,
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
                return `${label}: ${(context.raw as number).toFixed(DIGITS_OF_PRECISION)}`;
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

  return (
    <>
      {previewStatsIsStringStats(stat) ? (
        <Bar
          className={className}
          options={options as ChartOptions<"bar">}
          data={data as ChartData<"bar">}
        />
      ) : (
        <Line
          className={className}
          options={options as ChartOptions<"line">}
          data={data as ChartData<"line">}
        />
      )}
    </>
  );
}
