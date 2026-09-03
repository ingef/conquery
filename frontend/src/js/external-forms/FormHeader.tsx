import { faBook } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

const root = tv({
  base: ["flex flex-col", "w-full", "gap-[7px]"],
});

const description = tv({
  base: ["mx-[10px]", "text-base"],
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
      <p className={description()}>{descriptionText}</p>
      {manualUrl && (
        <a href={manualUrl} target="_blank" rel="noreferrer" className="grid">
          <Button intent="secondary">
            <Icon icon={faBook} />
            {t("externalForms.manualButton")}
          </Button>
        </a>
      )}
    </div>
  );
};

export default FormHeader;
