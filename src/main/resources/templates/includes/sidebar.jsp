<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8" %>
<%@ page import="java.util.List, java.util.ArrayList" %>
<%
    // Permisos cacheados por PermisoFilter (IDs de los permisos del usuario)
    @SuppressWarnings("unchecked")
    List<Integer> _permisos = (List<Integer>) session.getAttribute("permisosUsuario");
    if (_permisos == null) _permisos = new ArrayList<>();

    // IDs según tabla permiso
    boolean pUsuarios      = _permisos.contains(1);
    boolean pVoluntarios   = _permisos.contains(2);
    boolean pBeneficiarios = _permisos.contains(3);
    boolean pActividades   = _permisos.contains(4);
    boolean pAsistencias   = _permisos.contains(5);
    boolean pCertificados  = _permisos.contains(6);
    boolean pCalendario    = _permisos.contains(7);
    boolean pDonaciones    = _permisos.contains(8);
    boolean pInventario    = _permisos.contains(9);
    boolean pTesoreria     = _permisos.contains(10);
    boolean pReportes      = _permisos.contains(11);

    // Grupos visibles
    boolean grupoPersonas    = pUsuarios || pVoluntarios || pBeneficiarios;
    boolean grupoActividades = pActividades || pAsistencias || pCertificados;
    boolean grupoAgenda      = pCalendario;
    boolean grupoDonaciones  = pDonaciones || pInventario;
    boolean grupoFinanciero  = pTesoreria;
    boolean grupoInformes    = pReportes;
%>

<link
  rel="stylesheet"
  href="${pageContext.request.contextPath}/css/sidebar.css"
/>
<link
  href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
  rel="stylesheet"
/>

