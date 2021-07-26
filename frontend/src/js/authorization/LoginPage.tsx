import styled from "@emotion/styled";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

import { usePostLogin } from "../api/api";
import PrimaryButton from "../button/PrimaryButton";
import ErrorMessage from "../error-message/ErrorMessage";
import InputPlain from "../form-components/InputPlain";
import FaIcon from "../icon/FaIcon";

import { storeAuthToken } from "./helper";

const Root = styled("div")`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: ${({ theme }) => theme.col.bgAlt};
`;

const Wrap = styled("div")`
  max-width: 255px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
`;

const Logo = styled("div")`
  width: ${({ theme }) => theme.img.logoWidth};
  height: 35px;
  background-image: url(${({ theme }) => theme.img.logo});
  background-repeat: no-repeat;
  background-position-y: 50%;
  background-size: ${({ theme }) => theme.img.logoBackgroundSize};
`;

const Headline = styled("h2")`
  line-height: 2;
  font-size: ${({ theme }) => theme.font.md};
  text-transform: uppercase;
  font-weight: 300;
  color: ${({ theme }) => theme.col.gray};
  margin: 0;
`;

const Form = styled("form")`
  margin: 15px auto 50px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
`;

const SxInputPlain = styled(InputPlain)`
  padding: 10px 0;
`;

const SxPrimaryButton = styled(PrimaryButton)`
  margin-top: 35px;
  width: 255px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const SxFaIcon = styled(FaIcon)`
  margin-right: 10px;
`;

const SxErrorMessage = styled(ErrorMessage)`
  margin: 20px 10px 0;
`;

const LoginPage = () => {
  const [user, setUser] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const history = useHistory();
  const postLogin = usePostLogin();
  const { t } = useTranslation();

  async function onSubmit(e: any) {
    e.preventDefault();

    setLoading(true);

    try {
      const result = await postLogin(user, password);

      if (result.access_token) {
        storeAuthToken(result.access_token);
        history.push("/");

        return;
      }
    } catch (e) {
      setError(true);
    }

    setLoading(false);
  }

  return (
    <Root>
      <Wrap>
        <Logo />
        <Headline>{t("login.headline")}</Headline>
        {!!error && <SxErrorMessage message={t("login.error")} />}
        <Form onSubmit={onSubmit}>
          <SxInputPlain
            label={t("login.username")}
            large
            input={{
              value: user,
              onChange: setUser,
            }}
            inputProps={{
              disabled: loading,
            }}
          />
          <SxInputPlain
            inputType="password"
            label={t("login.password")}
            large
            input={{
              value: password,
              onChange: setPassword,
            }}
            inputProps={{
              disabled: loading,
            }}
          />
          <SxPrimaryButton disabled={!user || !password} large type="submit">
            <SxFaIcon large white icon={loading ? "spinner" : "check"} />
            {t("login.submit")}
          </SxPrimaryButton>
        </Form>
      </Wrap>
    </Root>
  );
};

export default LoginPage;
