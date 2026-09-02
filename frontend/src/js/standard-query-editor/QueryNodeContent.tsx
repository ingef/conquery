import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import ErrorMessage from "../error-message/ErrorMessage";
import WithTooltip from "../ui-components/WithTooltip";

// tv consts named *Text to not shadow the label/description props
const labelText = tv({
  base: ["[word-break:break-word]", "leading-[1.2]", "text-base"],
});

const descriptionText = tv({
  base: [
    "mt-[3px]",
    "[word-break:break-word]",
    "leading-[1.2]",
    "uppercase",
    "text-xs",
  ],
});

const previousQueryLabel = tv({
  base: [
    "mb-[3px]",
    "leading-[1.2]",
    "text-xs",
    "uppercase",
    "font-bold",
    "text-primary-500",
  ],
});

const rootNode = tv({
  base: [
    "mb-1",
    "leading-none",
    "uppercase",
    "font-bold",
    "text-xs",
    "text-primary-500",
    "[word-break:break-word]",
  ],
});

interface Props {
  tooltipText?: string;
  isConceptQueryNode?: boolean;
  error?: string;
  label: string;
  description?: string;
  rootNodeLabel: string | null;
}

const QueryNodeContent = ({
  tooltipText,
  error,
  label,
  description,
  rootNodeLabel,
  isConceptQueryNode,
}: Props) => {
  const { t } = useTranslation();

  return (
    <WithTooltip text={tooltipText}>
      <div className="grow pt-[2px]">
        {!isConceptQueryNode && (
          <p className={previousQueryLabel()}>
            {t("queryEditor.previousQuery")}
          </p>
        )}
        {error ? (
          <ErrorMessage className="m-0" message={error} />
        ) : (
          <>
            {rootNodeLabel && <p className={rootNode()}>{rootNodeLabel}</p>}
            <p className={labelText()}>{label}</p>
            {description && <p className={descriptionText()}>{description}</p>}
          </>
        )}
      </div>
    </WithTooltip>
  );
};

export default memo(QueryNodeContent);
