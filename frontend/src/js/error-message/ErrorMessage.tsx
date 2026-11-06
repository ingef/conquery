import { C2 } from "../ui-components/Typography";

interface Props {
  className?: string;
  message: string;
}

const ErrorMessage = ({ className, message }: Props) => (
  <div className={className}>
    <C2 tone="danger">{message}</C2>
  </div>
);

export default ErrorMessage;
