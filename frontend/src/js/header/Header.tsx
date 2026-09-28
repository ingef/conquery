import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import { useAppTheme } from "../app-theme-context";
import DatasetSelector from "../dataset/DatasetSelector";
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

const Header = () => {
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
      </div>
      <div className="flex items-center gap-[5px]">
        <DatasetSelector />
        <HeaderMenu manualUrl={manualUrl} contactEmail={contactEmail} />
      </div>
    </header>
  );
};

export default Header;
