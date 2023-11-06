import { useTheme } from "@emotion/react";
import styled from "@emotion/styled";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
  ChartOptions,
} from "chart.js";
import { useMemo } from "react";
import { Bar } from "react-chartjs-2";

import { TimeStratifiedInfo } from "../api/types";
import { exists } from "../common/helpers/exists";

import { formatCurrency } from "./timeline/util";

const TRUNCATE_X_AXIS_LABELS_LEN = 18;

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip);

const ChartContainer = styled("div")`
  height: 190px;
  width: 100%;
  display: flex;
  justify-content: flex-end;
`;

function hexToRgbA(hex: string) {
  let c: string | string[];
  if (/^#([A-Fa-f0-9]{3}){1,2}$/.test(hex)) {
    c = hex.substring(1).split("");
    if (c.length === 3) {
      c = [c[0], c[0], c[1], c[1], c[2], c[2]];
    }
    c = "0x" + c.join("");
    // @ts-ignore TODO: clarify why this works / use a different / typed algorithm
    return [(c >> 16) & 255, (c >> 8) & 255, c & 255].join(",");
  }
  throw new Error("Bad Hex");
}

function interpolateDecreasingOpacity(index: number) {
  return Math.min(1, 1 / (index + 0.3));
}

export const TimeStratifiedChart = ({
  timeStratifiedInfo,
}: {
  timeStratifiedInfo: TimeStratifiedInfo;
}) => {
  const theme = useTheme();
  const labels = timeStratifiedInfo.columns.map((col) => col.label);

  const datasets = useMemo(() => {
    const sortedYears = [...timeStratifiedInfo.years].sort(
      (a, b) => b.year - a.year,
    );

    return sortedYears.map((year, i) => {
      return {
        label: year.year.toString(),
        data: labels.map((label) => year.values[label]),
        backgroundColor: `rgba(${hexToRgbA(
          theme.col.blueGrayDark,
        )}, ${interpolateDecreasingOpacity(i)})`,
      };
    });
  }, [theme, timeStratifiedInfo, labels]);

  const data = {
    labels,
    datasets,
  };

  const options: ChartOptions<"bar"> = useMemo(() => {
    return {
      plugins: {
        title: {
          display: true,
          text: timeStratifiedInfo.label,
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
            label: (context) => {
              const label = context.dataset.label || context.label || "";
              const value = exists(context.parsed.y)
                ? formatCurrency(context.parsed.y)
                : 0;
              return `${label}: ${value}`;
            },
          },
          caretSize: 0,
          caretPadding: 0,
        },
        legend: {
          labels: {
            usePointStyle: true,
            pointStyle: "circle",
            pointBorderWidth: 1,
            pointBorderColor: "rgba(0, 0, 0, 0.2)",
          },
        },
      },
      responsive: true,
      interaction: {
        mode: "index" as const,
        intersect: false,
      },
      layout: {
        padding: 0,
      },
      maintainAspectRatio: false,
      scales: {
        x: {
          ticks: {
            callback: (idx: number) => {
              return labels[idx].length > TRUNCATE_X_AXIS_LABELS_LEN
                ? labels[idx].substring(0, TRUNCATE_X_AXIS_LABELS_LEN - 3) +
                    "..."
                : labels[idx];
            },
          },
        },
        y: {
          ticks: {
            callback: (value) =>
              typeof value === "number" ? formatCurrency(value) : value,
          },
        },
      },
    };
  }, [timeStratifiedInfo, labels]);

  return (
    <ChartContainer>
      <Bar options={options} data={data} />
    </ChartContainer>
  );
};
