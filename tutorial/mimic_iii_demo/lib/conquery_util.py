from enum import Enum, auto
import re
import numpy as np
from jsonschema import Draft7Validator, RefResolver
import json
from typing import List

class CQTypes(Enum):
    STRING = auto()
    INTEGER = auto()
    BOOLEAN = auto()
    REAL= auto()
    DECIMAL= auto()
    MONEY= auto()
    DATE= auto()
    DATE_RANGE= auto()

class DateRangeColumn:
    """
        A virtual column that combines a lower bound date (start_column) 
        and an upper date (end_column) to span a date range.
        A date in the start_column must be equal or less than the corresponding
        date in the end column.
    """
    def __init__(self, name: str, start_column: str, end_column: str):
        self.name = name
        self.start_column = start_column
        self.end_column = end_column
        pass

    def generate_table_column(self): 
        return {
            "name": self.name,
            "type" : CQTypes.DATE_RANGE.name
        }

    def generate_import_column(self): 
        return {
                "operation": "COMPOUND_DATE_RANGE",
                "startColumn": self.start_column,
                "endColumn": self.end_column,
                "name": self.name,
        }

def get_csv_name(url: str) -> str:
    filename_matcher = re.compile(r"[\w\d_-]+\.csv")
    match = filename_matcher.search(url)
    if not match:
        raise ValueError(f"Unable to extract file name from {url}")
    return match.group(0)


def typeConverter(dtype) -> CQTypes :
    if np.issubdtype(dtype, np.object) :
        return CQTypes.STRING.name
    if np.issubdtype(dtype, np.integer) :
        return CQTypes.INTEGER.name
    if np.issubdtype(dtype, np.bool_) :
        return CQTypes.BOOLEAN
    if np.issubdtype(dtype, np.inexact) :
        return CQTypes.REAL
    # DECIMAL cannot be derived from the dtype because there is no analogon
    # MONEY cannot be derived from the dtype because it is a semantic rather than a logical type
    if np.issubdtype(dtype, np.datetime64):
        return CQTypes.DATE.name
    # DATE_RANGE not supported here yet
    raise ValueError(f"Encountered unhandled dtype: {dtype}")

def generate_table_column(name, dtype) :
    return {
        "name": name,
        "type" : typeConverter(dtype)
    }

def generate_table(name, df, primary_column: str, extra: List[DateRangeColumn] = []) :
    columns = [ generate_table_column(name, dtype) for name, dtype in zip(df.dtypes.keys().array, df.dtypes.values) if name != primary_column]
    columns = [*columns, *[col_descr.generate_table_column() for col_descr in extra]]
    return {
        "name" : name,
        "columns": columns
    }

def generate_import_column(name, dtype) :
    return {
        "inputColumn": name,
        "inputType": typeConverter(dtype),
        "name": name,
        "operation": "COPY"
    }

def generate_import(df, primary_column, source_file, extra: List[DateRangeColumn] = []) :

    col_names = list(df.columns.values)
    col_names.remove(primary_column)
    non_primary_df = df[col_names]

    # Skip the filename suffix
    table_label = source_file.name.split(".")[0]

    outputs = [ generate_import_column(name, dtype) for name, dtype in zip(non_primary_df.dtypes.keys().array, non_primary_df.dtypes.values)]
    outputs = [*outputs,  *[col_descr.generate_import_column() for col_descr in extra]]

    return {
        "inputs": [
            {
                "output": outputs,
                "primary": {
                    **generate_import_column(primary_column, df[[primary_column]].dtypes.values[0]),
                    "required": True,
                },
                "sourceFile": source_file.as_posix()
            }
        ],
        "table": table_label,
        "name": table_label
    }


"""
Create a validator from a base schema in the directory "./json_schema"
"""
def get_validator(base_schema_file):
    schema_store = {}

    directory = base_schema_file.parent
        
    for file in list(directory.glob("*.json")):
        
        with open(file, "r") as schema_file:
            schema = json.load(schema_file)
            schema_store[file.name] = schema

    resolver = RefResolver.from_schema(schema_store[base_schema_file.name], store=schema_store)
    return Draft7Validator(schema_store[base_schema_file.name], resolver=resolver)