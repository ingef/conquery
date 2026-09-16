import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { EditorV2 } from "../editor-v2/EditorV2";
import { isEditorV2Enabled } from "../environment";
import { ResetableErrorBoundary } from "../error-fallback/ResetableErrorBoundary";
import FormsTab from "../external-forms/FormsTab";
import Pane, { type PaneTab } from "../pane/Pane";
import StandardQueryEditorTab from "../standard-query-editor/StandardQueryEditorTab";

const RightPane = () => {
  const { t } = useTranslation();

  const tabs: PaneTab[] = useMemo(
    () => [
      {
        key: "queryEditor",
        label: t("rightPane.queryEditor"),
        tooltip: t("help.tabQueryEditor"),
        content: <StandardQueryEditorTab />,
      },
      ...(isEditorV2Enabled
        ? [
            {
              key: "editorV2",
              label: t("rightPane.editorV2"),
              tooltip: t("help.tabEditorV2"),
              content: (
                <EditorV2
                  featureDates
                  featureNegate
                  featureExpand
                  featureConnectorRotate
                  featureQueryNodeEdit
                  featureContentInfos
                  featureTimebasedQueries
                />
              ),
            },
          ]
        : []),
      {
        key: "externalForms",
        label: t("rightPane.externalForms"),
        tooltip: t("help.tabFormEditor"),
        content: (
          <ResetableErrorBoundary>
            <FormsTab />
          </ResetableErrorBoundary>
        ),
      },
    ],
    [t],
  );

  return (
    <Pane className="bg-bg-100" right tabs={tabs} dataTestId="right-pane" />
  );
};

export default RightPane;
