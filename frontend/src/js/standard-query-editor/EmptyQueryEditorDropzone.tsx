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

import { C1, H2 } from "../ui-components/Typography";

const textInitial = tv({
  base: [
    "grid",
    "[grid-template-areas:'free_headline'_'arrow_description']",
    "gap-y-3 gap-x-5",
    "w-full",
    "p-[30px]",
    "text-base",
  ],
});

const arrowRight = tv({
  base: ["[grid-area:arrow]", "size-10", "text-gray-100"],
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
      <div className="[grid-area:headline]">
        <H2>{t("dropzone.explanation")}</H2>
      </div>
      <ArrowRightIcon className={arrowRight()} />
      <div className="[grid-area:description]">
        <C1>{t("dropzone.dropIntoThisArea")}</C1>
        <div className={grid()}>
          <div className={row()}>
            <div className={iconInABox()}>
              <FolderIcon data-filled className="text-primary-500" />
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
