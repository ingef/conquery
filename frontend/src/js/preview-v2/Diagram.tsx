import { useMemo } from "react";
import { NumberStatistics, PreviewStatistics, StringStatistics } from "../api/types";
import { ChartData, ChartOptions } from "chart.js";
import { Bar } from "react-chartjs-2";
import { hexToRgbA } from "../entity-history/TimeStratifiedChart";
import { theme } from "../../app-theme";

type DiagramProps = {
    stat: PreviewStatistics;
    className?: string;
}

function previewStatsIsStringStats(stats: PreviewStatistics): stats is StringStatistics {
    return stats.type === "STRING";
}

function previewStatsIsNumberStats(stats: PreviewStatistics): stats is NumberStatistics {
    return stats.type !== "STRING" && "stddev" in stats;
}

function transformStringStatsToData(stats: StringStatistics): ChartData<"bar"> {
    return {
        labels: Object.keys(stats.histogram),
        datasets: [{
            data: Object.values(stats.histogram),
            backgroundColor: `rgba(${hexToRgbA(
                theme.col.blueGrayDark,
            )}, 1)`,
            borderWidth: 1
        }]
    }
}

export default function ({ stat, className }: DiagramProps) {
    const options: ChartOptions<"bar">|ChartOptions<"line"> = useMemo(() => {
        if (previewStatsIsNumberStats(stat)) {
            return {
                scales: {
                    y: {
                        min: stat.min,
                        max: stat.max
                    }
                }
            }
        }
        if (previewStatsIsStringStats(stat)) {
            console.log("string_stats", stat);
            return {
                type: "bar",
                responsive: true,
                interaction: {
                    mode: "index" as const,
                    intersect: false,
                },
                layout: {
                    padding: 0,
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        }
        throw new Error("Unknown stats type");
    }, [stat]);

    return (
        <div className={className}>
            {
                previewStatsIsStringStats(stat) ?
                    (<Bar options={options as ChartOptions<"bar">} data={transformStringStatsToData(stat)}/>) : null
            }
        </div>
    )
}