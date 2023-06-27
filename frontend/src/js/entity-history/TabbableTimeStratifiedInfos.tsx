import styled from "@emotion/styled";
import { useState, useMemo } from "react";

import { TimeStratifiedInfo } from "../api/types";
import SmallTabNavigation from "../small-tab-navigation/SmallTabNavigation";

import { TimeStratifiedChart } from "./TimeStratifiedChart";
import { TimeStratifiedConceptChart } from "./TimeStratifiedConceptChart";
import { isConceptColumn, isMoneyColumn } from "./timeline/util";

const Container = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
`;

export const TabbableTimeStratifiedInfos = ({
  infos,
}: {
  infos: TimeStratifiedInfo[];
}) => {
  const [activeTab, setActiveTab] = useState(infos[0].label);
  const options = useMemo(() => {
    return infos.map((info) => ({
      value: info.label,
      label: () => info.label,
    }));
  }, [infos]);

  const { data, type } = useMemo(() => {
    let infoType = "money";
    let infoData = infos.find((info) => info.label === activeTab);

    if (infoData?.columns.some((c) => !isMoneyColumn(c))) {
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

  console.log(data);

  return (
    <Container>
      <SmallTabNavigation
        options={options}
        selectedTab={activeTab}
        onSelectTab={setActiveTab}
      />
      {/* {data && type === "money" && (
        <TimeStratifiedChart timeStratifiedInfo={data} />
      )} */}
      {data && <TimeStratifiedConceptChart timeStratifiedInfo={data} />}
    </Container>
  );
};
