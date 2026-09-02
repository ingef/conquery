import { faPlay, faSpinner, faStop } from "@fortawesome/free-solid-svg-icons";
import type { Ref } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import BasicButton from "../button/BasicButton";
import FaIcon from "../icon/FaIcon";

const left = tv({
  base: ["px-[15px]", "transition-[color,background-color] duration-100"],
  variants: {
    running: {
      true: ["bg-white", "border-r border-primary-500"],
      false: "bg-primary-500",
    },
  },
});

const runnerLabel = tv({
  base: [
    "px-[15px]",
    "bg-white group-hover/runner:bg-gray-50",
    "text-gray-800",
    "leading-[2.5]",
    "whitespace-nowrap",
    "transition-[background-color] duration-100",
  ],
});

const button = tv({
  base: [
    "group/runner",
    "inline-flex flex-row items-center",
    "m-0 p-0",
    "overflow-hidden",
    "outline-none",
    "rounded",
    "border border-primary-500",
    "text-sm",
    "leading-[2.5]",
  ],
});

function getIcon(loading: boolean, running: boolean) {
  return loading ? faSpinner : running ? faStop : faPlay;
}

interface Props {
  isStartStopLoading: boolean;
  isQueryRunning: boolean;
  disabled: boolean;
  onClick: () => void;
}

// A button that is prefixed by an icon
const QueryRunnerButton = ({
  ref,
  onClick,
  isStartStopLoading,
  isQueryRunning,
  disabled,
}: Props & { ref?: Ref<HTMLDivElement> }) => {
  const { t } = useTranslation();
  const label = isQueryRunning ? t("queryRunner.stop") : t("queryRunner.start");

  const icon = getIcon(isStartStopLoading, isQueryRunning);

  return (
    <div className="flex" ref={ref}>
      <BasicButton
        type="button"
        className={button()}
        onClick={onClick}
        disabled={disabled}
        data-test-id="query-runner-button"
      >
        <span className={left({ running: isQueryRunning })}>
          <FaIcon white={!isQueryRunning} icon={icon} />
        </span>
        <span className={runnerLabel()}>{label}</span>
      </BasicButton>
    </div>
  );
};

export default QueryRunnerButton;
