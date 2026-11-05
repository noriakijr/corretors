import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { errorMessage, getDashboard } from '../api.ts';
import type { Dashboard, DashboardContrato, DashboardParcela } from '../api.ts';
import { formatDate, formatMoney, textOrDash } from '../format.ts';
import { ErrorBanner, ParcelaBadge } from '../components/ui.tsx';

export function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getDashboard()
      .then((body) => {
        if (!cancelled) {
          setData(body);
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

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>Painel</h1>
          <p className="lede">O dia da corretora: contratos ativos, o que vence, vendas e o mês.</p>
        </div>
        <Link className="button secondary" to="/financeiro">
          Financeiro
        </Link>
      </header>
      <ErrorBanner message={error} />
      {data == null && error == null ? <p className="muted">Carregando…</p> : null}
      {data != null ? (
        <>
          <section className="metrics" aria-label="Resumo do dia">
            <Metric label="Contratos ativos" value={String(data.contratosAtivos)} />
            <Metric label="Vencem em 30 dias" value={String(data.contratosVencendo)} />
            <Metric label="Vencidos e ativos" value={String(data.contratosVencidos)} warn={data.contratosVencidos > 0} />
            <Metric label="Vendas hoje" value={String(data.vendasHoje)} />
            <Metric label="Renovações hoje" value={String(data.renovacoesHoje)} />
            <Metric label="Parcelas em aberto" value={String(data.parcelasAbertas)} />
            <Metric label="Parcelas vencidas" value={String(data.parcelasVencidas)} warn={data.parcelasVencidas > 0} />
            <Metric label="Comissão do mês" value={formatMoney(data.comissaoMes)} />
            <Metric label="Receita do mês" value={formatMoney(data.receitaCorretoraMes)} />
          </section>
          <div className="dash-grid">
            <ContratoLista
              titulo="Vencem nos próximos 30 dias"
              vazio="Nenhum contrato ativo vence nos próximos 30 dias."
              contratos={data.vencendo}
              dataLabel="Fim"
              dataDe={(contrato) => contrato.dtFim}
            />
            <ContratoLista
              titulo="Vencidos e ainda ativos"
              vazio="Nenhum contrato ativo passou da data de fim."
              contratos={data.vencidos}
              dataLabel="Fim"
              dataDe={(contrato) => contrato.dtFim}
            />
            <ContratoLista
              titulo="Vendas de hoje"
              vazio="Nenhuma venda criada hoje."
              contratos={data.vendas}
              dataLabel="Aquisição"
              dataDe={(contrato) => contrato.dtAquisicao}
            />
            <ContratoLista
              titulo="Renovações de hoje"
              vazio="Nenhuma renovação criada hoje."
              contratos={data.renovacoes}
              dataLabel="Aquisição"
              dataDe={(contrato) => contrato.dtAquisicao}
            />
            <section className="panel">
              <h2>Parcelas vencidas</h2>
              {data.parcelasVencidasLista.length === 0 ? (
                <p className="empty">Nenhuma parcela em aberto com vencimento anterior a hoje.</p>
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
                      </tr>
                    </thead>
                    <tbody>
                      {data.parcelasVencidasLista.map((parcela) => (
                        <ParcelaRow key={parcela.id} parcela={parcela} />
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
            <section className="panel">
              <h2>Comissão do mês</h2>
              {data.vendedores.length === 0 ? (
                <p className="empty">Nenhum contrato adquirido neste mês.</p>
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Vendedor</th>
                        <th className="num">Comissão</th>
                        <th className="num">Receita da corretora</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.vendedores.map((vendedor) => (
                        <tr key={vendedor.vendedorId ?? 'sem-vendedor'}>
                          <td>{textOrDash(vendedor.vendedorNome)}</td>
                          <td className="num">{formatMoney(vendedor.valorComissao)}</td>
                          <td className="num">{formatMoney(vendedor.receitaCorretora)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </div>
        </>
      ) : null}
    </div>
  );
}

function Metric({ label, value, warn }: { label: string; value: string; warn?: boolean }) {
  return (
    <article className={warn ? 'metric warn' : 'metric'}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function ContratoLista({
  titulo,
  vazio,
  contratos,
  dataLabel,
  dataDe,
}: {
  titulo: string;
  vazio: string;
  contratos: DashboardContrato[];
  dataLabel: string;
  dataDe: (contrato: DashboardContrato) => string | null;
}) {
  return (
    <section className="panel">
      <h2>{titulo}</h2>
      {contratos.length === 0 ? (
        <p className="empty">{vazio}</p>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Contrato</th>
                <th>Titular</th>
                <th>Vendedor</th>
                <th>{dataLabel}</th>
              </tr>
            </thead>
            <tbody>
              {contratos.map((contrato) => (
                <tr key={contrato.id}>
                  <td>
                    <Link to={`/contratos/${contrato.id}`}>{textOrDash(contrato.codigo)}</Link>
                  </td>
                  <td>{textOrDash(contrato.titularNome)}</td>
                  <td>{textOrDash(contrato.vendedorNome)}</td>
                  <td>{formatDate(dataDe(contrato))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function ParcelaRow({ parcela }: { parcela: DashboardParcela }) {
  return (
    <tr>
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
    </tr>
  );
}
