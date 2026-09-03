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

import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../ui-components/Tooltip";

const actionButton = tv({
  base: "px-[6px] py-1",
});

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
        <IconButton
          className={actionButton()}
          icon={faTimes}
          onClick={(e) => {
            e.stopPropagation();
            props.onDeleteNode(props.andIdx, props.orIdx);
          }}
        />
        <Tooltip>{t("queryEditor.removeNode")}</Tooltip>
      </TooltipTrigger>
      {props.excludeTimestamps && (
        <TooltipTrigger>
          <IconButton
            className={actionButton()}
            red
            icon={faCalendar}
            onClick={(e) => {
              e.stopPropagation();
              props.onToggleTimestamps(props.andIdx, props.orIdx);
            }}
          />
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
            <FaIcon className="mt-[7px] mb-1 mx-[6px]" icon={faSpinner} />
          </TooltipTarget>
          <Tooltip>{t("queryEditor.loadingPreviousQuery")}</Tooltip>
        </TooltipTrigger>
      )}
      {!props.error && props.isExpandable && !props.previousQueryLoading && (
        <TooltipTrigger>
          <IconButton
            className={actionButton()}
            icon={faExpandArrowsAlt}
            onClick={(e) => {
              e.stopPropagation();
              props.onExpandClick();
            }}
          />
          <Tooltip>{t("queryEditor.expand")}</Tooltip>
        </TooltipTrigger>
      )}
      {props.hasActiveSecondaryId && (
        <TooltipTrigger>
          <div className="relative">
            <IconButton
              className={actionButton()}
              icon={faMicroscope}
              data-test-id="secondary-id-toggle"
              onClick={(e) => {
                e.stopPropagation();
                props.onToggleSecondaryIdExclude(props.andIdx, props.orIdx);
              }}
            />
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
