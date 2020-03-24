import React, { useState } from "react";
import T from "i18n-react";
import { useDispatch } from "react-redux";
import { push } from "react-router-redux";
import styled from "@emotion/styled";

import InputText from "../form-components/InputText";
import PrimaryButton from "../button/PrimaryButton";
import FaIcon from "../icon/FaIcon";
import { postLogin } from "../api/api";
import { storeAuthToken } from "./helper";
import ErrorMessage from "../error-message/ErrorMessage";

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
  height: ${({ theme }) => theme.img.logoHeight || "35px"};
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

const SxInputText = styled(InputText)`
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
  const dispatch = useDispatch();

  const redirectToApp = () => dispatch(push("/"));

  async function onSubmit(e) {
    e.preventDefault();

    setLoading(true);

    try {
      const result = await postLogin(user, password);

      if (result.access_token) {
        storeAuthToken(result.access_token);
        redirectToApp();

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
        <Headline>{T.translate("login.headline")}</Headline>
        {!!error && <SxErrorMessage message={T.translate("login.error")} />}
        <Form onSubmit={onSubmit}>
          <SxInputText
            inputType="text"
            label={T.translate("login.username")}
            large
            input={{
              value: user,
              onChange: setUser
            }}
            inputProps={{
              disabled: loading
            }}
          />
          <SxInputText
            inputType="password"
            label={T.translate("login.password")}
            large
            input={{
              value: password,
              onChange: setPassword
            }}
            inputProps={{
              disabled: loading
            }}
          />
          <SxPrimaryButton disabled={!user || !password} large type="submit">
            <SxFaIcon large white icon={loading ? "spinner" : "check"} />
            {T.translate("login.submit")}
          </SxPrimaryButton>
        </Form>
      </Wrap>
    </Root>
  );
};

export default LoginPage;
