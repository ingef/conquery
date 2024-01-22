import { ReactNode, useCallback, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";

import ErrorFallback from "./ErrorFallback";

export const ResetableErrorBoundary = ({
  children,
}: {
  children: ReactNode;
}) => {
  const [resetKey, setResetKey] = useState<number>(0);
  const onReset = useCallback(() => setResetKey((key) => key + 1), []);

  return (
    <ErrorBoundary
      resetKeys={[resetKey]}
      onReset={onReset}
      fallbackRender={({ resetErrorBoundary }) => (
        <ErrorFallback onReset={resetErrorBoundary} />
      )}
    >
      {children}
    </ErrorBoundary>
  );
};
