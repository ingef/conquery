import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import {
  faExpandArrowsAlt,
  faMicroscope,
  faSpinner,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";

import { Icon } from "../ui-components/Icon";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../ui-components/Tooltip";

const crossedOut = tv({
  base: [
    "absolute top-[40%] left-[10%]",
    "h-[3px] w-[22px]",
    "rotate-[135deg]",
    "bg-red",
    "opacity-50",
    "pointer-events-none",
  ],
});

interface Props {
  andIdx: number;
  orIdx: number;
  excludeTimestamps?: boolean;
  excludeFromSecondaryId?: boolean;
  isExpandable?: boolean;
  hasDetails?: boolean;
  previousQueryLoading?: boolean;
  error?: string;
  hasActiveSecondaryId?: boolean;
  onDeleteNode: (andIdx: number, orIdx: number) => void;
  onExpandClick: () => void;
  onToggleTimestamps: (andIdx: number, orIdx: number) => void;
  onToggleSecondaryIdExclude: (andIdx: number, orIdx: number) => void;
}

const QueryNodeActions = (props: Props) => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-center justify-start">
      <TooltipTrigger>
        <Button
          size="sm"
          aria-label={t("queryEditor.removeNode")}
          intent="tertiary"
          onPress={() => {
            props.onDeleteNode(props.andIdx, props.orIdx);
          }}
        >
          <Icon icon={faTimes} />
        </Button>
        <Tooltip>{t("queryEditor.removeNode")}</Tooltip>
      </TooltipTrigger>
      {props.excludeTimestamps && (
        <TooltipTrigger>
          <Button
            aria-label={t("queryNodeEditor.excludingTimestamps")}
            intent="tertiary"
            danger
            size="sm"
            onPress={() => {
              props.onToggleTimestamps(props.andIdx, props.orIdx);
            }}
          >
            <Icon icon={faCalendar} />
          </Button>
          <Tooltip>{t("queryNodeEditor.excludingTimestamps")}</Tooltip>
        </TooltipTrigger>
      )}
      {!props.error && !!props.previousQueryLoading && (
        <TooltipTrigger>
          <TooltipTarget
            role="img"
            aria-label={t("queryEditor.loadingPreviousQuery")}
            excludeFromTabOrder
          >
            <Icon icon={faSpinner} className="mt-[7px] mb-1 mx-[6px]" />
          </TooltipTarget>
          <Tooltip>{t("queryEditor.loadingPreviousQuery")}</Tooltip>
        </TooltipTrigger>
      )}
      {!props.error && props.isExpandable && !props.previousQueryLoading && (
        <TooltipTrigger>
          <Button
            aria-label={t("queryEditor.expand")}
            intent="tertiary"
            onPress={() => {
              props.onExpandClick();
            }}
          >
            <Icon icon={faExpandArrowsAlt} />
          </Button>
          <Tooltip>{t("queryEditor.expand")}</Tooltip>
        </TooltipTrigger>
      )}
      {props.hasActiveSecondaryId && (
        <TooltipTrigger>
          <div className="relative">
            <Button
              aria-label={
                props.excludeFromSecondaryId
                  ? t("queryNodeEditor.excludingFromSecondaryId")
                  : t("queryEditor.hasSecondaryId")
              }
              intent="tertiary"
              size="sm"
              data-test-id="secondary-id-toggle"
              onPress={() => {
                props.onToggleSecondaryIdExclude(props.andIdx, props.orIdx);
              }}
            >
              <Icon icon={faMicroscope} />
            </Button>
            {props.excludeFromSecondaryId && <div className={crossedOut()} />}
          </div>
          <Tooltip>
            {props.excludeFromSecondaryId
              ? t("queryNodeEditor.excludingFromSecondaryId")
              : t("queryEditor.hasSecondaryId")}
          </Tooltip>
        </TooltipTrigger>
      )}
    </div>
  );
};

export default memo(QueryNodeActions);
