import { faCheck } from "@fortawesome/free-solid-svg-icons";
import { tv } from "tailwind-variants";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import InfoTooltip from "./InfoTooltip";
import { Tooltip, TooltipTarget, TooltipTrigger } from "./Tooltip";

const row = tv({
  base: ["flex flex-row items-center", "cursor-pointer"],
  variants: {
    disabled: { true: "cursor-not-allowed" },
  },
});

const label = tv({ base: ["ml-[10px]", "text-sm", "leading-none"] });

const container = tv({
  base: [
    "relative",
    "shrink-0",
    "box-content",
    "h-5 w-5",
    "border-2 border-primary-500",
    "rounded",
    "text-[22px]",
  ],
  variants: {
    disabled: { true: "opacity-50" },
  },
});

const checkmark = tv({
  base: [
    "absolute top-0 left-0",
    "flex items-center justify-center",
    "h-5 w-5",
    "bg-primary-500",
    "text-white",
  ],
});

const checkmarkIcon = tv({ base: ["text-white!", "scale-125"] });

const InputCheckbox = ({
  label: labelText,
  className,
  tooltip,
  infoTooltip,
  value,
  onChange,
  disabled,
}: {
  label: string;
  className?: string;
  tooltip?: string;
  infoTooltip?: string;
  value?: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
}) => (
  // biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a real checkbox
  // biome-ignore lint/a11y/noStaticElementInteractions: see above
  <div
    className={row({ disabled, className })}
    onClick={() => {
      if (!disabled) onChange(!value);
    }}
  >
    <TooltipTrigger>
      <TooltipTarget
        as="div"
        excludeFromTabOrder
        className={container({ disabled })}
      >
        {!!value && (
          <div className={checkmark()}>
            <FaIcon icon={faCheck} className={checkmarkIcon()} />
          </div>
        )}
      </TooltipTarget>
      <Tooltip>{tooltip}</Tooltip>
    </TooltipTrigger>
    <span className={label()}>{labelText}</span>
    {exists(infoTooltip) && <InfoTooltip text={infoTooltip} />}
  </div>
);

export default InputCheckbox;
