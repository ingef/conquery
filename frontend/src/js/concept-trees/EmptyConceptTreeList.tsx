import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

const msgContainer = tv({
  base: ["flex flex-col items-start justify-center", "w-full", "h-full"],
});

const preview = tv({
  base: ["rounded", "bg-gray-50", "h-5", "my-[3px]"],
});

const EmptyConceptTreeList = () => {
  const { t } = useTranslation();

  return (
    <div className="relative ml-[10px] flex w-full flex-col">
      <div className={msgContainer()}>
        <div className="w-[400px] whitespace-normal">
          <p className="mt-[10px] text-xl font-normal">
            {t("conceptTreeList.noTrees")}
          </p>
          <p className="mb-[10px] text-base">
            {t("conceptTreeList.noTreesExplanation")}
          </p>
        </div>
      </div>
      <div className={preview()} style={{ width: `${200}px` }} />
      <div className={preview()} style={{ width: `${100}px` }} />
      <div className="flex flex-col pl-5">
        <div className={preview()} style={{ width: `${250}px` }} />
        <div className={preview()} style={{ width: `${150}px` }} />
        <div className={preview()} style={{ width: `${300}px` }} />
        <div className="flex flex-col pl-5">
          <div className={preview()} style={{ width: `${200}px` }} />
          <div className={preview()} style={{ width: `${50}px` }} />
        </div>
      </div>
      <div className={preview()} style={{ width: `${350}px` }} />
      <div className={preview()} style={{ width: `${200}px` }} />
      <div className={preview()} style={{ width: `${300}px` }} />
      <div className={preview()} style={{ width: `${250}px` }} />
    </div>
  );
};

export default EmptyConceptTreeList;
