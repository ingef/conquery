import { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
import { ConfirmableTooltip } from "../../tooltip/ConfirmableTooltip";
import WithTooltip from "../../tooltip/WithTooltip";

import { ProjectItemT } from "./ProjectItem";
import { useRemoveQuery, useRemoveFormConfig } from "./actions";
import { isFormConfig } from "./helpers";

export const DeleteProjectItemButton = ({
  item,
  mayDeleteRightAway,
}: {
  item: ProjectItemT;
  mayDeleteRightAway: boolean;
}) => {
  const { t } = useTranslation();
  const { removeQuery } = useRemoveQuery();
  const { removeFormConfig } = useRemoveFormConfig();

  const onDelete = useCallback(() => {
    if (isFormConfig(item)) {
      removeFormConfig(item.id);
    } else {
      removeQuery(item.id);
    }
  }, [item, removeQuery, removeFormConfig]);

  const confirmationText = useMemo(
    () =>
      isFormConfig(item)
        ? t("formConfig.deleteNow")
        : t("previousQuery.deleteNow"),
    [item, t],
  );

  return mayDeleteRightAway ? (
    <WithTooltip text={t("common.delete")}>
      <IconButton
        icon="times"
        bare
        data-test-id="project-item-delete-button"
        onClick={onDelete}
      />
    </WithTooltip>
  ) : (
    <ConfirmableTooltip
      red
      onConfirm={onDelete}
      confirmationText={confirmationText}
    >
      <WithTooltip text={t("common.delete")}>
        <IconButton
          icon="times"
          bare
          data-test-id="project-item-delete-button"
        />
      </WithTooltip>
    </ConfirmableTooltip>
  );
};
