import { faQuestionCircle } from "@fortawesome/free-regular-svg-icons";
import type { ReactElement } from "react";
import { tv } from "tailwind-variants";

import { Icon } from "./Icon";

import {
  Tooltip,
  type TooltipSize,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "./Tooltip";

const icon = tv({
  base: ["transition-all duration-100", "hover:text-gray-800"],
});

const spanContainer = tv({ base: ["inline-block", "px-[7px]"] });

const InfoTooltip = ({
  className,
  text,
  html,
  size,
}: {
  text?: string;
  html?: ReactElement;
  className?: string;
  size?: TooltipSize;
}) => {
  return (
    <TooltipTrigger delay={tooltipDelay.immediate}>
      <TooltipTarget
        role="img"
        aria-label="Info"
        className={spanContainer({ className })}
      >
        <Icon icon={faQuestionCircle} className={[icon(), "text-gray-500"]} />
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
