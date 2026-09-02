import { tv } from "tailwind-variants";

interface Props {
  className?: string;
  message: string;
}

const root = tv({ base: ["text-red", "font-normal"] });

const ErrorMessage = ({ className, message }: Props) => {
  return <p className={root({ className })}>{message}</p>;
};

export default ErrorMessage;
