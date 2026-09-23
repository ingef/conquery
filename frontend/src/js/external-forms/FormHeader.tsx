import { BookIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { C1 } from "../ui-components/Typography";

const root = tv({
  base: ["flex flex-col", "w-full", "gap-[7px]"],
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
      <div className="mx-[10px]">
        <C1>{descriptionText}</C1>
      </div>
      {manualUrl && (
        <a href={manualUrl} target="_blank" rel="noreferrer" className="grid">
          <Button intent="secondary">
            <BookIcon />
            {t("externalForms.manualButton")}
          </Button>
        </a>
      )}
    </div>
  );
};

export default FormHeader;
