# MIMIC III Demo Tutorial
This tutorial demonstrates how parts of the [MIMIC-III Demo Dataset](https://physionet.org/content/mimiciii-demo/1.4/) are prepared for analysis in conquery.

## Preparation

Make sure you have the [required build tools](../../README.md#conquery#requirements) ready (or your container environment prepared) and python (3.x) installed. We recommend to create a [virtual environment](https://docs.python.org/3/library/venv.html) and install the required libraries by running: `pip install -r requirements.txt`.

## Tutorial Overview

The tutorials
- [Age and Gender](./age_gender.ipynb) and
- [ICD9](./icd9.ipynb)
show how meta data is generated, that enable the drag'n'drop analysis of conquery.

The subsequent tutorial [Preprocess and Upload](./preprocess_and_upload.ipynb) shows the preprocessing step and uploads all files into a conquery backend.
Finally you can start you first analysis in the frontend.

## Start the Tutorial

Open Jupyter Notebooks from this folder using
```bash
$ jupyter notebook
```
and start with one of the meta-data notebooks.