import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import { useAppTheme } from "../app-theme-context";
import DatasetSelector from "../dataset/DatasetSelector";
import { textStyle } from "../ui-components/Typography";
import { HeaderMenu } from "./HeaderMenu";

// position absolute: fix, so content can expand to 100% and scroll
const root = tv({
  base: [
    "absolute top-0 left-0",
    "z-3",
    "flex flex-row items-center justify-between",
    "h-header w-full",
    "px-5",
    "bg-bg-50",
    "shadow-[0_0_1px_1px_rgba(0,0,0,0.3)]",
  ],
});

const overflowHidden = tv({
  base: ["flex flex-row items-center", "shrink-0", "overflow-hidden"],
});

const logo = tv({
  base: ["h-header", "bg-no-repeat", "[background-position-y:50%]"],
});

const headline = tv({
  base: ["mr-auto", textStyle({ size: 3, tone: "muted" })],
});

const Header = () => {
  const { t } = useTranslation();
  const { manualUrl, contactEmail } = useSelector<
    StateT,
    StateT["startup"]["config"]
  >((state) => state.startup.config);

  const { img } = useAppTheme();

  return (
    <header className={root()}>
      <div className={overflowHidden()}>
        <div
          className={logo()}
          style={{
            width: img.logoWidth,
            backgroundImage: `url(${img.logo})`,
            backgroundSize: img.logoBackgroundSize,
          }}
        />
        <span className="mx-[5px] h-5" />
        <h1 className={headline()}>{t("headline")}</h1>
      </div>
      <div className="flex items-center gap-[5px]">
        <DatasetSelector />
        <HeaderMenu manualUrl={manualUrl} contactEmail={contactEmail} />
      </div>
    </header>
  );
};

export default Header;
