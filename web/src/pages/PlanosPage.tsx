import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';

import {
  addPreco,
  createPlano,
  deleteComissaoVendedor,
  errorMessage,
  getSeguradora,
  listPlanosDaSeguradora,
  listPrecos,
  listVendedores,
  saveComissaoPadrao,
  saveComissaoVendedor,
  updatePlanoLimite,
} from '../api.ts';
import type { Plano, PlanoPreco, TipoComissao, Vendedor } from '../api.ts';
import { filled, formatDateTime, formatMoney, moneyInput, parseDecimal, rotuloRegra, textOrDash } from '../format.ts';
import { DigitsInput, ErrorBanner, Field, MoneyInput } from '../components/ui.tsx';

function valorDaRegra(
  tipo: TipoComissao | null,
  percentual: number | null | undefined,
  valorFixo: number | null | undefined,
): string {
  if (tipo === 'PERCENTUAL' && percentual != null) {
    return String(percentual).replace('.', ',');
  }
  if (tipo === 'VALOR_FIXO' && valorFixo != null) {
    return moneyInput(valorFixo);
  }
  return '';
}

function lerComissao(
  modo: '' | TipoComissao,
  valor: string,
): { input: { tipoComissao?: TipoComissao; percentual?: number; valorFixo?: number } } | { error: string } {
  if (!modo) {
    return { input: {} };
  }
  const numero = parseDecimal(valor);
  if (numero === undefined || Number.isNaN(numero)) {
    return { error: modo === 'PERCENTUAL' ? 'Informe o percentual da comissão.' : 'Informe o valor fixo da comissão.' };
  }
  if (modo === 'PERCENTUAL') {
    if (numero > 100) {
      return { error: 'O percentual da comissão não pode passar de 100.' };
    }
    return { input: { tipoComissao: 'PERCENTUAL', percentual: numero } };
  }
  return { input: { tipoComissao: 'VALOR_FIXO', valorFixo: numero } };
}

