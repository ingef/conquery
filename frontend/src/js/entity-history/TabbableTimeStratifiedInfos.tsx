import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { TimeStratifiedInfo } from "../api/types";
import { Tab, TabList, Tabs } from "../ui-components/Tabs";

import { TimeStratifiedChart } from "./TimeStratifiedChart";
import { TimeStratifiedConceptChart } from "./TimeStratifiedConceptChart";
import { isConceptColumn, isMoneyColumn } from "./timeline/util/util";

const container = tv({
  base: ["self-start", "flex flex-col items-end", "overflow-x-hidden"],
});

export const TabbableTimeStratifiedInfos = ({
  infos,
}: {
  infos: TimeStratifiedInfo[];
}) => {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState(infos[0].label);

  const { data, type } = useMemo(() => {
    let infoType = "money";
    let infoData = infos.find((info) => info.label === activeTab);

    if (infoData?.columns.some((c) => isMoneyColumn(c))) {
      const columns = infoData?.columns.filter(isMoneyColumn);

      infoData = {
        ...infoData,
        totals: Object.fromEntries(
          Object.entries(infoData?.totals).filter(([k]) =>
            columns?.map((c) => c.label).includes(k),
          ),
        ),
        columns: columns ?? [],
      };
    } else if (infoData?.columns.some(isConceptColumn)) {
      // TODO: Handle concept data
      infoType = "concept";
    }

    return { data: infoData, type: infoType };
  }, [infos, activeTab]);

  return (
    <div className={container()}>
      <Tabs selectedKey={activeTab} onSelectionChange={setActiveTab}>
        <TabList aria-label={t("history.infoTabs")}>
          {infos.map((info) => (
            <Tab key={info.label} id={info.label}>
              {info.label}
            </Tab>
          ))}
        </TabList>
      </Tabs>
      {data && type === "money" && (
        <TimeStratifiedChart timeStratifiedInfo={data} />
      )}
      {data && type === "concept" && (
        <TimeStratifiedConceptChart timeStratifiedInfo={data} />
      )}
    </div>
  );
};
