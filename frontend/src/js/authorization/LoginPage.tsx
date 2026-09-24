import { CheckIcon, LoaderCircleIcon } from "lucide-react";
import { type FormEvent, useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { tv } from "tailwind-variants";
import { usePostLogin } from "../api/api";
import { useAppTheme } from "../app-theme-context";
import ErrorMessage from "../error-message/ErrorMessage";
import { Button } from "../ui-components/Button";
import { TextField } from "../ui-components/TextField";
import { H3 } from "../ui-components/Typography";

import { AuthTokenContext } from "./AuthTokenProvider";

const root = tv({
  base: ["flex items-center justify-center", "h-screen", "bg-bg-100"],
});

const wrap = tv({
  base: ["flex flex-col items-center justify-center", "max-w-[255px]"],
});

const logo = tv({
  base: ["h-9", "bg-no-repeat", "[background-position-y:50%]"],
});

const form = tv({
  base: [
    "flex flex-col",
    "gap-[10px]",
    "w-[255px]",
    "mx-auto mt-[15px] mb-[50px]",
  ],
});

const submitButton = tv({ base: ["grid", "mt-[25px]"] });

const LoginPage = () => {
  const [user, setUser] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const navigate = useNavigate();
  const postLogin = usePostLogin();
  const { t } = useTranslation();
  const { setAuthToken } = useContext(AuthTokenContext);
  const { img } = useAppTheme();

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setLoading(true);

    try {
      const result = await postLogin(user, password);

      if (result.access_token) {
        setAuthToken(result.access_token);
        navigate("/");

        return;
      }
    } catch {
      setError(true);
    }

    setLoading(false);
  }

  return (
    <div className={root()}>
      <div className={wrap()}>
        <div
          className={logo()}
          style={{
            width: img.logoWidth,
            backgroundImage: `url(${img.logo})`,
            backgroundSize: img.logoBackgroundSize,
          }}
        />
        <H3 as="h2">{t("login.headline")}</H3>
        {!!error && (
          <ErrorMessage className="mx-[10px] mt-5" message={t("login.error")} />
        )}
        <form className={form()} onSubmit={onSubmit}>
          <TextField
            label={t("login.username")}
            value={user}
            onChange={setUser}
            isDisabled={loading}
          />
          <TextField
            type="password"
            label={t("login.password")}
            value={password}
            onChange={setPassword}
            isDisabled={loading}
          />
          <div className={submitButton()}>
            <Button
              intent="primary"
              isDisabled={!user || !password}
              size="lg"
              type="submit"
            >
              {loading ? <LoaderCircleIcon /> : <CheckIcon />}
              {t("login.submit")}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
