function updateDatasetUploadForm(select) {
  const uploadFormMapping = {
    mapping: {
      name: "mapping",
      uri: "internToExtern",
      accept: "*.mapping.json",
    },
    table: { name: "table_schema", uri: "tables", accept: "*.table.json" },
    concept: {
      name: "concept_schema",
      uri: "concepts",
      accept: "*.concept.json",
    },
    structure: {
      name: "structure_schema",
      uri: "structure",
      accept: "structure.json",
    },
  };

  const data = uploadFormMapping[select.value];
  const fileInput = $(select).next();
  fileInput.value = "";
  fileInput.attr("accept", data.accept);
  fileInput.attr("name", data.name);
  $(select)
    .parent()
    .attr(
      "onsubmit",
      "postFile(event, '/admin/datasets/${c.ds.id}/" + data.uri + "')"
    );
}
