import {useMemo} from "react";
import {useTranslation} from "react-i18next";

export const useDefaultStatusOptions = () => {
    const {t} = useTranslation();

    return useMemo(
        () => [
            {
                label: t("history.options.check"),
                value: t("history.options.check") as string,
            },
            {
                label: t("history.options.noCheck"),
                value: t("history.options.noCheck") as string,
            },
        ],
        [t],
    );
};
