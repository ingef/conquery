import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { C2, H3 } from "../ui-components/Typography";

const root = tv({
  base: [
    "flex flex-col items-center justify-center",
    "gap-[10px]",
    "h-full w-full",
    "p-5",
  ],
});

const description = tv({ base: ["max-w-[300px]", "text-justify"] });

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
      <H3>{t("error.sorry")}</H3>
      <div className={description()}>
        <C2>{t("error.description")}</C2>
      </div>
      {allowFullRefresh && (
        <>
          <div className={description()}>
            <C2>{t("error.reloadDescription")}</C2>
          </div>
          <div className={reloadButton()}>
            <Button intent="secondary" onPress={() => window.location.reload()}>
              {t("error.reload")}
            </Button>
          </div>
        </>
      )}
      {onReset && (
        <>
          <div className={description()}>
            <C2>{t("error.resetDescription")}</C2>
          </div>
          <div className={reloadButton()}>
            <Button intent="secondary" onPress={onReset}>
              {t("error.reset")}
            </Button>
          </div>
        </>
      )}
    </div>
  );
};
export default ErrorFallback;
