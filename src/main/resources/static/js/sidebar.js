document.addEventListener('DOMContentLoaded', function() {
    const sidebar = document.querySelector('.sidebar');
    const toggleBtn = document.createElement('button');
    toggleBtn.innerHTML = '☰';
    toggleBtn.style.cssText = 'position:absolute;top:10px;right:-40px;background:#4f4fc1;color:#fff;border:none;padding:10px;border-radius:5px;cursor:pointer;';
    sidebar.appendChild(toggleBtn);

    toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('collapsed');
    });
});


document.querySelectorAll(".accordion").forEach(btn => {
  btn.addEventListener("click", () => {
    // Cerrar otros paneles (comportamiento tipo accordion)
    document.querySelectorAll(".accordion").forEach(otherBtn => {
      if (otherBtn !== btn && otherBtn.classList.contains("active")) {
        otherBtn.classList.remove("active");
        otherBtn.nextElementSibling.style.maxHeight = null;
        otherBtn.nextElementSibling.classList.remove("open");
      }
    });

    btn.classList.toggle("active");
    let panel = btn.nextElementSibling;
    if (panel.style.maxHeight) {
      panel.style.maxHeight = null;
      panel.classList.remove("open");
    } else {
      panel.style.maxHeight = panel.scrollHeight + "px";
      panel.classList.add("open");
    }
  });
});

// Auto-abrir el grupo que contiene el item activo
document.addEventListener('DOMContentLoaded', function() {
  const activeItem = document.querySelector('.panel .nav-item.active');
  if (activeItem) {
    const panel = activeItem.closest('.panel');
    const accordion = panel.previousElementSibling;
    if (accordion && accordion.classList.contains('accordion')) {
      accordion.classList.add('active');
      panel.style.maxHeight = panel.scrollHeight + "px";
      panel.classList.add("open");
    }
  }
});
