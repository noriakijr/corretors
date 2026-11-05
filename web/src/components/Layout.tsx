import { NavLink, Outlet } from 'react-router-dom';

import { apiBase } from '../api.ts';

export function Layout() {
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark" aria-hidden="true">
            C
          </div>
          <div>
            <div className="brand-text">Corretors</div>
            <span className="brand-sub">Corretora</span>
          </div>
        </div>
        <nav className="nav" aria-label="Principal">
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            Painel
          </NavLink>
          <NavLink to="/financeiro" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            Financeiro
          </NavLink>
          <NavLink to="/seguradoras" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            Seguradoras
          </NavLink>
          <NavLink
            to="/titulares"
            className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
          >
            Titulares
          </NavLink>
          <NavLink
            to="/vendedores"
            className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
          >
            Vendedores
          </NavLink>
        </nav>
        <p className="sidebar-meta">Uso interno, sem login. API {apiBase()}</p>
      </aside>
      <div className="workspace">
        <Outlet />
      </div>
    </div>
  );
}
