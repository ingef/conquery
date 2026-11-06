import logo from "./images/conquery-logo.png";
import spinner from "./images/spinner.png";
import type { AppTheme } from "./js/app-theme-context";

/**
 * Colors, fonts, radii etc. live in css (see index.css @theme).
 * This is only what has to be injected at runtime: images and chart palette.
 * The downstream repo provides its own version of this object.
 */
export const theme: AppTheme = {
  img: {
    logo: logo,
    logoWidth: "172px",
    logoBackgroundSize: "172px 40px",
    spinner: spinner,
  },
  palette: [
    "#277da1",
    "#43aa8b",
    "#5e60ce",
    "#f9c74f",
    "#90be6d",
    "#f8961e",
    "#f94144",
    "#aaa",
    "#777",
    "#fff",
  ],
};
