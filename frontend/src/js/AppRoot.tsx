import { I18nProvider } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { Provider } from "react-redux";
import type { Store } from "redux";

import AppRouter from "./app/AppRouter";
import type { StateT } from "./app/reducers";

interface Props {
  store: Store<StateT>;
}

const AppRoot = ({ store }: Props) => {
  const { i18n } = useTranslation();

  return (
    // react-aria formats numbers and dates in the app's language, not the browser's
    <I18nProvider locale={i18n.language}>
      <Provider store={store}>
        <AppRouter />
      </Provider>
    </I18nProvider>
  );
};

export default AppRoot;
