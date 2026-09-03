import { faPlay, faSpinner, faStop } from "@fortawesome/free-solid-svg-icons";
import type { Ref } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { Button } from "../ui-components/Button";
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
    "text-gray-800",
    "self-stretch",
    "flex items-center",
    "whitespace-nowrap",
    "transition-[background-color] duration-100",
  ],
});

const button = tv({
  base: ["group/runner", "p-0", "overflow-hidden", "border-primary-500"],
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
      <Button
        intent="secondary"
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
      </Button>
    </div>
  );
};

export default QueryRunnerButton;
