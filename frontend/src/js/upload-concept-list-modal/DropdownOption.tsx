import { FolderIcon } from "lucide-react";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { exists } from "../common/helpers/exists";
import { IndexPrefix } from "../ui-components/IndexPrefix";
import { C2, textStyle } from "../ui-components/Typography";

const container = tv({
  base: [
    "grid grid-cols-[110px_30px_auto_auto_auto_auto_1fr]",
    "items-start",
    "gap-x-2",
    "py-[3px]",
  ],
});

const indexPrefix = tv({
  base: [
    "mr-0",
    "bg-primary-500",
    "text-white",
    textStyle({ size: 3, strong: true }),
  ],
});

export const DropdownOption = memo(
  ({
    conceptLabel,
    connectorLabel,
    filterLabel,
    filterIdx,
  }: {
    conceptLabel: string;
    connectorLabel?: string;
    filterLabel?: string;
    filterIdx?: number;
  }) => {
    const { t } = useTranslation();
    const hasDifferentFilterLabel = exists(filterLabel) && exists(filterIdx);

    return (
      <div className={container()}>
        <C2 as="span" tone="muted">
          {hasDifferentFilterLabel
            ? t("uploadConceptListModal.filterValuesFrom")
            : t("uploadConceptListModal.conceptValuesFrom")}
        </C2>
        <div className="flex items-center justify-end">
          {hasDifferentFilterLabel ? (
            <IndexPrefix className={indexPrefix()}># {filterIdx}</IndexPrefix>
          ) : (
            <FolderIcon className="text-primary-500" />
          )}
        </div>
        <C2 as="span" tone={hasDifferentFilterLabel ? "muted" : "default"}>
          {conceptLabel}
        </C2>
        {hasDifferentFilterLabel && (
          <>
            <C2 as="span" tone="muted">
              &gt;
            </C2>
            <C2 as="span" tone="muted">
              {connectorLabel}
            </C2>
            <C2 as="span" tone="muted">
              &gt;
            </C2>
            <C2 as="span">{filterLabel}</C2>
          </>
        )}
      </div>
    );
  },
);
