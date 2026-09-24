import { ClipboardPasteIcon, FileIcon } from "lucide-react";
import { type ChangeEvent, useEffect, useRef, useState } from "react";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { getUniqueFileRows } from "../common/helpers/fileHelper";
import { Button } from "./Button";
import DropzoneWithFileInput, {
  type DragItemFile,
} from "./DropzoneWithFileInput";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  type ModalProps,
} from "./Modal";
import { C2 } from "./Typography";

const content = tv({
  base: ["flex flex-col", "gap-5"],
});

const textarea = tv({
  base: ["font-mono", "w-full"],
});

const acceptedDropTypes = [NativeTypes.FILE];

const useCanReadClipboard = () => {
  const [canReadClipboard, setCanReadClipboard] = useState(false);
  useEffect(() => {
    if (!navigator.clipboard?.readText) return;

    navigator.permissions
      .query({
        // @ts-ignore https://github.com/microsoft/TypeScript/issues/33923
        name: "clipboard-read",
      })
      .then(({ state }) => setCanReadClipboard(state !== "denied"))
      // Firefox knows no clipboard-read permission, readText prompts on its own
      .catch(() => setCanReadClipboard(true));
  }, []);

  return canReadClipboard;
};

type ImportProps = {
  description?: string;
  placeholder?: string;
  onSubmit: (lines: string[], filename?: string) => void;
};

// rendered only while the modal is open, so nothing here runs for a closed modal
const ImportModalContent = ({
  placeholder,
  description,
  onSubmit,
  close,
}: ImportProps & { close: () => void }) => {
  const { t } = useTranslation();
  const [textInput, setTextInput] = useState("");
  const [droppedFilename, setDroppedFilename] = useState<string>();
  const canReadClipboard = useCanReadClipboard();

  const fileInputRef = useRef<HTMLInputElement>(null);

  const onSubmitClick = () => {
    const lines = textInput
      .split("\n")
      .map((line) => line.trim())
      .filter((line) => line.length > 0);

    onSubmit(lines, droppedFilename);
    close();
  };

  const onOpenFileDialog = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const autoFormatAndSet = (text: string) => {
    let delimiter = "\n";

    if (text.includes(";")) {
      delimiter = ";";
    } else if (text.includes(",")) {
      delimiter = ",";
    }

    const formatted = text
      .split(delimiter)
      .map((part) => part.trim())
      .join("\n");

    setTextInput(formatted);
  };

  const onSelectFile = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    setDroppedFilename(file.name);
    autoFormatAndSet(rows.join("\n"));
  };

  const onPasteClick = async () => {
    // rejects when the user denies the browser's clipboard prompt
    const text = await navigator.clipboard.readText().catch(() => null);

    if (text) autoFormatAndSet(text);
  };

  const onDrop = async ({ files }: DragItemFile) => {
    const file = files[0];
    onSelectFile(file);
  };

  const onChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value;

    // If the user is regularly typing or deleting, don't auto format
    // but if the user is pasting, auto format
    if (val.length - textInput.length < 3) {
      setTextInput(val);
    } else {
      autoFormatAndSet(val);
    }
  };

  return (
    <>
      <ModalHeader subtitle={t("importModal.subtitle")}>
        {t("importModal.headline")}
      </ModalHeader>
      <ModalBody>
        <div className={content()}>
          {description && (
            <C2
              // biome-ignore lint/security/noDangerouslySetInnerHtml: description is our own i18n text
              dangerouslySetInnerHTML={{ __html: description }}
            />
          )}
          <DropzoneWithFileInput
            onDrop={onDrop}
            acceptedDropTypes={acceptedDropTypes}
            disableClick
            accept="text/plain,text/csv"
          >
            {() => (
              <textarea
                className={textarea()}
                rows={15}
                value={textInput}
                onChange={onChange}
                placeholder={placeholder}
              />
            )}
          </DropzoneWithFileInput>
          <input
            className="hidden"
            type="file"
            ref={fileInputRef}
            accept="text/plain,text/csv"
            onChange={(e) => {
              if (e.target.files) {
                onSelectFile(e.target.files[0]);
              }

              if (fileInputRef.current) {
                fileInputRef.current.value = "";
              }
            }}
          />
        </div>
      </ModalBody>
      <ModalFooter>
        <Button intent="tertiary" onPress={onOpenFileDialog}>
          <FileIcon />
          {t("common.openFileDialog")}
        </Button>
        {canReadClipboard && (
          <Button intent="tertiary" onPress={onPasteClick}>
            <ClipboardPasteIcon />
            {t("importModal.paste")}
          </Button>
        )}
        <Button
          intent="primary"
          isDisabled={textInput.length === 0}
          onPress={onSubmitClick}
        >
          {t("importModal.submit")}
        </Button>
      </ModalFooter>
    </>
  );
};

export const ImportModal = ({
  placeholder,
  description,
  onSubmit,
  ...modalProps
}: Pick<ModalProps, "isOpen" | "onOpenChange"> & ImportProps) => (
  <Modal size="lg" {...modalProps}>
    {({ close }) => (
      <ImportModalContent
        placeholder={placeholder}
        description={description}
        onSubmit={onSubmit}
        close={close}
      />
    )}
  </Modal>
);
