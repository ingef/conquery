import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Link } from "../ui-components/Link";
import { C2 } from "../ui-components/Typography";

const root = tv({
  base: ["flex flex-col items-start", "w-full", "gap-2"],
});

interface Props {
  description: string;
  className?: string;
  manualUrl?: string;
}

const FormHeader = ({
  className,
  description: descriptionText,
  manualUrl,
}: Props) => {
  const { t } = useTranslation();
  return (
    <div className={root({ className })}>
      <C2>{descriptionText}</C2>
      {manualUrl && (
        <Link href={manualUrl} external>
          {t("externalForms.manualButton")}
        </Link>
      )}
    </div>
  );
};

export default FormHeader;
