import { memo } from "react";
import { useTranslation } from "react-i18next";
import ErrorMessage from "../error-message/ErrorMessage";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../ui-components/Tooltip";
import { C2, C3 } from "../ui-components/Typography";

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
    <TooltipTrigger>
      <TooltipTarget
        as="div"
        excludeFromTabOrder
        className="grow pt-[2px] [word-break:break-word]"
      >
        {!isConceptQueryNode && (
          <C3 tone="muted" strong>
            {t("queryEditor.previousQuery")}
          </C3>
        )}
        {error ? (
          <ErrorMessage className="m-0" message={error} />
        ) : (
          <>
            {rootNodeLabel && (
              <C3 tone="muted" strong>
                {rootNodeLabel}
              </C3>
            )}
            <C2>{label}</C2>
            {description && <C3>{description}</C3>}
          </>
        )}
      </TooltipTarget>
      <Tooltip>{tooltipText}</Tooltip>
    </TooltipTrigger>
  );
};

export default memo(QueryNodeContent);
