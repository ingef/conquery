import { faAngleRight } from "@fortawesome/free-solid-svg-icons";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

import { toggleInfoPane } from "./actions";

const button = tv({
  base: [
    "absolute top-[40px] right-0 bottom-0",
    "w-full h-auto",
    "pt-3",
    "rounded-none",
    "items-start",
  ],
});

const InfoPaneCollapsed = () => {
  const dispatch = useDispatch();
  const onToggleInfoPane = () => dispatch(toggleInfoPane());

  return (
    <div className="relative h-full">
      <Button intent="tertiary" onPress={onToggleInfoPane} className={button()}>
        <Icon icon={faAngleRight} />
      </Button>
    </div>
  );
};

export default InfoPaneCollapsed;
