import { LoaderCircleIcon, PlayIcon, SquareIcon } from "lucide-react";
import type { Ref } from "react";
import { Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

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
      false: "bg-primary-500 text-white",
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

const RunnerIcon = ({
  loading,
  running,
}: {
  loading: boolean;
  running: boolean;
}) => {
  if (loading) return <LoaderCircleIcon />;

  return running ? <SquareIcon className="fill-current" /> : <PlayIcon />;
};

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

  return (
    <div className="flex" ref={ref}>
      <RacButton
        className={button()}
        onPress={onClick}
        isDisabled={disabled}
        data-test-id="query-runner-button"
      >
        <span className={left({ running: isQueryRunning })}>
          <RunnerIcon loading={isStartStopLoading} running={isQueryRunning} />
        </span>
        <span className={runnerLabel()}>{label}</span>
      </RacButton>
    </div>
  );
};

export default QueryRunnerButton;
