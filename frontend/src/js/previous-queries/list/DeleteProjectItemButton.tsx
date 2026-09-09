import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../../ui-components/Button";
import { ConfirmMenu } from "../../ui-components/ConfirmMenu";
import { Icon } from "../../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";
import { useRemoveFormConfig, useRemoveQuery } from "./actions";
import { isFormConfig } from "./helpers";
import type { ProjectItemT } from "./ProjectItem";

export const DeleteProjectItemButton = ({ item }: { item: ProjectItemT }) => {
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

  return (
    <TooltipTrigger>
      <ConfirmMenu red onConfirm={onDelete} confirmationText={confirmationText}>
        <Button
          intent="tertiary"
          size="sm"
          aria-label={t("common.delete")}
          data-test-id="project-item-delete-button"
        >
          <Icon icon={faTimes} />
        </Button>
      </ConfirmMenu>
      <Tooltip>{t("common.delete")}</Tooltip>
    </TooltipTrigger>
  );
};
