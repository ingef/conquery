import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import type { PostFilterResolveResponseT } from "../api/types";
import FaIcon from "../icon/FaIcon";
import Modal from "../modal/Modal";
import ScrollableList from "../scrollable-list/ScrollableList";

const Root = styled("div")`
  padding: 0 0 10px;
`;
const Section = styled("div")`
  padding: 10px 20px;
`;
const Msg = styled("p")`
  margin: 10px 0 5px;
`;
const BigIcon = styled(FaIcon)`
  font-size: 20px;
  margin-right: 10px;
`;
const ErrorIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.red};
`;
const SuccessIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.green};
`;
const CenteredIcon = styled(FaIcon)`
  text-align: center;
`;

interface PropsT {
  loading: boolean;
  resolved: PostFilterResolveResponseT | null;
  error: boolean;
  onClose: () => void;
}

const selectResolvedItemsCount = (
  resolved: PostFilterResolveResponseT | null,
) => {
  return resolved?.resolvedFilter?.value?.length || 0;
};

const selectUnresolvedItemsCount = (
  resolved: PostFilterResolveResponseT | null,
) => {
  return resolved && resolved.unknownCodes && resolved.unknownCodes.length
    ? resolved.unknownCodes.length
    : 0;
};

const UploadFilterListModal: FC<PropsT> = ({
  loading,
  resolved,
  error,
  onClose,
}) => {
  const { t } = useTranslation();
  const resolvedItemsCount = selectResolvedItemsCount(resolved);
  const unresolvedItemsCount = selectUnresolvedItemsCount(resolved);

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  return (
    <Modal
      onClose={onClose}
      doneButton
      headline={t("uploadFilterListModal.headline")}
    >
      <Root>
        {loading && <CenteredIcon icon="spinner" />}
        {error && (
          <p>
            <ErrorIcon icon="exclamation-circle" />
            {t("uploadConceptListModal.error")}
          </p>
        )}
        {resolved && (
          <Section>
            {hasResolvedItems && (
              <Msg>
                <SuccessIcon icon="check-circle" />
                {t("uploadConceptListModal.resolvedCodes", {
                  count: resolvedItemsCount,
                })}
              </Msg>
            )}
            {hasUnresolvedItems && (
              <>
                <Msg>
                  <ErrorIcon icon="exclamation-circle" />
                  <span
                    dangerouslySetInnerHTML={{
                      __html: t("uploadConceptListModal.unknownCodes", {
                        count: unresolvedItemsCount,
                      }),
                    }}
                  />
                </Msg>
                <ScrollableList
                  maxVisibleItems={3}
                  fullWidth
                  items={resolved.unknownCodes || []}
                />
              </>
            )}
          </Section>
        )}
      </Root>
    </Modal>
  );
};

export default UploadFilterListModal;
