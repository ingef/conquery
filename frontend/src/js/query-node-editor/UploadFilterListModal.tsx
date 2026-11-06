import {
  CircleAlertIcon,
  CircleCheckIcon,
  LoaderCircleIcon,
} from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { PostFilterResolveResponseT } from "../api/types";
import ScrollableList from "../scrollable-list/ScrollableList";
import { Button } from "../ui-components/Button";
import { CheckboxField } from "../ui-components/CheckboxField";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../ui-components/Modal";
import { C2 } from "../ui-components/Typography";

const root = tv({
  base: ["flex flex-col", "gap-[15px]"],
});

const msg = tv({ base: ["flex items-center", "gap-[10px]"] });

const bigIcon = tv({
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
      isOpen
      onOpenChange={(isOpen) => {
        if (!isOpen) onClose();
      }}
    >
      <ModalHeader>{t("uploadFilterListModal.headline")}</ModalHeader>
      <ModalBody>
        <div className={root()}>
          {loading && <LoaderCircleIcon className="text-center" />}
          {error && (
            <div className={msg()}>
              <CircleAlertIcon className={bigIcon({ kind: "error" })} />
              <C2 as="span">{t("uploadConceptListModal.error")}</C2>
            </div>
          )}
          {hasUnresolvedItems && (
            <div className="flex flex-col gap-[5px]">
              <div className={msg()}>
                <CircleAlertIcon className={bigIcon({ kind: "error" })} />
                <C2
                  as="span"
                  // biome-ignore lint/security/noDangerouslySetInnerHtml: i18n text with markup
                  dangerouslySetInnerHTML={{
                    __html: t("uploadConceptListModal.unknownCodes", {
                      count: unresolvedItemsCount,
                    }),
                  }}
                />
              </div>
              <ScrollableList
                maxVisibleItems={3}
                fullWidth
                items={resolved.unknownCodes || []}
              />
            </div>
          )}
          <div className="flex flex-col gap-[5px]">
            {hasResolvedItems && (
              <div className={msg()}>
                <CircleCheckIcon className={bigIcon({ kind: "success" })} />
                <C2 as="span">
                  {t("uploadConceptListModal.resolvedCodes", {
                    count: resolvedItemsCount,
                  })}
                </C2>
              </div>
            )}
            {(resolved.unknownCodes?.length || 0) > 0 && (
              <CheckboxField
                isSelected={includeUnresolved}
                onChange={setIncludeUnresolved}
              >
                {t("uploadConceptListModal.includeUnresolved")}
              </CheckboxField>
            )}
          </div>
        </div>
      </ModalBody>
      <ModalFooter>
        <Button slot="close">{t("common.done")}</Button>
        <Button
          intent="primary"
          isDisabled={loading || nothingToInsert}
          onPress={() => {
            onSubmit(resolved, { includeUnresolved });
            onClose();
          }}
        >
          {t("uploadConceptListModal.insertNode")}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default UploadFilterListModal;
