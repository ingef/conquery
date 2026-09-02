import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import { useAppTheme } from "../app-theme-context";
import { HistoryButton } from "../button/HistoryButton";
import DatasetSelector from "../dataset/DatasetSelector";
import { canViewEntityPreview, useHideLogoutButton } from "../user/selectors";
import { HelpMenu } from "./HelpMenu";
import LogoutButton from "./LogoutButton";

// position absolute: fix, so content can expand to 100% and scroll
const root = tv({
  base: [
    "absolute top-0 left-0",
    "z-3",
    "flex flex-row items-center justify-between",
    "w-full",
    "px-5",
    "bg-bg-50",
    "shadow-[0_0_1px_1px_rgba(0,0,0,0.3)]",
  ],
});

const right = tv({
  base: ["flex flex-row items-center", "gap-[5px]"],
});

const overflowHidden = tv({
  base: ["flex flex-row items-center", "shrink-0", "overflow-hidden"],
});

const logo = tv({
  base: ["h-[40px]", "bg-no-repeat", "[background-position-y:50%]"],
});

// the second font-size of the old styles won, hence text-xs and not text-base
const headline = tv({
  base: [
    "mr-auto",
    "text-xs",
    "leading-[2]",
    "font-bold",
    "uppercase",
    "opacity-30",
    "text-primary-500",
  ],
});

const Header = () => {
  const { t } = useTranslation();
  const canViewHistory = useSelector<StateT, boolean>(canViewEntityPreview);
  const hideLogoutButton = useHideLogoutButton();
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
      <div className={right()}>
        <DatasetSelector />
        {canViewHistory && <HistoryButton />}
        {(manualUrl || contactEmail) && (
          <HelpMenu manualUrl={manualUrl} contactEmail={contactEmail} />
        )}
        {!hideLogoutButton && <LogoutButton />}
      </div>
    </header>
  );
};

export default Header;
