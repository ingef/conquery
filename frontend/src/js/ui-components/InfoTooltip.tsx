import { faQuestionCircle } from "@fortawesome/free-regular-svg-icons";
import type { ReactElement } from "react";
import { tv } from "tailwind-variants";

import { Icon } from "./Icon";

import {
  Tooltip,
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
  wide,
}: {
  text?: string;
  html?: ReactElement;
  className?: string;
  wide?: boolean;
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
      <Tooltip wide={wide}>
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
