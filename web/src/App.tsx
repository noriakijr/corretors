import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';

import { Layout } from './components/Layout.tsx';
import { ContratoDetalhePage } from './pages/ContratoDetalhePage.tsx';
import { ContratoNovoPage } from './pages/ContratoNovoPage.tsx';
import { PlanosPage } from './pages/PlanosPage.tsx';
import { SeguradorasPage } from './pages/SeguradorasPage.tsx';
import { TitularFichaPage } from './pages/TitularFichaPage.tsx';
import { TitularNovoPage } from './pages/TitularNovoPage.tsx';
import { TitularesPage } from './pages/TitularesPage.tsx';
import { VendedoresPage } from './pages/VendedoresPage.tsx';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/seguradoras" replace />} />
          <Route path="planos" element={<Navigate to="/seguradoras" replace />} />
          <Route path="seguradoras" element={<SeguradorasPage />} />
          <Route path="seguradoras/:id" element={<PlanosPage />} />
          <Route path="titulares" element={<TitularesPage />} />
          <Route path="titulares/novo" element={<TitularNovoPage />} />
          <Route path="titulares/:id" element={<TitularFichaPage />} />
          <Route path="titulares/:id/contratos/novo" element={<ContratoNovoPage />} />
          <Route path="contratos/:id" element={<ContratoDetalhePage />} />
          <Route path="vendedores" element={<VendedoresPage />} />
          <Route path="*" element={<p className="page">Página não encontrada.</p>} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
