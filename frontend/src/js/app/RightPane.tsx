import styled from "@emotion/styled";
import { useCallback, useMemo, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { EditorV2 } from "../editor-v2/EditorV2";
import { ResetableErrorBoundary } from "../error-fallback/ResetableErrorBoundary";
import FormsTab from "../external-forms/FormsTab";
import Pane from "../pane/Pane";
import { TabNavigationTab } from "../pane/TabNavigation";
import StandardQueryEditorTab from "../standard-query-editor/StandardQueryEditorTab";
import TimebasedQueryEditorTab from "../timebased-query-editor/TimebasedQueryEditorTab";
import { getUserSettings, storeUserSettings } from "../user/userSettings";

import type { StateT } from "./reducers";

const Tab = styled("div")<{ isActive: boolean }>`
  height: 100%;
  flex-grow: 1;
  flex-direction: column;

  display: ${({ isActive }) => (isActive ? "flex" : "none")};
`;

const SxPane = styled(Pane)`
  background-color: ${({ theme }) => theme.col.bgAlt};
`;

const useEditorV2 = () => {
  const [showEditorV2, setShowEditorV2] = useState<boolean>(
    getUserSettings().showEditorV2,
  );

  const toggleEditorV2 = useCallback(() => {
    setShowEditorV2(!showEditorV2);
    storeUserSettings({ showEditorV2: !showEditorV2 });
  }, [showEditorV2]);

  useHotkeys("shift+alt+e", toggleEditorV2, [showEditorV2]);

  return {
    showEditorV2,
  };
};

const RightPane = () => {
  const { t } = useTranslation();
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes.right.activeTab,
  );

  const { showEditorV2 } = useEditorV2();

  const tabs: TabNavigationTab[] = useMemo(
    () => [
      {
        key: "queryEditor",
        label: t("rightPane.queryEditor"),
        tooltip: t("help.tabQueryEditor"),
      },
      showEditorV2
        ? {
            key: "editorV2",
            label: t("rightPane.editorV2"),
            tooltip: t("help.tabEditorV2"),
          }
        : {
            key: "timebasedQueryEditor",
            label: t("rightPane.timebasedQueryEditor"),
            tooltip: t("help.tabTimebasedEditor"),
          },
      {
        key: "externalForms",
        label: t("rightPane.externalForms"),
        tooltip: t("help.tabFormEditor"),
      },
    ],
    [t, showEditorV2],
  );

  return (
    <SxPane right tabs={tabs} dataTestId="right-pane">
      <Tab key={tabs[0].key} isActive={activeTab === tabs[0].key}>
        <StandardQueryEditorTab />
      </Tab>
      <Tab key={tabs[1].key} isActive={activeTab === tabs[1].key}>
        {showEditorV2 ? (
          <EditorV2
            featureDates
            featureNegate
            featureExpand
            featureConnectorRotate
            featureQueryNodeEdit
            featureContentInfos
            featureTimebasedQueries
          />
        ) : (
          <TimebasedQueryEditorTab />
        )}
      </Tab>
      <Tab key={tabs[2].key} isActive={activeTab === tabs[2].key}>
        <ResetableErrorBoundary>
          <FormsTab />
        </ResetableErrorBoundary>
      </Tab>
    </SxPane>
  );
};

export default RightPane;
