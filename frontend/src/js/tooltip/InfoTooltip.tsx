import { faQuestionCircle } from "@fortawesome/free-regular-svg-icons";
import type { ReactElement } from "react";
import { tv } from "tailwind-variants";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

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
    <WithTooltip text={text} html={html} wide={wide}>
      <span className={spanContainer({ className })}>
        <FaIcon className={icon()} gray icon={faQuestionCircle} />
      </span>
    </WithTooltip>
  );
};

export default InfoTooltip;
