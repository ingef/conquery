import styled from "@emotion/styled";
import { faArrowLeft, faArrowRight } from "@fortawesome/free-solid-svg-icons";
import { t } from "i18next";

import { PreviewStatistics } from "../api/types";
import IconButton from "../button/IconButton";

import Diagram from "./Diagram";

const Root = styled("div")``;

const SxDiagram = styled(Diagram)`
  padding: 5px;
  margin-right: 15px;
  height: 27vh;
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

const SxIconButton = styled(IconButton)`
  font-size: 24;
`;

const DiagramContainer = styled("div")`
  overflow-x: hidden;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-gap: 5px;
`;

type ChartProps = {
  statistics: PreviewStatistics[];
  className?: string;
  showPopup: (statistic: PreviewStatistics) => void;
  page: number;
  setPage: (page: number) => void;
};

const DIAGRAMS_PER_PAGE = 4;

export default function Charts({
  statistics,
  className,
  showPopup,
  page,
  setPage,
}: ChartProps) {
  const diagramsOnPage = statistics.slice(page * DIAGRAMS_PER_PAGE, (page + 1) * DIAGRAMS_PER_PAGE);
  return (
    <>
      <Root className={className}>
        <DiagramContainer>
          {diagramsOnPage.map((statistic) => {
            return (
              <div key={statistic.label}>
                <SxDiagram
                  stat={statistic}
                  onClick={() => showPopup(statistic)}
                />
              </div>
            );
          })}
        </DiagramContainer>
        <DirectionSelector>
          <SxIconButton
            icon={faArrowLeft}
            onClick={() => setPage(page - 1)}
            disabled={page === 0}
          />
          <span>
            {t("preview.page")} {page + 1}/{Math.ceil(statistics.length / DIAGRAMS_PER_PAGE)}
          </span>
          <SxIconButton
            icon={faArrowRight}
            onClick={() => setPage(page + 1)}
            disabled={(page + 1) * DIAGRAMS_PER_PAGE >= statistics.length}
          />
        </DirectionSelector>
      </Root>
    </>
  );
}
