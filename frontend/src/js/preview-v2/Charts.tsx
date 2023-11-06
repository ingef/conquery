import styled from "@emotion/styled";
import { faArrowLeft, faArrowRight } from "@fortawesome/free-solid-svg-icons";
import { t } from "i18next";
import { useState } from "react";

import { PreviewStatistics } from "../api/types";
import IconButton from "../button/IconButton";

import Diagram from "./Diagram";

const Root = styled("div")``;

const SxDiagram = styled(Diagram)`
  width: 100px;
  padding: 5px;
  margin-right: 15px;
`;

const DirectionSelector = styled("div")`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
  grid-column: 1 / 3;
  grid-row: 3;
  padding-left: 100px;
  padding-right: 100px;
`;

type ChartProps = {
  statistics: PreviewStatistics[];
  className?: string;
  showPopup: (statistic: PreviewStatistics) => void;
};

export default function Charts({ statistics, className, showPopup }: ChartProps) {
  const [index, setIndex] = useState<number>(0);

  return (
    <>
      <Root className={className}>
        {statistics.slice(index * 4, (index + 1) * 4).map((statistic) => {
          return (
            <div>
              <SxDiagram stat={statistic} onClick={() => showPopup(statistic)} />
            </div>
          );
        })}
        <DirectionSelector>
          <IconButton
            icon={faArrowLeft}
            onClick={() => setIndex(index - 1)}
            disabled={index === 0}
          />
          <span>
            {t("preview.page")} {index + 1}/{Math.ceil(statistics.length / 4)}
          </span>
          <IconButton
            icon={faArrowRight}
            onClick={() => setIndex(index + 1)}
            disabled={(index + 1) * 4 >= statistics.length}
          />
        </DirectionSelector>
      </Root>
    </>
  );
}
