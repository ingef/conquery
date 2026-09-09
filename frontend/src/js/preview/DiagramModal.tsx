import { t } from "i18next";
import RcTable from "rc-table";
import { tv } from "tailwind-variants";
import type { PreviewStatistics } from "../api/types";
import { Modal, ModalBody } from "../ui-components/Modal";
import Diagram from "./Diagram";
import { StyledTable } from "./Table";
import { previewStatsIsBarStats } from "./util";

interface DiagramModalProps {
  statistic: PreviewStatistics;
  onClose: () => void;
}

const diagram = tv({
  base: ["h-[70vh] w-[70vw]", "mr-[15px]"],
});

export default function DiagramModal({
  statistic,
  onClose,
}: DiagramModalProps) {
  const components = {
    table: StyledTable,
  };

  return (
    <Modal
      aria-label={t("preview.headline")}
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalBody>
        <div className="inline-flex">
          <Diagram className={diagram()} stat={statistic} />
          {previewStatsIsBarStats(statistic) &&
            Object.keys(statistic.extras).length > 0 && (
              <RcTable
                className="m-auto"
                columns={[
                  {
                    title: t("preview.name"),
                    dataIndex: "name",
                    key: "name",
                  },
                  {
                    title: t("preview.value"),
                    dataIndex: "value",
                    key: "value",
                  },
                ]}
                data={Object.entries(statistic.extras).map(([name, value]) => {
                  return { name, value };
                })}
                rowKey={(_, index) => `row_${index}`}
                components={components}
              />
            )}
        </div>
      </ModalBody>
    </Modal>
  );
}
