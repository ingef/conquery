# CSRF-Filter

This filters prevents cross-site-request-forgery attempts which are conducted by luring a browser user to a malicious
website.
The user's browser needs be authenticated via an authentication cookie.
Because we have two servlets (UI + API) for the admin-end there are two different filters.

The prevention method used is the
stateless [Signed Double-Submit Cookie](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#signed-double-submit-cookie-recommended).

## [CsrfTokenSetFilter](./CsrfTokenSetFilter.java)

This filter is installed on the UI-servlet. It generates a csrf-token and injects the plaintext token into the payload
and the signed/hashed token into a cookie.
The UI-servlet provides all html resources for the browser.

## [CsrfTokenCheckFilter](./CsrfTokenCheckFilter.java)

This filter is installed on the API-servlet. It extracts the double submitted token from a requests and validates them.
If the request did not contain a csrf token cookie (e.g. request was initiated by cURL) the filter does nothing.
If a csrf check was successful, the request is marked.

This filter is not installed on the UI-servlet. Even though this servlet's responses contain sensitive data, it only
provides GET-endpoints, which are not state altering and thus don't need csrf protection. On the API-servlet side we
validate all requests for simplicity.

## [AuthCookieFilter](../AuthCookieFilter.java)

The mark set by the CsrfTokenCheckFilter is then checked by the AuthCookieFilter, when it encounters an auth-cookie.
If an AuthCookie is provided, but no csrf check happened before, the request is rejected.