import {
  ArrowRightIcon,
  FileIcon,
  FolderIcon,
  MinusIcon,
  WorkflowIcon,
} from "lucide-react";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

const textInitial = tv({
  base: [
    "grid",
    "[grid-template-areas:'free_headline'_'arrow_description']",
    "gap-y-3 gap-x-5",
    "w-full",
    "p-[30px]",
    "text-xl",
    "font-normal",
  ],
});

const arrowRight = tv({
  base: ["[grid-area:arrow]", "size-10", "text-gray-100"],
});

const headline = tv({
  base: ["[grid-area:headline]", "text-2xl", "leading-tight", "font-bold"],
});

const grid = tv({
  base: ["grid items-center", "grid-cols-[auto_1fr]", "gap-[5px]", "mt-[10px]"],
});

const row = tv({
  base: ["flex items-center justify-end", "gap-[6px]"],
});

const iconInABox = tv({
  base: [
    "flex items-center justify-center",
    "h-[30px] w-[30px]",
    "border border-gray-100",
    "rounded",
  ],
});

export const EmptyQueryEditorDropzone = memo(() => {
  const { t } = useTranslation();

  return (
    <div className={textInitial()} data-test-id="text-initial">
      <h2 className={headline()}>{t("dropzone.explanation")}</h2>
      <ArrowRightIcon className={arrowRight()} />
      <div className="[grid-area:description]">
        <p>{t("dropzone.dropIntoThisArea")}</p>
        <div className={grid()}>
          <div className={row()}>
            <div className={iconInABox()}>
              <FolderIcon className="text-primary-500" />
            </div>
            <div className={iconInABox()}>
              <MinusIcon className="text-primary-500" />
            </div>
          </div>
          {t("dropzone.aConcept")}
          <div className={row()}>
            <div className={iconInABox()}>
              <WorkflowIcon className="text-primary-500" />
            </div>
          </div>
          {t("dropzone.aQuery")}
          <div className={row()}>
            <div className={iconInABox()}>
              <FileIcon className="text-primary-500" />
            </div>
          </div>
          {t("dropzone.aConceptList")}
        </div>
      </div>
    </div>
  );
});
