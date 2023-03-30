import { ReactNode } from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";

import {
  AuthTokenContextProvider,
  useAuthTokenContextValue,
} from "../authorization/AuthTokenProvider";
import KeycloakProvider from "../authorization/KeycloakProvider";
import LoginPage from "../authorization/LoginPage";
import WithAuthToken from "../authorization/WithAuthToken";
import { basename } from "../environment";

import App from "./App";

const ContextProviders = ({ children }: { children: ReactNode }) => {
  const authTokenContextValue = useAuthTokenContextValue();

  return (
    <AuthTokenContextProvider value={authTokenContextValue}>
      <KeycloakProvider>{children}</KeycloakProvider>
    </AuthTokenContextProvider>
  );
};

const AppRouter = () => {
  return (
    <Router basename={basename}>
      <ContextProviders>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/*"
            element={
              <WithAuthToken>
                <App />
              </WithAuthToken>
            }
          />
        </Routes>
      </ContextProviders>
    </Router>
  );
};

export default AppRouter;
