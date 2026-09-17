import { faPlay, faSpinner, faStop } from "@fortawesome/free-solid-svg-icons";
import type { Ref } from "react";
import { Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Icon } from "../ui-components/Icon";

const left = tv({
  base: [
    "self-stretch",
    "flex items-center",
    "px-[15px]",
    "transition-[color,background-color] duration-100",
  ],
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
    "text-gray-800 font-medium",
    "self-stretch",
    "flex items-center",
    "whitespace-nowrap",
    "transition-[background-color] duration-100",
  ],
});

// two-tone: the icon part is filled, the label part stays white
const button = tv({
  base: [
    "group/runner",
    "inline-flex items-center",
    "h-[30px]",
    "overflow-hidden",
    "rounded",
    "border border-primary-500",
    "text-sm",
    "cursor-pointer",
    "disabled:cursor-not-allowed disabled:opacity-40",
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
      <RacButton
        className={button()}
        onPress={onClick}
        isDisabled={disabled}
        data-test-id="query-runner-button"
      >
        <span className={left({ running: isQueryRunning })}>
          <Icon
            icon={icon}
            className={[!isQueryRunning ? "text-white" : undefined]}
          />
        </span>
        <span className={runnerLabel()}>{label}</span>
      </RacButton>
    </div>
  );
};

export default QueryRunnerButton;
