import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';

import { addPreco, createPlano, errorMessage, getSeguradora, listPlanosDaSeguradora, listPrecos } from '../api.ts';
import type { Plano, PlanoPreco } from '../api.ts';
import { filled, formatDateTime, formatMoney, parseDecimal, textOrDash } from '../format.ts';
import { ErrorBanner, Field, MoneyInput } from '../components/ui.tsx';

export function PlanosPage() {
  const params = useParams();
  const seguradoraId = params.id && /^\d+$/.test(params.id) ? Number(params.id) : null;
  const [seguradoraNome, setSeguradoraNome] = useState<string | null>(null);
  const [missing, setMissing] = useState(false);
  const [planos, setPlanos] = useState<Plano[]>([]);
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [nome, setNome] = useState('');
  const [descricao, setDescricao] = useState('');
  const [precoInicial, setPrecoInicial] = useState('');
  const [renovacaoInicial, setRenovacaoInicial] = useState('');
  const [novoPreco, setNovoPreco] = useState('');
  const [novaRenovacao, setNovaRenovacao] = useState('');
  const [historico, setHistorico] = useState<PlanoPreco[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingPreco, setSavingPreco] = useState(false);
  const [savingRenovacao, setSavingRenovacao] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (seguradoraId == null) {
      setLoading(false);
      setMissing(true);
      setSeguradoraNome(null);
      setPlanos([]);
      setSelectedId(null);
      setHistorico([]);
      setNovoPreco('');
      setNovaRenovacao('');
      return;
    }
    let cancelled = false;
    setLoading(true);
    setMissing(false);
    setSeguradoraNome(null);
    setPlanos([]);
    setSelectedId(null);
    setHistorico([]);
    setNovoPreco('');
    setNovaRenovacao('');
    getSeguradora(seguradoraId)
      .then((data) => {
        if (!cancelled) {
          setSeguradoraNome(data.nome);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setMissing(true);
          setError(errorMessage(cause));
        }
      });
    listPlanosDaSeguradora(seguradoraId)
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
  }, [seguradoraId]);

  useEffect(() => {
    if (selectedId == null) {
      setHistorico([]);
      return;
    }
    let cancelled = false;
    listPrecos(selectedId)
      .then((data) => {
        if (!cancelled) {
          setHistorico(data);
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
  }, [selectedId]);

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
    const preco = parseDecimal(precoInicial);
    if (Number.isNaN(preco)) {
      setError('Informe o preço com números, no formato 1.234,56.');
      return;
    }
    const renovacao = parseDecimal(renovacaoInicial);
    if (Number.isNaN(renovacao)) {
      setError('Informe o preço de renovação com números, no formato 1.234,56, ou deixe em branco.');
      return;
    }
    if (seguradoraId == null) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await createPlano(seguradoraId, {
        nome: nomePlano,
        descricao: filled(descricao),
        valor: preco,
        valorRenovacao: renovacao,
      });
      setPlanos((current) => [...current, created]);
      setSelectedId(created.id);
      setNome('');
      setDescricao('');
      setPrecoInicial('');
      setRenovacaoInicial('');
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  async function onChangePrice(event: FormEvent) {
    event.preventDefault();
    if (selected == null) {
      return;
    }
    const preco = parseDecimal(novoPreco);
    if (preco === undefined || Number.isNaN(preco)) {
      setError('Informe o novo preço com números, no formato 1.234,56.');
      return;
    }
    setSavingPreco(true);
    setError(null);
    try {
      const saved = await addPreco(selected.id, preco);
      setHistorico((current) => [saved, ...current.filter((item) => item.id !== saved.id)]);
      setPlanos((current) =>
        current.map((plano) => (plano.id === selected.id ? { ...plano, valor: saved.valor } : plano)),
      );
      setNovoPreco('');
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSavingPreco(false);
    }
  }

  async function onChangeRenewal(event: FormEvent) {
    event.preventDefault();
    if (selected == null) {
      return;
    }
    const preco = parseDecimal(novaRenovacao);
    if (preco === undefined || Number.isNaN(preco)) {
      setError('Informe o preço de renovação com números, no formato 1.234,56.');
      return;
    }
    setSavingRenovacao(true);
    setError(null);
    try {
      const saved = await addPreco(selected.id, preco, true);
      setHistorico((current) => [saved, ...current.filter((item) => item.id !== saved.id)]);
      setPlanos((current) =>
        current.map((plano) =>
          plano.id === selected.id ? { ...plano, valorRenovacao: saved.valor } : plano,
        ),
      );
      setNovaRenovacao('');
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSavingRenovacao(false);
    }
  }

  const historicoVisivel = [...historico].sort((left, right) => {
    if (left.dtModif === right.dtModif) {
      return right.id - left.id;
    }
    return left.dtModif < right.dtModif ? 1 : -1;
  });

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>{loading ? 'Carregando…' : (seguradoraNome ?? 'Seguradora')}</h1>
          <p className="lede">
            Planos desta seguradora. O preço vigente entra na venda. O preço de renovação é opcional e também fica no
            histórico.
          </p>
        </div>
        <Link className="button secondary" to="/seguradoras">
          Seguradoras
        </Link>
      </header>
      <ErrorBanner message={error} />
      {!loading && missing ? <p className="empty">Seguradora não encontrada.</p> : null}
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
                <p>
                  {textOrDash(plano.descricao)}
                  {' · '}
                  {plano.valor == null ? 'Sem preço' : formatMoney(plano.valor)}
                  {plano.valorRenovacao == null ? '' : ` · Renovação ${formatMoney(plano.valorRenovacao)}`}
                </p>
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
              <Field label="Preço (R$)">
                <MoneyInput value={precoInicial} onChange={setPrecoInicial} />
              </Field>
              <Field label="Preço de renovação (R$)">
                <MoneyInput value={renovacaoInicial} onChange={setRenovacaoInicial} />
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
            {loading ? (
              <p className="muted">Carregando…</p>
            ) : selected ? (
              <div className="stack">
                <dl className="kv">
                  <dt>Nome</dt>
                  <dd>{selected.nome}</dd>
                  <dt>Descrição</dt>
                  <dd>{textOrDash(selected.descricao)}</dd>
                  <dt>Preço atual</dt>
                  <dd>{formatMoney(selected.valor)}</dd>
                  <dt>Preço de renovação</dt>
                  <dd>{formatMoney(selected.valorRenovacao)}</dd>
                </dl>
                <form className="stack" noValidate onSubmit={onChangePrice}>
                  <Field label="Novo preço (R$)">
                    <MoneyInput value={novoPreco} onChange={setNovoPreco} />
                  </Field>
                  <div className="actions">
                    <button className="button" type="submit" disabled={savingPreco}>
                      {savingPreco ? 'Salvando…' : 'Registrar preço'}
                    </button>
                  </div>
                </form>
                <form className="stack" noValidate onSubmit={onChangeRenewal}>
                  <Field label="Novo preço de renovação (R$)">
                    <MoneyInput value={novaRenovacao} onChange={setNovaRenovacao} />
                  </Field>
                  <div className="actions">
                    <button className="button" type="submit" disabled={savingRenovacao}>
                      {savingRenovacao ? 'Salvando…' : 'Registrar renovação'}
                    </button>
                  </div>
                </form>
                <h3>Histórico de preços</h3>
                {historicoVisivel.length === 0 ? (
                  <p className="empty">Nenhum preço registrado.</p>
                ) : (
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Tipo</th>
                        <th className="num">Valor</th>
                      </tr>
                    </thead>
                    <tbody>
                      {historicoVisivel.map((item) => (
                        <tr key={item.id}>
                          <td>{formatDateTime(item.dtModif)}</td>
                          <td>{item.renovacao ? 'Renovação' : 'Preço'}</td>
                          <td className="num">{formatMoney(item.valor)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            ) : (
              <p className="empty">Selecione um plano na lista.</p>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
