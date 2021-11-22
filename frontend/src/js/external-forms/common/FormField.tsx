import styled from "@emotion/styled";

const TheFormField = styled("div")`
  margin: 0 0 10px;
`;

const FormField = (Component) => (props) =>
  (
    <TheFormField>
      <Component {...props} />
    </TheFormField>
  );

export default FormField;
