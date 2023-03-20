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

function updateDatasetUploadForm(select) {
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

async function restOptionalForce(url, options) {
  return rest(url, options, false).then((res) => {
    // force button in case of 409 status
    const forceURL = new URL(url, window.location);
    forceURL.searchParams.append('force', true);
    const customButton = createCustomButton('Force delete');
    customButton.onclick = () => rest(forceURL, options).then((res) => {
      res.ok && location.reload();
    });

    showMessageForResponse(res, customButton);
  })
}