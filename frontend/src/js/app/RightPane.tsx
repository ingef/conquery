import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import { EditorV2 } from "../editor-v2/EditorV2";
import { isEditorV2Enabled } from "../environment";
import { ResetableErrorBoundary } from "../error-fallback/ResetableErrorBoundary";
import FormsTab from "../external-forms/FormsTab";
import Pane from "../pane/Pane";
import type { TabNavigationTab } from "../pane/TabNavigation";
import StandardQueryEditorTab from "../standard-query-editor/StandardQueryEditorTab";
import type { StateT } from "./reducers";

const tab = tv({
  base: ["h-full", "grow", "flex-col"],
  variants: {
    isActive: {
      true: "flex",
      false: "hidden",
    },
  },
});

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
    <Pane className="bg-bg-100" right tabs={tabs} dataTestId="right-pane">
      <div
        key="queryEditor"
        className={tab({ isActive: activeTab === "queryEditor" })}
      >
        <StandardQueryEditorTab />
      </div>
      {isEditorV2Enabled && (
        <div
          key="editorV2"
          className={tab({ isActive: activeTab === "editorV2" })}
        >
          <EditorV2
            featureDates
            featureNegate
            featureExpand
            featureConnectorRotate
            featureQueryNodeEdit
            featureContentInfos
            featureTimebasedQueries
          />
        </div>
      )}
      <div
        key="externalForms"
        className={tab({ isActive: activeTab === "externalForms" })}
      >
        <ResetableErrorBoundary>
          <FormsTab />
        </ResetableErrorBoundary>
      </div>
    </Pane>
  );
};

export default RightPane;
