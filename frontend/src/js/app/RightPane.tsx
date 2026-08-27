import styled from "@emotion/styled";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { EditorV2 } from "../editor-v2/EditorV2";
import { isEditorV2Enabled } from "../environment";
import { ResetableErrorBoundary } from "../error-fallback/ResetableErrorBoundary";
import FormsTab from "../external-forms/FormsTab";
import Pane from "../pane/Pane";
import type { TabNavigationTab } from "../pane/TabNavigation";
import StandardQueryEditorTab from "../standard-query-editor/StandardQueryEditorTab";

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

const RightPane = () => {
  const { t } = useTranslation();
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes.right.activeTab,
  );

  const tabs: TabNavigationTab[] = useMemo(
    () => [
      {
        key: "queryEditor",
        label: t("rightPane.queryEditor"),
        tooltip: t("help.tabQueryEditor"),
      },
      ...(isEditorV2Enabled
        ? [
            {
              key: "editorV2",
              label: t("rightPane.editorV2"),
              tooltip: t("help.tabEditorV2"),
            },
          ]
        : []),
      {
        key: "externalForms",
        label: t("rightPane.externalForms"),
        tooltip: t("help.tabFormEditor"),
      },
    ],
    [t],
  );

  return (
    <SxPane right tabs={tabs} dataTestId="right-pane">
      <Tab key="queryEditor" isActive={activeTab === "queryEditor"}>
        <StandardQueryEditorTab />
      </Tab>
      {isEditorV2Enabled && (
        <Tab key="editorV2" isActive={activeTab === "editorV2"}>
          <EditorV2
            featureDates
            featureNegate
            featureExpand
            featureConnectorRotate
            featureQueryNodeEdit
            featureContentInfos
            featureTimebasedQueries
          />
        </Tab>
      )}
      <Tab key="externalForms" isActive={activeTab === "externalForms"}>
        <ResetableErrorBoundary>
          <FormsTab />
        </ResetableErrorBoundary>
      </Tab>
    </SxPane>
  );
};

export default RightPane;