function parseLimite(value: string): number | null | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  if (!/^\d+$/.test(trimmed)) {
    return undefined;
  }
  return Number(trimmed);
}

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
  const [limiteInicial, setLimiteInicial] = useState('');
  const [limiteEdicao, setLimiteEdicao] = useState('');
  const [novoPreco, setNovoPreco] = useState('');
  const [novaRenovacao, setNovaRenovacao] = useState('');
  const [historico, setHistorico] = useState<PlanoPreco[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savingPreco, setSavingPreco] = useState(false);
  const [savingRenovacao, setSavingRenovacao] = useState(false);
  const [savingLimite, setSavingLimite] = useState(false);
  const [comissaoModoInicial, setComissaoModoInicial] = useState<'' | TipoComissao>('');
  const [comissaoValorInicial, setComissaoValorInicial] = useState('');
  const [comissaoModo, setComissaoModo] = useState<'' | TipoComissao>('');
  const [comissaoValor, setComissaoValor] = useState('');
  const [savingComissao, setSavingComissao] = useState(false);
  const [vendedores, setVendedores] = useState<Vendedor[]>([]);
  const [excecaoVendedorId, setExcecaoVendedorId] = useState('');
  const [excecaoModo, setExcecaoModo] = useState<TipoComissao>('PERCENTUAL');
  const [excecaoValor, setExcecaoValor] = useState('');
  const [savingExcecao, setSavingExcecao] = useState(false);
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

  useEffect(() => {
    const current = planos.find((plano) => plano.id === selectedId);
    setLimiteEdicao(current?.limiteDependentes == null ? '' : String(current.limiteDependentes));
    setComissaoModo(current?.tipoComissao ?? '');
    setComissaoValor(valorDaRegra(current?.tipoComissao ?? null, current?.percentual, current?.valorFixo));
    setExcecaoVendedorId('');
    setExcecaoModo('PERCENTUAL');
    setExcecaoValor('');
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
    const limite = parseLimite(limiteInicial);
    if (limite === undefined) {
      setError('Informe o limite de dependentes com um número inteiro a partir de zero, ou deixe em branco.');
      return;
    }
    const comissao = lerComissao(comissaoModoInicial, comissaoValorInicial);
    if ('error' in comissao) {
      setError(comissao.error);
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
        limiteDependentes: limite,
        ...comissao.input,
      });
      setPlanos((current) => [...current, created]);
      setSelectedId(created.id);
      setNome('');
      setDescricao('');
      setPrecoInicial('');
      setRenovacaoInicial('');
      setLimiteInicial('');
      setComissaoModoInicial('');
      setComissaoValorInicial('');
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

  async function onChangeLimite(event: FormEvent) {
    event.preventDefault();
    if (selected == null) {
      return;
    }
    const limite = parseLimite(limiteEdicao);
    if (limite === undefined) {
      setError('Informe o limite de dependentes com um número inteiro a partir de zero, ou deixe em branco.');
      return;
    }
    setSavingLimite(true);
    setError(null);
    try {
      const saved = await updatePlanoLimite(selected.id, limite);
      setPlanos((current) => current.map((plano) => (plano.id === selected.id ? saved : plano)));
      setLimiteEdicao(saved.limiteDependentes == null ? '' : String(saved.limiteDependentes));
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSavingLimite(false);
    }
  }

  async function onSaveComissao(event: FormEvent) {
    event.preventDefault();
    if (selected == null) {
      return;
    }
    const comissao = lerComissao(comissaoModo, comissaoValor);
    if ('error' in comissao) {
      setError(comissao.error);
      return;
    }
    setSavingComissao(true);
    setError(null);
    try {
      const saved = await saveComissaoPadrao(selected.id, comissao.input);
      setPlanos((current) => current.map((plano) => (plano.id === selected.id ? saved : plano)));
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSavingComissao(false);
    }
  }

  async function onSaveExcecao(event: FormEvent) {
    event.preventDefault();
    if (selected == null) {
      return;
    }
    if (!excecaoVendedorId) {
      setError('Selecione o vendedor da exceção.');
      return;
    }
    const comissao = lerComissao(excecaoModo, excecaoValor);
    if ('error' in comissao || !comissao.input.tipoComissao) {
      setError('error' in comissao ? comissao.error : 'Informe a regra do vendedor.');
      return;
    }
    setSavingExcecao(true);
    setError(null);
    try {
      const saved = await saveComissaoVendedor(selected.id, Number(excecaoVendedorId), comissao.input);
      setPlanos((current) => current.map((plano) => (plano.id === selected.id ? saved : plano)));
      setExcecaoVendedorId('');
      setExcecaoModo('PERCENTUAL');
      setExcecaoValor('');
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSavingExcecao(false);
    }
  }

  async function onRemoveExcecao(vendedorId: number) {
    if (selected == null) {
      return;
    }
    setSavingExcecao(true);
    setError(null);
    try {
      const saved = await deleteComissaoVendedor(selected.id, vendedorId);
      setPlanos((current) => current.map((plano) => (plano.id === selected.id ? saved : plano)));
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSavingExcecao(false);
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
            Planos desta seguradora. O preço vigente entra na venda. A comissão padrão vale para todo vendedor, com
            exceção quando o vendedor tem uma regra própria.
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
                  {` · Comissão ${rotuloRegra(plano)}`}
                  {' · '}
                  {plano.limiteDependentes == null
                    ? 'Sem limite de dependentes'
                    : `Limite de dependentes: ${plano.limiteDependentes}`}
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
              <Field label="Limite de dependentes">
                <DigitsInput value={limiteInicial} onChange={setLimiteInicial} />
              </Field>
              <p className="note">Em branco não limita a quantidade de dependentes do plano.</p>
              <Field label="Comissão padrão">
                <select
                  value={comissaoModoInicial}
                  onChange={(event) => setComissaoModoInicial(event.target.value as '' | TipoComissao)}
                >
                  <option value="">Sem comissão</option>
                  <option value="PERCENTUAL">Percentual</option>
                  <option value="VALOR_FIXO">Valor fixo</option>
                </select>
              </Field>
              {comissaoModoInicial === 'PERCENTUAL' ? (
                <Field label="Percentual (%)">
                  <MoneyInput value={comissaoValorInicial} onChange={setComissaoValorInicial} />
                </Field>
              ) : null}
              {comissaoModoInicial === 'VALOR_FIXO' ? (
                <Field label="Valor fixo (R$)">
                  <MoneyInput value={comissaoValorInicial} onChange={setComissaoValorInicial} />
                </Field>
              ) : null}
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
                  <dt>Limite de dependentes</dt>
                  <dd>{selected.limiteDependentes == null ? 'Sem limite' : selected.limiteDependentes}</dd>
                  <dt>Comissão padrão</dt>
                  <dd>{rotuloRegra(selected)}</dd>
                </dl>
                <form className="stack" noValidate onSubmit={onSaveComissao}>
                  <Field label="Comissão padrão">
                    <select
                      value={comissaoModo}
                      onChange={(event) => setComissaoModo(event.target.value as '' | TipoComissao)}
                    >
                      <option value="">Sem comissão</option>
                      <option value="PERCENTUAL">Percentual da venda ou da renovação</option>
                      <option value="VALOR_FIXO">Valor fixo</option>
                    </select>
                  </Field>
                  {comissaoModo === 'PERCENTUAL' ? (
                    <Field label="Percentual (%)">
                      <MoneyInput value={comissaoValor} onChange={setComissaoValor} />
                    </Field>
                  ) : null}
                  {comissaoModo === 'VALOR_FIXO' ? (
                    <Field label="Valor fixo (R$)">
                      <MoneyInput value={comissaoValor} onChange={setComissaoValor} />
                    </Field>
                  ) : null}
                  <p className="note">
                    Sem regra, a comissão fica zero. Percentual incide sobre o total da venda, já com desconto, e sobre
                    o total do contrato novo na renovação. Valor fixo não muda com o preço.
                  </p>
                  <div className="actions">
                    <button className="button" type="submit" disabled={savingComissao}>
                      {savingComissao ? 'Salvando…' : 'Salvar comissão padrão'}
                    </button>
                  </div>
                </form>
                <h3>Exceções por vendedor</h3>
                {selected.excecoes.length === 0 ? (
                  <p className="empty">Nenhuma exceção. Os vendedores usam a comissão padrão.</p>
                ) : (
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Vendedor</th>
                        <th>Regra</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {selected.excecoes.map((excecao) => (
                        <tr key={excecao.vendedorId}>
                          <td>{excecao.vendedorNome ?? `Vendedor ${excecao.vendedorId}`}</td>
                          <td>{rotuloRegra(excecao)}</td>
                          <td>
                            <button
                              className="button secondary"
                              type="button"
                              disabled={savingExcecao}
                              onClick={() => onRemoveExcecao(excecao.vendedorId)}
                            >
                              Remover
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                <form className="stack" noValidate onSubmit={onSaveExcecao}>
                  <Field label="Vendedor">
                    <select value={excecaoVendedorId} onChange={(event) => setExcecaoVendedorId(event.target.value)}>
                      <option value="">Selecione o vendedor</option>
                      {vendedores.map((vendedor) => (
                        <option key={vendedor.id} value={vendedor.id}>
                          {vendedor.nome}
                        </option>
                      ))}
                    </select>
                  </Field>
                  {vendedores.length === 0 ? (
                    <p className="note">
                      Nenhum vendedor cadastrado. <Link to="/vendedores">Cadastrar um vendedor</Link> para criar uma
                      exceção.
                    </p>
                  ) : null}
                  <Field label="Regra do vendedor">
                    <select
                      value={excecaoModo}
                      onChange={(event) => setExcecaoModo(event.target.value as TipoComissao)}
                    >
                      <option value="PERCENTUAL">Percentual</option>
                      <option value="VALOR_FIXO">Valor fixo</option>
                    </select>
                  </Field>
                  {excecaoModo === 'PERCENTUAL' ? (
                    <Field label="Percentual (%)">
                      <MoneyInput value={excecaoValor} onChange={setExcecaoValor} />
                    </Field>
                  ) : (
                    <Field label="Valor fixo (R$)">
                      <MoneyInput value={excecaoValor} onChange={setExcecaoValor} />
                    </Field>
                  )}
                  <div className="actions">
                    <button className="button" type="submit" disabled={savingExcecao}>
                      {savingExcecao ? 'Salvando…' : 'Salvar exceção'}
                    </button>
                  </div>
                </form>
                <form className="stack" noValidate onSubmit={onChangeLimite}>
                  <Field label="Limite de dependentes">
                    <DigitsInput value={limiteEdicao} onChange={setLimiteEdicao} />
                  </Field>
                  <p className="note">Em branco remove o limite. O valor vale para as vendas deste plano.</p>
                  <div className="actions">
                    <button className="button" type="submit" disabled={savingLimite}>
                      {savingLimite ? 'Salvando…' : 'Salvar limite'}
                    </button>
                  </div>
                </form>
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
