import {Meta, StoryObj} from "@storybook/react";
import {ComponentProps, useState} from "react";

import wordslist from "../../../fixtures/words.json";
import {SelectOptionT} from "../../api/types";

import InputMultiSelect from "./InputMultiSelect";

const wl = wordslist.slice(0, 100);
let offset = 100;

export default {
    title: "FormComponents/InputMultiSelect",
    component: InputMultiSelect,
    argTypes: {
        backgroundColor: {control: "#fafafa"},
    },
} as Meta<typeof InputMultiSelect>;

type Props = ComponentProps<typeof InputMultiSelect> & {
    passOnResolve?: boolean;
};

const Render = ({passOnResolve, ...args}: Props) => {
    const [loading, setLoading] = useState<boolean>(false);
    const [options, setOptions] = useState<SelectOptionT[]>(
        wl.map((w) => ({label: w, value: w, disabled: Math.random() < 0.1})),
    );
    const [value, setValue] = useState<SelectOptionT[] | null>([
        {
            label: "Option 1",
            value: "option1",
        },
    ]);
    const onLoad = (str: string) => {
        setLoading(true);
        setTimeout(() => {
            setOptions((opts) => {
                const next = Array.from(
                    new Set([
                        ...opts.map((v) => v.value),
                        ...wordslist
                            .slice(offset, offset + 100)
                            .filter((w) => w.startsWith(str)),
                    ]),
                );

                return next.map((w) => ({label: String(w), value: w}));
            });
            offset += 100;
            setLoading(false);
        }, 500);
    };

    const onResolve = (csvLines: string[]) => {
        setValue(csvLines.map((line) => ({value: line, label: line})));
    };

    return (
        <InputMultiSelect
            {...args}
            options={options}
            value={value || []}
            onChange={(v) => setValue(v)}
            loading={args.loading || loading}
            onResolve={passOnResolve ? onResolve : undefined}
            onLoadMore={onLoad}
        />
    );
};

type Story = StoryObj<Props>;

export const Default: Story = {
    args: {
        indexPrefix: 5,
        label: "This is a nice label",
        tooltip:
            "And here goes some tooltip that really helps the user understand what's going on",
        disabled: false,
        passOnResolve: true,
        creatable: true,
        loading: false,
    },
    argTypes: {
        passOnResolve: {
            type: {name: "boolean"},
        },
        indexPrefix: {
            type: {name: "number", required: false},
        },
    },
    render: Render,
};
