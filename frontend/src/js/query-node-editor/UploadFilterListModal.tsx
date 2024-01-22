import styled from "@emotion/styled";
import {
  faCheckCircle,
  faExclamationCircle,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import { useState } from "react";
import type { PostFilterResolveResponseT } from "../api/types";
import PrimaryButton from "../button/PrimaryButton";
import FaIcon from "../icon/FaIcon";
import Modal from "../modal/Modal";
import ScrollableList from "../scrollable-list/ScrollableList";
import InputCheckbox from "../ui-components/InputCheckbox";

const Root = styled("div")`
  padding: 0 0 10px;
  display: flex;
  flex-direction: column;
  gap: 15px;
`;
const Col = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const Msg = styled("p")`
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
`;
const BigIcon = styled(FaIcon)`
  font-size: 20px;
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

const UploadFilterListModal = ({
  loading,
  resolved,
  error,
  onSubmit,
  onClose,
}: {
  loading: boolean;
  resolved: PostFilterResolveResponseT;
  error: boolean;
  onSubmit: (
    resolved: PostFilterResolveResponseT,
    { includeUnresolved }: { includeUnresolved: boolean },
  ) => void;
  onClose: () => void;
}) => {
  const { t } = useTranslation();
  const [includeUnresolved, setIncludeUnresolved] = useState(false);

  const resolvedItemsCount = selectResolvedItemsCount(resolved);
  const unresolvedItemsCount = selectUnresolvedItemsCount(resolved);

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  const nothingToInsert =
    (!hasUnresolvedItems && !hasResolvedItems) ||
    (!hasResolvedItems && !includeUnresolved);

  return (
    <Modal
      onClose={onClose}
      doneButton
      headline={t("uploadFilterListModal.headline")}
    >
      <Root>
        {loading && <CenteredIcon icon={faSpinner} />}
        {error && (
          <p>
            <ErrorIcon icon={faExclamationCircle} />
            {t("uploadConceptListModal.error")}
          </p>
        )}
        {hasUnresolvedItems && (
          <Col>
            <Msg>
              <ErrorIcon icon={faExclamationCircle} />
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
          </Col>
        )}
        <Col>
          {hasResolvedItems && (
            <Msg>
              <SuccessIcon icon={faCheckCircle} />
              {t("uploadConceptListModal.resolvedCodes", {
                count: resolvedItemsCount,
              })}
            </Msg>
          )}
          {(resolved.unknownCodes?.length || 0) > 0 && (
            <InputCheckbox
              value={includeUnresolved}
              onChange={setIncludeUnresolved}
              label={t("uploadConceptListModal.includeUnresolved")}
            />
          )}
        </Col>
        <PrimaryButton
          disabled={loading || nothingToInsert}
          onClick={() => {
            onSubmit(resolved, { includeUnresolved });
            onClose();
          }}
        >
          {t("uploadConceptListModal.insertNode")}
        </PrimaryButton>
      </Root>
    </Modal>
  );
};

export default UploadFilterListModal;
