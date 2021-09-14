import axios, { AxiosRequestConfig } from "axios";
import { useContext, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";

import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import { isIDPEnabled, isLoginDisabled } from "../environment";

export const useApiUnauthorized = <T>(
  requestConfig: Partial<AxiosRequestConfig> = {},
) => {
  return (finalRequestConfig: Partial<AxiosRequestConfig> = {}): Promise<T> =>
    fetchJsonUnauthorized({
      ...requestConfig,
      ...finalRequestConfig,
    });
};

export const useApi = <T>(requestConfig: Partial<AxiosRequestConfig> = {}) => {
  const history = useHistory();
  const { authToken } = useContext(AuthTokenContext);

  // In order to always have the up to date token,
  // especially when polling for a long time within nested loops
  const authTokenRef = useRef<string>(authToken);
  useEffect(
    function updateRef() {
      authTokenRef.current = authToken;
    },
    [authToken],
  );

  return async (
    finalRequestConfig: Partial<AxiosRequestConfig> = {},
  ): Promise<T> => {
    try {
      const response = await fetchJson(authTokenRef.current, {
        ...requestConfig,
        ...finalRequestConfig,
      });

      return response;
    } catch (error) {
      if (
        !isIDPEnabled &&
        !isLoginDisabled &&
        error.status &&
        error.status === 401
      ) {
        history.push("/login");
      }

      throw error;
    }
  };
};

export async function fetchJsonUnauthorized(
  request?: Partial<AxiosRequestConfig>,
  rawBody: boolean = false,
) {
  const finalRequest: AxiosRequestConfig = request
    ? {
        ...request,
        data: rawBody ? request.data : JSON.stringify(request.data),
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
          ...request.headers,
        },
      }
    : {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
      };

  try {
    const response = await axios({
      ...finalRequest,
      validateStatus: () => true, // Don't ever reject the promise for special status codes
    });

    if (
      response.status >= 200 &&
      response.status < 300 &&
      // Also handle empty responses
      (!response.data || (!!response.data && !response.data.error))
    ) {
      return response.data;
    } else {
      // Reject other status
      try {
        return Promise.reject({ status: response.status, ...response.data });
      } catch (e) {
        return Promise.reject({ status: response.status });
      }
    }
  } catch (e) {
    return Promise.reject(e); // Network or connection failure
  }
}

function fetchJson(authToken: string, request?: Partial<AxiosRequestConfig>) {
  const finalRequest = {
    ...(request || {}),
    headers: {
      Authorization: `Bearer ${authToken}`,
      ...((request && request.headers) || {}),
    },
  };

  return fetchJsonUnauthorized(finalRequest);
}
