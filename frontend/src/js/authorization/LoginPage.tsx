import { faCheck, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { type FormEvent, useContext, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { tv } from "tailwind-variants";
import { usePostLogin } from "../api/api";
import { useAppTheme } from "../app-theme-context";
import PrimaryButton from "../button/PrimaryButton";
import ErrorMessage from "../error-message/ErrorMessage";
import FaIcon from "../icon/FaIcon";
import InputPlain from "../ui-components/InputPlain/InputPlain";

import { AuthTokenContext } from "./AuthTokenProvider";

const root = tv({
  base: ["flex items-center justify-center", "h-screen", "bg-bg-100"],
});

const wrap = tv({
  base: ["flex flex-col items-center justify-center", "max-w-[255px]"],
});

const logo = tv({
  base: ["h-[35px]", "bg-no-repeat", "[background-position-y:50%]"],
});

const headline = tv({
  base: [
    "m-0",
    "text-base",
    "leading-[2]",
    "font-light",
    "uppercase",
    "text-gray-500",
  ],
});

const form = tv({
  base: [
    "flex flex-col items-center justify-center",
    "mx-auto mt-[15px] mb-[50px]",
  ],
});

const submitButton = tv({
  base: ["flex items-center justify-center", "mt-[35px]", "w-[255px]"],
});

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
        <h2 className={headline()}>{t("login.headline")}</h2>
        {!!error && (
          <ErrorMessage className="mx-[10px] mt-5" message={t("login.error")} />
        )}
        <form className={form()} onSubmit={onSubmit}>
          <InputPlain
            className="px-0 py-[5px]"
            label={t("login.username")}
            large
            value={user}
            onChange={(value) => setUser(value as string)}
            inputProps={{
              disabled: loading,
            }}
          />
          <InputPlain
            className="px-0 py-[5px]"
            inputType="password"
            label={t("login.password")}
            large
            value={password}
            onChange={(value) => setPassword(value as string)}
            inputProps={{
              disabled: loading,
            }}
          />
          <PrimaryButton
            className={submitButton()}
            disabled={!user || !password}
            large
            type="submit"
          >
            <FaIcon
              className="mr-[10px]"
              large
              white
              icon={loading ? faSpinner : faCheck}
            />
            {t("login.submit")}
          </PrimaryButton>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
