import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';

import { createSeguradora, errorMessage, listSeguradoras } from '../api.ts';
import type { Seguradora } from '../api.ts';
import { filled } from '../format.ts';
import { ErrorBanner, Field } from '../components/ui.tsx';

export function SeguradorasPage() {
  const [seguradoras, setSeguradoras] = useState<Seguradora[]>([]);
  const [nome, setNome] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listSeguradoras()
      .then((data) => {
        if (!cancelled) {
          setSeguradoras(data);
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
    const nomeSeguradora = filled(nome);
    if (!nomeSeguradora) {
      setError('Informe o nome da seguradora.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await createSeguradora(nomeSeguradora);
      setSeguradoras((current) => [...current, created]);
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
          <h1>Seguradoras</h1>
          <p className="lede">Cada seguradora oferece os próprios planos. O cadastro do plano fica na página dela.</p>
        </div>
      </header>
      <ErrorBanner message={error} />
      <div className="split">
        <section className="panel">
          <h2>Lista</h2>
          {loading ? <p className="muted">Carregando…</p> : null}
          {!loading && !error && seguradoras.length === 0 ? (
            <p className="empty">Nenhuma seguradora cadastrada.</p>
          ) : null}
          <div className="list">
            {seguradoras.map((seguradora) => (
              <Link key={seguradora.id} className="list-button" to={`/seguradoras/${seguradora.id}`}>
                <h3>{seguradora.nome}</h3>
                <p>Planos e preços</p>
              </Link>
            ))}
          </div>
        </section>
        <section className="panel">
          <h2>Nova seguradora</h2>
          <form className="stack" noValidate onSubmit={onSubmit}>
            <Field label="Nome">
              <input value={nome} onChange={(event) => setNome(event.target.value)} autoComplete="off" />
            </Field>
            <div className="actions">
              <button className="button" type="submit" disabled={saving}>
                {saving ? 'Salvando…' : 'Cadastrar seguradora'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </div>
  );
}
