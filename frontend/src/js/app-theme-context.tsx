import { createContext, useContext } from "react";

/**
 * What the downstream repo injects at runtime: logo / spinner images and
 * the chart palette. Colors, fonts etc. live in css (see index.css @theme).
 */
export interface AppTheme {
  img: {
    logo: string;
    logoWidth: string;
    logoBackgroundSize: string;
    spinner: string;
  };
  palette: string[];
}

export const AppThemeContext = createContext<AppTheme | null>(null);

export const useAppTheme = (): AppTheme => {
  const theme = useContext(AppThemeContext);
  if (!theme) throw new Error("AppThemeContext not provided");
  return theme;
};
