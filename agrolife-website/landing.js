'use strict';
// Keep the FAQ compact while preserving the native, keyboard-accessible details UI.
document.querySelectorAll('.faq-items details').forEach(item => {
  item.addEventListener('toggle', () => {
    if (item.open) document.querySelectorAll('.faq-items details').forEach(other => {
      if (other !== item) other.open = false;
    });
  });
});
