<#macro editableText text onChange class="" style="">
  <div class="d-inline-flex align-items-center justify-content-between ${class}" style="width: 200px; gap: 0.25rem; ${style}">
    <i class="fa fa-edit"></i>
    <span
      style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-decoration: underline dotted; flex-grow: 1; cursor: pointer;"
      onclick="$(this).parent().children().toggleClass('d-none')"
      title="${text}"
      data-test-id="editableText-btn"
    >
      ${text}
    </span>
    <form
      class="d-none"
      onsubmit="event.preventDefault(); (${onChange})($(this).children().first().val()); $(this).parent().children().toggleClass('d-none')"
      data-test-id="editableText-form"
    >
      <input
        class="w-100"
        type="text"
        value="${text}"
        data-test-id="editableText-input"
      />
    </form>
  </div>
</#macro>