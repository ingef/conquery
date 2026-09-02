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
import WithTooltip from "../tooltip/WithTooltip";

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
      <WithTooltip text={t("queryEditor.removeNode")}>
        <IconButton
          className={actionButton()}
          icon={faTimes}
          onClick={(e) => {
            e.stopPropagation();
            props.onDeleteNode(props.andIdx, props.orIdx);
          }}
        />
      </WithTooltip>
      {props.excludeTimestamps && (
        <WithTooltip text={t("queryNodeEditor.excludingTimestamps")}>
          <IconButton
            className={actionButton()}
            red
            icon={faCalendar}
            onClick={(e) => {
              e.stopPropagation();
              props.onToggleTimestamps(props.andIdx, props.orIdx);
            }}
          />
        </WithTooltip>
      )}
      {!props.error && !!props.previousQueryLoading && (
        <WithTooltip text={t("queryEditor.loadingPreviousQuery")}>
          <FaIcon className="mt-[7px] mb-1 mx-[6px]" icon={faSpinner} />
        </WithTooltip>
      )}
      {!props.error && props.isExpandable && !props.previousQueryLoading && (
        <WithTooltip text={t("queryEditor.expand")}>
          <IconButton
            className={actionButton()}
            icon={faExpandArrowsAlt}
            onClick={(e) => {
              e.stopPropagation();
              props.onExpandClick();
            }}
          />
        </WithTooltip>
      )}
      {props.hasActiveSecondaryId && (
        <WithTooltip
          text={
            props.excludeFromSecondaryId
              ? t("queryNodeEditor.excludingFromSecondaryId")
              : t("queryEditor.hasSecondaryId")
          }
        >
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
        </WithTooltip>
      )}
    </div>
  );
};

export default memo(QueryNodeActions);
