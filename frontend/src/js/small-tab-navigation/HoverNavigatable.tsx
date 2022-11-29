import { FC } from "react";
import { useDrop } from "react-dnd";
import { DNDType } from "../common/constants/dndTypes";


interface PropsT {
    timeUntilNavigateOverride?: number;
    triggerNavigate: () => void;
    disabled?: boolean;
}

// default time until the hover triggers navigation
// 600 ms is the default time for a tooltip to appear
// feels very responsive
// can be overriden by passing a timeUntilNavigateOverride prop
const TIME_UNTIL_NAVIGATE = 600;

export const HoverNagiatable:FC<PropsT> = ({ timeUntilNavigateOverride, triggerNavigate, children, disabled }) => {
    const time_to_navigate = timeUntilNavigateOverride || TIME_UNTIL_NAVIGATE;
    let timeout: number | null | NodeJS.Timeout; // timeout can either be a number or a NodeJS.Timeout depending on the environment
    const [_, drop] = useDrop({
        accept: [DNDType.FORM_CONFIG, DNDType.CONCEPT_TREE_NODE, DNDType.PREVIOUS_QUERY, DNDType.PREVIOUS_SECONDARY_ID_QUERY],
        hover: (_, monitor) => {
            // uses property of null => if null == false
            if (timeout == null && !disabled) {
                timeout = setTimeout(() => {
                    timeout = null;
                    if(monitor.isOver()) {
                        triggerNavigate();
                    }
                    
                }, time_to_navigate);
            }
        },
    });
    return(
        <div ref={drop}>
            {children}
        </div>
    )
};