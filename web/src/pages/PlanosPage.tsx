import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';

import { createPlano, errorMessage, listPlanos } from '../api.ts';
import type { Plano } from '../api.ts';
import { filled, textOrDash } from '../format.ts';
import { ErrorBanner, Field } from '../components/ui.tsx';

export function PlanosPage() {
  const [planos, setPlanos] = useState<Plano[]>([]);
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [nome, setNome] = useState('');
  const [descricao, setDescricao] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listPlanos()
      .then((data) => {
        if (!cancelled) {
          setPlanos(data);
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

  const needle = query.trim().toLowerCase();
  const visible = needle
    ? planos.filter((plano) => plano.nome.toLowerCase().includes(needle))
    : planos;
  const selected = planos.find((plano) => plano.id === selectedId) ?? null;

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    const nomePlano = filled(nome);
    if (!nomePlano) {
      setError('Informe o nome do plano.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await createPlano({
        nome: nomePlano,
        descricao: filled(descricao),
      });
      setPlanos((current) => [...current, created]);
      setSelectedId(created.id);
      setNome('');
      setDescricao('');
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
          <h1>Planos</h1>
          <p className="lede">Catálogo da corretora. O plano criado entra na lista e no seletor da venda.</p>
        </div>
      </header>
      <ErrorBanner message={error} />
      <div className="split">
        <section className="panel">
          <h2>Lista</h2>
          <input
            className="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Buscar por nome"
            aria-label="Buscar plano por nome"
          />
          {loading ? <p className="muted">Carregando…</p> : null}
          {!loading && !error && planos.length === 0 ? (
            <p className="empty">Nenhum plano cadastrado. Crie o primeiro ao lado.</p>
          ) : null}
          {!loading && planos.length > 0 && visible.length === 0 ? (
            <p className="empty">Nenhum plano com esse nome.</p>
          ) : null}
          <div className="list">
            {visible.map((plano) => (
              <button
                key={plano.id}
                type="button"
                className={plano.id === selectedId ? 'list-button active' : 'list-button'}
                onClick={() => setSelectedId(plano.id)}
              >
                <h3>{plano.nome}</h3>
                <p>{textOrDash(plano.descricao)}</p>
              </button>
            ))}
          </div>
        </section>
        <div className="stack">
          <section className="panel">
            <h2>Novo plano</h2>
            <form className="stack" noValidate onSubmit={onSubmit}>
              <Field label="Nome">
                <input value={nome} onChange={(event) => setNome(event.target.value)} autoComplete="off" />
              </Field>
              <Field label="Descrição">
                <textarea rows={3} value={descricao} onChange={(event) => setDescricao(event.target.value)} />
              </Field>
              <div className="actions">
                <button className="button" type="submit" disabled={saving}>
                  {saving ? 'Salvando…' : 'Criar plano'}
                </button>
              </div>
            </form>
          </section>
          <section className="panel">
            <h2>Detalhe</h2>
            {selected ? (
              <dl className="kv">
                <dt>Nome</dt>
                <dd>{selected.nome}</dd>
                <dt>Descrição</dt>
                <dd>{textOrDash(selected.descricao)}</dd>
              </dl>
            ) : (
              <p className="empty">Selecione um plano na lista. Não há edição.</p>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
