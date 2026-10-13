import { Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { AppShell } from './components/AppShell';
import { MODULES } from './modules';
import { HomePage } from './pages/HomePage';
import { ModulePage } from './pages/ModulePage';
import { NotFoundPage } from './pages/NotFoundPage';

export function App() {
  return (
    <RequireAuth>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<HomePage />} />
          {MODULES.map((m) => (
            <Route key={m.id} path={m.path} element={<ModulePage module={m} />} />
          ))}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </RequireAuth>
  );
}
