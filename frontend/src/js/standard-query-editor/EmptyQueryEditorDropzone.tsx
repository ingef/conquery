import { faFile } from "@fortawesome/free-regular-svg-icons";
import {
  faArrowRight,
  faDiagramProject,
  faFolder,
  faMinus,
} from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import FaIcon from "../icon/FaIcon";

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
  base: ["[grid-area:arrow]", "text-[140px]", "text-gray-100"],
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
      <FaIcon className={arrowRight()} icon={faArrowRight} />
      <div className="[grid-area:description]">
        <p>{t("dropzone.dropIntoThisArea")}</p>
        <div className={grid()}>
          <div className={row()}>
            <div className={iconInABox()}>
              <FaIcon icon={faFolder} active />
            </div>
            <div className={iconInABox()}>
              <FaIcon icon={faMinus} active />
            </div>
          </div>
          {t("dropzone.aConcept")}
          <div className={row()}>
            <div className={iconInABox()}>
              <FaIcon icon={faDiagramProject} active />
            </div>
          </div>
          {t("dropzone.aQuery")}
          <div className={row()}>
            <div className={iconInABox()}>
              <FaIcon icon={faFile} active />
            </div>
          </div>
          {t("dropzone.aConceptList")}
        </div>
      </div>
    </div>
  );
});
