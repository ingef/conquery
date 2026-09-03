import { faAngleRight } from "@fortawesome/free-solid-svg-icons";
import { Button as RacButton } from "react-aria-components";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";
import { Icon } from "../ui-components/Icon";

import { toggleInfoPane } from "./actions";

// a strip that fills the collapsed pane; the chevron sits at its top
const button = tv({
  base: [
    "absolute top-[40px] right-0 bottom-0",
    "w-full",
    "flex justify-center items-start",
    "pt-3",
    "text-gray-800",
    "cursor-pointer",
    "hover:bg-gray-50",
  ],
});

const InfoPaneCollapsed = () => {
  const dispatch = useDispatch();
  const onToggleInfoPane = () => dispatch(toggleInfoPane());

  return (
    <div className="relative h-full">
      <RacButton className={button()} onPress={onToggleInfoPane}>
        <Icon icon={faAngleRight} />
      </RacButton>
    </div>
  );
};

export default InfoPaneCollapsed;
