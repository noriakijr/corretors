import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { errorMessage, listTitulares } from '../api.ts';
import type { Titular } from '../api.ts';
import { formatDocumento, formatPhone, textOrDash } from '../format.ts';
import { ErrorBanner } from '../components/ui.tsx';

function digits(value: string): string {
  return value.replace(/\D/g, '');
}

export function TitularesPage() {
  const [titulares, setTitulares] = useState<Titular[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listTitulares()
      .then((data) => {
        if (!cancelled) {
          setTitulares(data);
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
  const needleDigits = digits(query);
  const visible = titulares.filter((titular) => {
    if (!needle) {
      return true;
    }
    const byNome = titular.nome.toLowerCase().includes(needle);
    const cpf = titular.cpf ?? '';
    const byCpf = cpf.toLowerCase().includes(needle);
    const byDigits = needleDigits.length > 0 && digits(cpf).includes(needleDigits);
    return byNome || byCpf || byDigits;
  });

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Titulares</h1>
          <p className="lede">Clientes da corretora. O contrato aparece na ficha de cada titular.</p>
        </div>
        <Link className="button" to="/titulares/novo">
          Novo titular
        </Link>
      </header>
      <ErrorBanner message={error} />
      <section className="panel">
        <input
          className="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Buscar por nome ou CPF"
          aria-label="Buscar titular por nome ou CPF"
        />
        {loading ? <p className="muted">Carregando…</p> : null}
        {!loading && !error && titulares.length === 0 ? (
          <p className="empty">Nenhum titular cadastrado.</p>
        ) : null}
        {!loading && titulares.length > 0 && visible.length === 0 ? (
          <p className="empty">Nenhum titular com esse nome ou CPF.</p>
        ) : null}
        {visible.length > 0 ? (
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>CPF</th>
                <th>Celular</th>
                <th>Cidade</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((titular) => (
                <tr key={titular.id}>
                  <td>
                    <Link to={`/titulares/${titular.id}`}>{titular.nome}</Link>
                  </td>
                  <td>{formatDocumento(titular.cpf)}</td>
                  <td>{formatPhone(titular.contato?.celular)}</td>
                  <td>{textOrDash(titular.contato?.endereco?.cidade)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </section>
    </div>
  );
}
