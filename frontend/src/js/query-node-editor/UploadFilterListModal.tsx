import {
  faCheckCircle,
  faExclamationCircle,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { PostFilterResolveResponseT } from "../api/types";
import PrimaryButton from "../button/PrimaryButton";
import Modal from "../modal/Modal";
import ScrollableList from "../scrollable-list/ScrollableList";
import { Icon } from "../ui-components/Icon";
import InputCheckbox from "../ui-components/InputCheckbox";

const root = tv({
  base: ["flex flex-col", "gap-[15px]", "pb-[10px]"],
});

const msg = tv({
  base: ["m-0", "flex items-center", "gap-[10px]"],
});

const bigIcon = tv({
  base: "text-xl",
  variants: {
    kind: {
      error: "text-red",
      success: "text-green",
    },
  },
});

const selectResolvedItemsCount = (
  resolved: PostFilterResolveResponseT | null,
) => {
  return resolved?.resolvedFilter?.value?.length || 0;
};

const selectUnresolvedItemsCount = (
  resolved: PostFilterResolveResponseT | null,
) => {
  return resolved?.unknownCodes?.length ? resolved.unknownCodes.length : 0;
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
      <div className={root()}>
        {loading && <Icon icon={faSpinner} className="text-center" />}
        {error && (
          <p>
            <Icon
              icon={faExclamationCircle}
              className={bigIcon({ kind: "error" })}
            />
            {t("uploadConceptListModal.error")}
          </p>
        )}
        {hasUnresolvedItems && (
          <div className="flex flex-col gap-[5px]">
            <p className={msg()}>
              <Icon
                icon={faExclamationCircle}
                className={bigIcon({ kind: "error" })}
              />
              <span
                // biome-ignore lint/security/noDangerouslySetInnerHtml: i18n text with markup
                dangerouslySetInnerHTML={{
                  __html: t("uploadConceptListModal.unknownCodes", {
                    count: unresolvedItemsCount,
                  }),
                }}
              />
            </p>
            <ScrollableList
              maxVisibleItems={3}
              fullWidth
              items={resolved.unknownCodes || []}
            />
          </div>
        )}
        <div className="flex flex-col gap-[5px]">
          {hasResolvedItems && (
            <p className={msg()}>
              <Icon
                icon={faCheckCircle}
                className={bigIcon({ kind: "success" })}
              />
              {t("uploadConceptListModal.resolvedCodes", {
                count: resolvedItemsCount,
              })}
            </p>
          )}
          {(resolved.unknownCodes?.length || 0) > 0 && (
            <InputCheckbox
              value={includeUnresolved}
              onChange={setIncludeUnresolved}
              label={t("uploadConceptListModal.includeUnresolved")}
            />
          )}
        </div>
        <PrimaryButton
          disabled={loading || nothingToInsert}
          onClick={() => {
            onSubmit(resolved, { includeUnresolved });
            onClose();
          }}
        >
          {t("uploadConceptListModal.insertNode")}
        </PrimaryButton>
      </div>
    </Modal>
  );
};

export default UploadFilterListModal;
