import {useCallback, useRef, useState} from "react";

export const useOpenCloseInteraction = () => {
    const [isOpen, setIsOpen] = useState<Record<string, boolean>>({});
    const isOpenRef = useRef(isOpen);
    isOpenRef.current = isOpen;

    const toId = useCallback(
        (year: number, quarter?: number) => `${year}-${quarter}`,
        [],
    );

    const getIsOpen = useCallback(
        (year: number, quarter?: number) => {
            if (quarter) {
                return isOpen[toId(year, quarter)];
            } else {
                return [1, 2, 3, 4].every((q) => isOpen[toId(year, q)]);
            }
        },
        [isOpen, toId],
    );

    const toggleOpenYear = useCallback(
        (year: number) => {
            const quarters = [1, 2, 3, 4].map((quarter) => toId(year, quarter));
            const wasOpen = quarters.some((quarter) => isOpenRef.current[quarter]);

            setIsOpen((prev) => ({
                ...prev,
                ...Object.fromEntries(quarters.map((quarter) => [quarter, !wasOpen])),
            }));
        },
        [toId],
    );

    const toggleOpenQuarter = useCallback(
        (year: number, quarter: number) => {
            const id = toId(year, quarter);

            setIsOpen((prev) => ({...prev, [id]: !prev[id]}));
        },
        [toId],
    );

    const closeAll = useCallback(() => {
        setIsOpen({});
    }, []);

    const openAll = useCallback(() => {
        const lastYearsToUse = 20;
        const currYear = new Date().getFullYear();
        const years = [...Array(lastYearsToUse).keys()].map((i) => currYear - i);

        const newIsOpen: Record<string, boolean> = {};

        for (const year of years) {
            for (const quarter of [1, 2, 3, 4]) {
                newIsOpen[toId(year, quarter)] = true;
            }
        }

        setIsOpen(newIsOpen);
    }, [toId]);

    return {
        getIsOpen,
        toggleOpenYear,
        toggleOpenQuarter,
        closeAll,
        openAll,
    };
};
