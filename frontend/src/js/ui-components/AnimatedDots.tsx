import { tv } from "tailwind-variants";

const dot = tv({
  base: [
    "animate-blink",
    "nth-of-type-2:[animation-delay:250ms]",
    "nth-of-type-3:[animation-delay:500ms]",
  ],
});

export default function AnimatedDots() {
  return (
    <span className="font-bold">
      <span className={dot()}>.</span>
      <span className={dot()}>.</span>
      <span className={dot()}>.</span>
    </span>
  );
}
