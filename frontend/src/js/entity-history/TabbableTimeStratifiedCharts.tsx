import styled from "@emotion/styled";
import { useState, useMemo } from "react";

import { TimeStratifiedInfo } from "../api/types";
import SmallTabNavigation from "../small-tab-navigation/SmallTabNavigation";

import { TimeStratifiedChart } from "./TimeStratifiedChart";

const Container = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
`;

export const TabbableTimeStratifiedCharts = ({
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

  const activeInfos = useMemo(() => {
    return infos.find((info) => info.label === activeTab);
  }, [infos, activeTab]);

  return (
    <Container>
      <SmallTabNavigation
        options={options}
        selectedTab={activeTab}
        onSelectTab={setActiveTab}
      />
      {activeInfos && <TimeStratifiedChart timeStratifiedInfo={activeInfos} />}
    </Container>
  );
};
