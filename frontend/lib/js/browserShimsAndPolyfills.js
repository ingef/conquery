// Remove fetch from window object so polyfill is used
if (window.navigator.userAgent.indexOf("Edge") > -1) window.fetch = undefined;
