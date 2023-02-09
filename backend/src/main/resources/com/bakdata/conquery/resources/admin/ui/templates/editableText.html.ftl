<#macro editableText text onChange class="">
  <div class="d-inline-flex align-items-center ${class}">
    <form onsubmit="event.preventDefault(); let input = $(this).first(); if (!input.prop('disabled')) { input.prop('disabled', true); input.css('all', 'unset'); (${onChange})(input.value) }">
      <input
        type="text"
        disabled="disabled"
        value="${text}"
        style="all: unset"
      />
      <button type="button" class="btn" onclick="$(this).prev().prop('disabled', false); $(this).prev().css('all', 'initial')">
        <i class="fa fa-edit"></i>
      </button>
      <button type="submit" class="btn">
        <i class="fa fa-check"></i>
      </button>
    </form>
  </div>
</#macro>