import type { ReactNode } from "react";
import {
  Dialog,
  type DialogRenderProps,
  Heading,
  ModalOverlay,
  type ModalOverlayProps,
  Modal as RacModal,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const overlay = tv({
  base: [
    "fixed inset-0 z-10",
    "flex items-center justify-center",
    "bg-white/50",
    "data-entering:animate-fade-in",
    "data-exiting:animate-fade-out",
  ],
});

const modal = tv({
  base: [
    "mx-5",
    "max-h-[95%] max-w-full",
    "rounded",
    "bg-white",
    "shadow-[0_0_15px_0_rgba(0,0,0,0.2)]",
    "outline-none",
  ],
  variants: {
    // visible overflow lets the menus of the select boxes reach past the card
    scrollable: {
      true: "overflow-y-auto",
      false: "overflow-y-visible",
    },
  },
  defaultVariants: { scrollable: false },
});

const dialog = tv({
  base: ["flex flex-col", "gap-5", "p-[30px]", "text-left", "outline-none"],
});

const header = tv({ base: ["flex flex-col", "gap-[15px]"] });

const heading = tv({
  base: ["flex items-center", "text-lg font-normal text-gray-800"],
});

const subtitleStyle = tv({ base: "max-w-[600px]" });

const body = tv({ base: "min-w-0" });

const footer = tv({ base: ["flex items-center justify-end", "gap-[10px]"] });

export interface ModalProps
  extends Omit<
    ModalOverlayProps,
    "className" | "style" | "children" | "isDismissable"
  > {
  children: ReactNode | ((renderProps: DialogRenderProps) => ReactNode);
  /** names the dialog when it has no ModalHeader */
  "aria-label"?: string;
  /** the card scrolls instead of growing past the viewport */
  scrollable?: boolean;
  "data-test-id"?: string;
}

/**
 * A modal dialog on react-aria-components: it traps focus, closes on Escape
 * and on a click outside, and hands focus back afterwards. Inside a
 * DialogTrigger it opens from the trigger; on its own it is controlled via
 * `isOpen` / `onOpenChange`. A Button with `slot="close"` closes it, so do
 * the render props: `{({ close }) => …}`.
 *
 *   <DialogTrigger>
 *     <Button>Settings</Button>
 *     <Modal>
 *       <ModalHeader subtitle={item.label}>Edit folders</ModalHeader>
 *       <ModalBody>…</ModalBody>
 *       <ModalFooter>
 *         <Button slot="close">Cancel</Button>
 *         <Button intent="primary" onPress={save}>Save</Button>
 *       </ModalFooter>
 *     </Modal>
 *   </DialogTrigger>
 *
 * Header and footer are optional. Layout around the modal is none of the
 * caller's concern; layout inside the body is the caller's.
 */
export const Modal = ({
  children,
  "aria-label": ariaLabel,
  scrollable,
  "data-test-id": dataTestId,
  ...props
}: ModalProps) => (
  <ModalOverlay className={overlay()} isDismissable {...props}>
    <RacModal className={modal({ scrollable })} data-test-id={dataTestId}>
      <Dialog className={dialog()} aria-label={ariaLabel}>
        {children}
      </Dialog>
    </RacModal>
  </ModalOverlay>
);

/** the dialog's title, which also names it for assistive technology */
export const ModalHeader = ({
  children,
  subtitle,
}: {
  children: ReactNode;
  subtitle?: ReactNode;
}) => (
  <header className={header()}>
    <Heading slot="title" className={heading()}>
      {children}
    </Heading>
    {subtitle && <p className={subtitleStyle()}>{subtitle}</p>}
  </header>
);

export const ModalBody = ({ children }: { children: ReactNode }) => (
  <div className={body()}>{children}</div>
);

/** the dialog's actions, right-aligned: a closing button first, the primary one last */
export const ModalFooter = ({ children }: { children: ReactNode }) => (
  <footer className={footer()}>{children}</footer>
);
