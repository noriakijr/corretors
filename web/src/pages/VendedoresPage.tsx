import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';

import { createVendedor, errorMessage, listVendedores } from '../api.ts';
import type { Vendedor } from '../api.ts';
import { filled } from '../format.ts';
import { ErrorBanner, Field } from '../components/ui.tsx';

export function VendedoresPage() {
  const [vendedores, setVendedores] = useState<Vendedor[]>([]);
  const [nome, setNome] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listVendedores()
      .then((data) => {
        if (!cancelled) {
          setVendedores(data);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(errorMessage(cause));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    const nomeVendedor = filled(nome);
    if (!nomeVendedor) {
      setError('Informe o nome do vendedor.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await createVendedor({ nome: nomeVendedor });
      setVendedores((current) => [...current, created]);
      setNome('');
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Vendedores</h1>
          <p className="lede">Quem fechou o contrato. O nome entra no seletor da venda.</p>
        </div>
      </header>
      <ErrorBanner message={error} />
      <div className="split">
        <section className="panel">
          <h2>Lista</h2>
          {loading ? <p className="muted">Carregando…</p> : null}
          {!loading && !error && vendedores.length === 0 ? (
            <p className="empty">Nenhum vendedor cadastrado.</p>
          ) : null}
          {vendedores.length > 0 ? (
            <ul className="name-list">
              {vendedores.map((vendedor) => (
                <li key={vendedor.id}>{vendedor.nome}</li>
              ))}
            </ul>
          ) : null}
        </section>
        <section className="panel">
          <h2>Novo vendedor</h2>
          <form className="stack" noValidate onSubmit={onSubmit}>
            <Field label="Nome">
              <input value={nome} onChange={(event) => setNome(event.target.value)} autoComplete="off" />
            </Field>
            <div className="actions">
              <button className="button" type="submit" disabled={saving}>
                {saving ? 'Salvando…' : 'Cadastrar vendedor'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </div>
  );
}
