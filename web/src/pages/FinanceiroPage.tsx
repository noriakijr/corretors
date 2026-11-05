import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';

import { errorMessage, getFinanceiro, listSeguradoras, listVendedores, pagarParcela } from '../api.ts';
import type { Financeiro, FinanceiroParcela, Seguradora, Vendedor } from '../api.ts';
import { formatDate, formatMoney, textOrDash } from '../format.ts';
import { ErrorBanner, Field, ParcelaBadge } from '../components/ui.tsx';

type Filtro = {
  de: string;
  ate: string;
  seguradoraId: string;
  vendedorId: string;
};

export function FinanceiroPage() {
  const inicial = periodoMesAtual();
  const [filtro, setFiltro] = useState<Filtro>({ ...inicial, seguradoraId: '', vendedorId: '' });
  const [aplicado, setAplicado] = useState<Filtro>({ ...inicial, seguradoraId: '', vendedorId: '' });
  const [seguradoras, setSeguradoras] = useState<Seguradora[]>([]);
  const [vendedores, setVendedores] = useState<Vendedor[]>([]);
  const [data, setData] = useState<Financeiro | null>(null);
  const [loading, setLoading] = useState(true);
  const [pagando, setPagando] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([listSeguradoras(), listVendedores()])
      .then(([listaSeguradoras, listaVendedores]) => {
        if (!cancelled) {
          setSeguradoras(listaSeguradoras);
          setVendedores(listaVendedores);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(errorMessage(cause));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getFinanceiro({
      de: aplicado.de,
      ate: aplicado.ate,
      seguradoraId: idOrUndefined(aplicado.seguradoraId),
      vendedorId: idOrUndefined(aplicado.vendedorId),
    })
      .then((body) => {
        if (!cancelled) {
          setData(body);
          setError(null);
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
  }, [aplicado]);

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!filtro.de || !filtro.ate) {
      setError('Informe o início e o fim do período.');
      return;
    }
    if (filtro.de > filtro.ate) {
      setError('O início do período não pode ser depois do fim.');
      return;
    }
    setAplicado(filtro);
  }

  async function receber(parcela: FinanceiroParcela) {
    setPagando(parcela.id);
    setError(null);
    try {
      await pagarParcela(parcela.id);
      const body = await getFinanceiro({
        de: aplicado.de,
        ate: aplicado.ate,
        seguradoraId: idOrUndefined(aplicado.seguradoraId),
        vendedorId: idOrUndefined(aplicado.vendedorId),
      });
      setData(body);
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setPagando(null);
    }
  }

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Financeiro</h1>
          <p className="lede">
            Contratos do período, com total, desconto, comissão e receita. Parcelas pelo vencimento.
          </p>
        </div>
      </header>
      <ErrorBanner message={error} />
      <form className="panel filters" onSubmit={onSubmit}>
        <Field label="De">
          <input type="date" value={filtro.de} onChange={(event) => setFiltro({ ...filtro, de: event.target.value })} />
        </Field>
        <Field label="Até">
          <input
            type="date"
            value={filtro.ate}
            onChange={(event) => setFiltro({ ...filtro, ate: event.target.value })}
          />
        </Field>
        <Field label="Seguradora">
          <select
            value={filtro.seguradoraId}
            onChange={(event) => setFiltro({ ...filtro, seguradoraId: event.target.value })}
          >
            <option value="">Todas</option>
            {seguradoras.map((seguradora) => (
              <option key={seguradora.id} value={seguradora.id}>
                {seguradora.nome}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Vendedor">
          <select
            value={filtro.vendedorId}
            onChange={(event) => setFiltro({ ...filtro, vendedorId: event.target.value })}
          >
            <option value="">Todos</option>
            {vendedores.map((vendedor) => (
              <option key={vendedor.id} value={vendedor.id}>
                {vendedor.nome}
              </option>
            ))}
          </select>
        </Field>
        <button className="button" type="submit">
          Filtrar
        </button>
      </form>
      {loading && data == null ? <p className="muted">Carregando…</p> : null}
      {data != null ? (
        <div className="page-sections">
          <section className="panel">
            <h2>
              {formatDate(data.de)} a {formatDate(data.ate)}
            </h2>
            <dl className="totals three">
              <div>
                <dt>Faturado</dt>
                <dd>{formatMoney(data.valorFaturado)}</dd>
              </div>
              <div>
                <dt>Comissão</dt>
                <dd>{formatMoney(data.comissao)}</dd>
              </div>
              <div>
                <dt>Receita da corretora</dt>
                <dd>{formatMoney(data.receitaCorretora)}</dd>
              </div>
            </dl>
          </section>
          <section className="panel">
            <h2>Contratos</h2>
            {data.contratos.length === 0 ? (
              <p className="empty">Nenhum contrato adquirido nesse período.</p>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Contrato</th>
                      <th>Titular</th>
                      <th>Seguradora</th>
                      <th>Vendedor</th>
                      <th>Aquisição</th>
                      <th className="num">Total</th>
                      <th className="num">Desconto</th>
                      <th className="num">Comissão</th>
                      <th className="num">Receita</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.contratos.map((contrato) => (
                      <tr key={contrato.id}>
                        <td>
                          <Link to={`/contratos/${contrato.id}`}>{textOrDash(contrato.codigo)}</Link>
                        </td>
                        <td>{textOrDash(contrato.titularNome)}</td>
                        <td>{textOrDash(contrato.seguradoraNome)}</td>
                        <td>{textOrDash(contrato.vendedorNome)}</td>
                        <td>{formatDate(contrato.dtAquisicao)}</td>
                        <td className="num">{formatMoney(contrato.total)}</td>
                        <td className="num">{formatMoney(contrato.desconto)}</td>
                        <td className="num">{formatMoney(contrato.valorComissao)}</td>
                        <td className="num">{formatMoney(contrato.receitaCorretora)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
          <section className="panel">
            <h2>Parcelas</h2>
            {data.parcelas.length === 0 ? (
              <p className="empty">Nenhuma parcela com vencimento nesse período.</p>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Contrato</th>
                      <th>Titular</th>
                      <th>Vencimento</th>
                      <th className="num">Valor</th>
                      <th>Situação</th>
                      <th>Pagamento</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.parcelas.map((parcela) => (
                      <tr key={parcela.id}>
                        <td>
                          {parcela.contratoId != null ? (
                            <Link to={`/contratos/${parcela.contratoId}`}>{textOrDash(parcela.codigo)}</Link>
                          ) : (
                            textOrDash(parcela.codigo)
                          )}
                        </td>
                        <td>{textOrDash(parcela.titularNome)}</td>
                        <td>{formatDate(parcela.dtParcela)}</td>
                        <td className="num">{formatMoney(parcela.valor)}</td>
                        <td>
                          <ParcelaBadge status={parcela.status} />
                        </td>
                        <td>
                          {parcela.status === 'PAGA' ? (
                            formatDate(parcela.dtPagamento)
                          ) : (
                            <button
                              type="button"
                              className="button small"
                              disabled={pagando === parcela.id}
                              onClick={() => void receber(parcela)}
                            >
                              {pagando === parcela.id ? 'Recebendo…' : 'Receber'}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      ) : null}
    </div>
  );
}

function periodoMesAtual(): { de: string; ate: string } {
  const now = new Date();
  const inicio = new Date(now.getFullYear(), now.getMonth(), 1);
  const fim = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return { de: isoDate(inicio), ate: isoDate(fim) };
}

function isoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function idOrUndefined(value: string): number | undefined {
  if (!/^\d+$/.test(value)) {
    return undefined;
  }
  return Number(value);
}
