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
import { Fragment, useMemo } from "react";
import { Bar } from "react-chartjs-2";
import { useSelector } from "react-redux";

import { EntityInfo, TimeStratifiedInfo } from "../api/types";
import { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";

import EntityInfos from "./EntityInfos";
import { getColumnType } from "./timeline/util";

const Container = styled("div")`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 20px;
  background-color: ${({ theme }) => theme.col.bg};
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
`;

const Centered = styled("div")`
  display: flex;
  align-items: flex-start;
`;

const Col = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

const ChartContainer = styled("div")`
  height: 200px;
  width: 100%;
`;

const Grid = styled("div")`
  display: grid;
  gap: 0 20px;
  grid-template-columns: auto auto;
`;

const Label = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;

const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
  justify-self: end;
`;

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  // Title,
  Tooltip,
  // Legend,
);

function hexToRgbA(hex: string) {
  let c: any;
  if (/^#([A-Fa-f0-9]{3}){1,2}$/.test(hex)) {
    c = hex.substring(1).split("");
    if (c.length === 3) {
      c = [c[0], c[0], c[1], c[1], c[2], c[2]];
    }
    c = "0x" + c.join("");
    return [(c >> 16) & 255, (c >> 8) & 255, c & 255].join(",");
  }
  throw new Error("Bad Hex");
}

const Table = ({
  timeStratifiedInfos,
  currencyUnit,
}: {
  timeStratifiedInfos: TimeStratifiedInfo[];
  currencyUnit: string;
}) => {
  return (
    <>
      {timeStratifiedInfos.map((timeStratifiedInfo) => {
        return (
          <Grid key={timeStratifiedInfo.label}>
            {timeStratifiedInfo.columns.map((column) => {
              const columnType = getColumnType(
                timeStratifiedInfo,
                column.label,
              );

              const label = column.label;
              const value = timeStratifiedInfo.totals[column.label];

              if (!exists(value)) return <></>;

              const valueFormatted =
                typeof value === "number"
                  ? Math.round(value)
                  : value instanceof Array
                  ? value.join(", ")
                  : value;

              return (
                <Fragment key={label}>
                  <Label>{label}</Label>
                  <Value>
                    {valueFormatted}
                    {columnType === "MONEY" ? " " + currencyUnit : ""}
                  </Value>
                </Fragment>
              );
            })}
          </Grid>
        );
      })}
    </>
  );
};

const TimeStratifiedInfos = ({
  timeStratifiedInfos,
}: {
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const currencyUnit = useSelector<StateT, string>(
    (state) => state.startup.config.currency.unit,
  );

  const theme = useTheme();
  const datasets = useMemo(() => {
    const sortedYears = [...timeStratifiedInfos[0].years].sort(
      (a, b) => b.year - a.year,
    );

    return sortedYears.map((year, i) => {
      return {
        label: year.year.toString(),
        data: Object.values(year.values),
        backgroundColor: `rgba(${hexToRgbA(theme.col.blueGrayDark)}, ${1 / i})`,
      };
    });
  }, [theme, timeStratifiedInfos]);

  const entries = Object.entries(timeStratifiedInfos[0].totals);
  const labels = entries.map(([key]) => key);
  // const values = entries.map(([, value]) => value);

  const data = {
    labels,
    datasets,
  };

  const options: ChartOptions<"bar"> = useMemo(() => {
    return {
      // indexAxis: "y" as const,
      // legend: {
      //   position: "right" as const,
      // },
      plugins: {
        title: {
          display: true,
          text: timeStratifiedInfos[0].label,
        },
        subtitle: {
          text: "ololol",
        },
      },
      responsive: true,
      interaction: {
        mode: "index" as const,
        intersect: false,
      },
      datasets: {},
      layout: {
        padding: 0,
      },
      // scales: {
      //   x: {
      //     stacked: true,
      //   },
      //   y: {
      //     stacked: true,
      //   },
      // },
    };
  }, [timeStratifiedInfos]);

  return (
    <Col>
      <ChartContainer>
        <Bar options={options} data={data} />
      </ChartContainer>
      {/* <Table
        currencyUnit={currencyUnit}
        timeStratifiedInfos={timeStratifiedInfos}
      /> */}
    </Col>
  );
};

export const EntityCard = ({
  className,
  infos,
  timeStratifiedInfos,
}: {
  className?: string;
  infos: EntityInfo[];
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  return (
    <Container className={className}>
      <Centered>
        <EntityInfos infos={infos} />
      </Centered>
      <TimeStratifiedInfos timeStratifiedInfos={timeStratifiedInfos} />
    </Container>
  );
};