<aside class="sidebar">
  <!-- Header con logo -->
  <div class="sidebar-header">
    <img
      src="${pageContext.request.contextPath}/img/logo.png"
      alt="Logo Voluntariado"
      class="logo-img"
    />
  </div>

  <nav class="sidebar-nav">
    <!-- ===========================================================
         DASHBOARD (siempre visible)
        ============================================================ -->
    <a
      href="${pageContext.request.contextPath}/dashboard"
      class="nav-item dashboard-link ${page eq 'dashboard' ? 'active' : ''}"
    >
      <svg
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <rect x="3" y="3" width="7" height="7" />
        <rect x="14" y="3" width="7" height="7" />
        <rect x="3" y="14" width="7" height="7" />
        <rect x="14" y="14" width="7" height="7" />
      </svg>
      <span>Dashboard</span>
    </a>

    <!-- ===========================================================
         GESTIÓN DE PERSONAS
        ============================================================ -->
    <% if (grupoPersonas) { %>
    <div class="group">
      <button class="accordion">
        <span class="group-text">Gestión de Personas</span>
        <svg class="chevron" viewBox="0 0 24 24">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>

      <div class="panel">
        <% if (pUsuarios) { %>
        <a
          href="${pageContext.request.contextPath}/usuarios"
          class="nav-item ${page eq 'usuarios' ? 'active' : ''}"
        >
          <span>Usuarios</span>
        </a>
        <% } %>

        <% if (pVoluntarios) { %>
        <a
          href="${pageContext.request.contextPath}/voluntarios"
          class="nav-item ${page eq 'voluntarios' ? 'active' : ''}"
        >
          <span>Voluntarios</span>
        </a>
        <% } %>

        <% if (pBeneficiarios) { %>
        <a
          href="${pageContext.request.contextPath}/beneficiarios"
          class="nav-item ${page eq 'beneficiarios' ? 'active' : ''}"
        >
          <span>Beneficiarios</span>
        </a>
        <% } %>
      </div>
    </div>
    <% } %>

    <!-- ===========================================================
         GESTIÓN DE ACTIVIDADES
        ============================================================ -->
    <% if (grupoActividades) { %>
    <div class="group">
      <button class="accordion">
        <span class="group-text">Gestión de Actividades</span>
        <svg class="chevron" viewBox="0 0 24 24">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>

      <div class="panel">
        <% if (pActividades) { %>
        <a
          href="${pageContext.request.contextPath}/actividades"
          class="nav-item ${page eq 'actividades' ? 'active' : ''}"
        >
          <span>Actividades</span>
        </a>
        <% } %>

        <% if (pAsistencias) { %>
        <a
          href="${pageContext.request.contextPath}/asistencias"
          class="nav-item ${page eq 'asistencias' ? 'active' : ''}"
        >
          <span>Asistencias</span>
        </a>
        <% } %>

        <% if (pCertificados) { %>
        <a
          href="${pageContext.request.contextPath}/certificados"
          class="nav-item ${page eq 'certificados' ? 'active' : ''}"
        >
          <span>Certificados</span>
        </a>
        <% } %>
      </div>
    </div>
    <% } %>

    <!-- ===========================================================
         AGENDA
        ============================================================ -->
    <% if (grupoAgenda) { %>
    <div class="group">
      <button class="accordion">
        <span class="group-text">Agenda</span>
        <svg class="chevron" viewBox="0 0 24 24">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>

      <div class="panel">
        <a
          href="${pageContext.request.contextPath}/calendario"
          class="nav-item ${page eq 'calendario' ? 'active' : ''}"
        >
          <span>Calendario</span>
        </a>
      </div>
    </div>
    <% } %>

    <!-- ===========================================================
         DONACIONES E INVENTARIO
        ============================================================ -->
    <% if (grupoDonaciones) { %>
    <div class="group">
      <button class="accordion">
        <span class="group-text">Donaciones e Inventario</span>
        <svg class="chevron" viewBox="0 0 24 24">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>

      <div class="panel">
        <% if (pDonaciones) { %>
        <a
          href="${pageContext.request.contextPath}/donaciones"
          class="nav-item ${page eq 'donaciones' ? 'active' : ''}"
        >
          <span>Donaciones</span>
        </a>
        <% } %>

        <% if (pInventario) { %>
        <a
          href="${pageContext.request.contextPath}/inventario"
          class="nav-item ${page eq 'inventario' ? 'active' : ''}"
        >
          <span>Inventario</span>
        </a>
        <% } %>
      </div>
    </div>
    <% } %>

    <!-- ===========================================================
         GESTIÓN FINANCIERA
        ============================================================ -->
    <% if (grupoFinanciero) { %>
    <div class="group">
      <button class="accordion">
        <span class="group-text">Gestión Financiera</span>
        <svg class="chevron" viewBox="0 0 24 24">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>

      <div class="panel">
        <a
          href="${pageContext.request.contextPath}/tesoreria"
          class="nav-item ${page eq 'tesoreria' ? 'active' : ''}"
        >
          <span>Tesorería</span>
        </a>
      </div>
    </div>
    <% } %>

    <!-- ===========================================================
         INFORMES
        ============================================================ -->
    <% if (grupoInformes) { %>
    <div class="group">
      <button class="accordion">
        <span class="group-text">Informes</span>
        <svg class="chevron" viewBox="0 0 24 24">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>

      <div class="panel">
        <a
          href="${pageContext.request.contextPath}/reportes"
          class="nav-item ${page eq 'reportes' ? 'active' : ''}"
        >
          <span>Reportes</span>
        </a>
      </div>
    </div>
    <% } %>
  </nav>

  <!-- ========================= FOOTER ========================= -->
  <div class="sidebar-footer">
    <a href="#" class="nav-item logout-item" onclick="logout(event)">
      <span>Cerrar Sesión</span>
    </a>
  </div>
</aside>

<script src="${pageContext.request.contextPath}/js/sidebar.js"></script>
<script src="${pageContext.request.contextPath}/js/session-manager.js"></script>
<script>
  function logout(event) {
    event.preventDefault();
    window.location.href = "${pageContext.request.contextPath}/logout";
  }
</script>
