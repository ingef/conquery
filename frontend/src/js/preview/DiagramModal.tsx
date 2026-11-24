import RcTable from "rc-table";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { PreviewStatistics } from "../api/types";
import { Modal, ModalBody, ModalHeader } from "../ui-components/Modal";
import Diagram from "./Diagram";
import { StyledTable } from "./Table";
import { previewStatsIsBarStats } from "./util";

interface DiagramModalProps {
  statistic: PreviewStatistics;
  onClose: () => void;
}

const body = tv({ base: ["flex items-start", "gap-5"] });

const diagram = tv({ base: ["h-[70vh]", "min-w-0 grow"] });

const components = { table: StyledTable };

export default function DiagramModal({
  statistic,
  onClose,
}: DiagramModalProps) {
  const { t } = useTranslation();

  return (
    <Modal
      size="full"
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalHeader subtitle={statistic.description}>
        {statistic.label}
      </ModalHeader>
      <ModalBody>
        <div className={body()}>
          <Diagram className={diagram()} stat={statistic} showTitle={false} />
          {previewStatsIsBarStats(statistic) &&
            Object.keys(statistic.extras).length > 0 && (
              <RcTable
                className="shrink-0"
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
