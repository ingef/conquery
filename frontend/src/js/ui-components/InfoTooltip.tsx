import { CircleQuestionMarkIcon } from "lucide-react";
import type { ReactElement } from "react";
import { tv } from "tailwind-variants";

import {
  Tooltip,
  type TooltipSize,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "./Tooltip";

const icon = tv({
  base: ["text-gray-600 hover:text-gray-800", "transition-all duration-100"],
});

// centered next to text, in a flex row and inside a line
const spanContainer = tv({
  base: ["inline-flex items-center", "align-middle", "px-[7px]"],
});

const InfoTooltip = ({
  className,
  text,
  html,
  size,
  excludeFromTabOrder,
}: {
  text?: string;
  html?: ReactElement;
  className?: string;
  size?: TooltipSize;
  /** for a help icon inside a label: tabbing through the fields must not open it */
  excludeFromTabOrder?: boolean;
}) => {
  return (
    <TooltipTrigger delay={tooltipDelay.immediate}>
      <TooltipTarget
        role="img"
        aria-label="Info"
        className={spanContainer({ className })}
        excludeFromTabOrder={excludeFromTabOrder}
      >
        <CircleQuestionMarkIcon className={icon()} />
      </TooltipTarget>
      <Tooltip size={size}>
        {text ? (
          <span
            // biome-ignore lint/security/noDangerouslySetInnerHtml: help texts come from form configs and the backend, which may use markup
            dangerouslySetInnerHTML={{ __html: text }}
          />
        ) : (
          html
        )}
      </Tooltip>
    </TooltipTrigger>
  );
};

export default InfoTooltip;
