import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";
import { Icon } from "../ui-components/Icon";

const container = tv({
  base: [
    "grid grid-cols-[110px_30px_auto_auto_auto_auto_1fr]",
    "items-start",
    "gap-x-2",
    "py-[3px]",
    "text-sm",
  ],
});

const text = tv({
  base: ["m-0", "text-gray-500"],
  variants: {
    bold: { true: "text-gray-800 font-normal" },
  },
});

const indexPrefix = tv({
  base: ["mr-0", "bg-primary-500", "text-white", "font-bold"],
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
        <span className={text()}>
          {hasDifferentFilterLabel
            ? t("uploadConceptListModal.filterValuesFrom")
            : t("uploadConceptListModal.conceptValuesFrom")}
        </span>
        <div className="flex items-center justify-end">
          {hasDifferentFilterLabel ? (
            <IndexPrefix className={indexPrefix()}># {filterIdx}</IndexPrefix>
          ) : (
            <Icon
              icon={faFolder}
              className={[
                !hasDifferentFilterLabel ? "text-primary-500" : undefined,
                hasDifferentFilterLabel ? "text-gray-500" : undefined,
              ]}
            />
          )}
        </div>
        <span className={text({ bold: !hasDifferentFilterLabel })}>
          {conceptLabel}
        </span>
        {hasDifferentFilterLabel && (
          <>
            <span className={text()}>&gt;</span>
            <span className={text()}>{connectorLabel}</span>
            <span className={text()}>&gt;</span>
            <span className={text({ bold: true })}>{filterLabel}</span>
          </>
        )}
      </div>
    );
  },
);
