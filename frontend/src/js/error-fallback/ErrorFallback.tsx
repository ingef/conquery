import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { TransparentButton } from "../button/TransparentButton";

const root = tv({
  base: [
    "flex flex-col items-center justify-center",
    "gap-[10px]",
    "h-full w-full",
    "p-5",
  ],
});

const heading = tv({ base: ["m-0", "text-base"] });

const description = tv({
  base: ["m-0", "max-w-[300px]", "text-sm", "text-justify"],
});

const reloadButton = tv({ base: "mt-[10px]" });

const ErrorFallback = ({
  allowFullRefresh,
  onReset,
}: {
  allowFullRefresh?: boolean;
  onReset?: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <div className={root()}>
      <h3 className={heading()}>{t("error.sorry")}</h3>
      <p className={description()}>{t("error.description")}</p>
      {allowFullRefresh && (
        <>
          <p className={description()}>{t("error.reloadDescription")}</p>
          <TransparentButton
            className={reloadButton()}
            onClick={() => window.location.reload()}
          >
            {t("error.reload")}
          </TransparentButton>
        </>
      )}
      {onReset && (
        <>
          <p className={description()}>{t("error.resetDescription")}</p>
          <TransparentButton className={reloadButton()} onClick={onReset}>
            {t("error.reset")}
          </TransparentButton>
        </>
      )}
    </div>
  );
};
export default ErrorFallback;
