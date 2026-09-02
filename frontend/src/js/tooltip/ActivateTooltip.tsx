import { faAngleRight } from "@fortawesome/free-solid-svg-icons";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";

import { toggleDisplayTooltip } from "./actions";

const button = tv({
  base: [
    "absolute top-[40px] right-0 bottom-0",
    "w-full",
    "p-3",
    "rounded-none",
    "flex items-start",
  ],
});

const ActivateTooltip = () => {
  const dispatch = useDispatch();
  const onToggleTooltip = () => dispatch(toggleDisplayTooltip());

  return (
    <div className="relative h-full">
      <IconButton
        className={button()}
        bgHover
        icon={faAngleRight}
        onClick={onToggleTooltip}
      />
    </div>
  );
};

export default ActivateTooltip;
