import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';

import {
  createDependente,
  deleteDependente,
  errorMessage,
  getContrato,
  listDependentes,
  listPlanos,
  listTitulares,
  listVendedores,
  renewContrato,
  updateDependente,
} from '../api.ts';
import type { Contrato, Dependente, PagamentoType, Plano, Titular, Vendedor } from '../api.ts';
import {
  calcularComissao,
  filled,
  formatDate,
  formatDateTime,
  formatMoney,
  labelOrId,
  moneyInput,
  parseDecimal,
  parseParcelas,
  periodoVigente,
  planoRotulo,
  pontoAtencao,
  regraDoVendedor,
  textOrDash,
  toApiDate,
} from '../format.ts';
import { DependentesEditor } from '../components/DependentesEditor.tsx';
import type { DependenteDraft } from '../components/DependentesEditor.tsx';
import { DigitsInput, ErrorBanner, Field, MoneyInput, OkBanner, PontoAtencao, StatusBadge } from '../components/ui.tsx';

const TIPOS: { value: PagamentoType; label: string }[] = [
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'PIX', label: 'Pix' },
];

function parseId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null;
  }
  return Number(value);
}

export function ContratoDetalhePage() {
  const params = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const contratoId = parseId(params.id);
  const notice =
    location.state && typeof location.state === 'object' && 'notice' in location.state
      ? String((location.state as { notice?: unknown }).notice ?? '')
      : '';

  const [contrato, setContrato] = useState<Contrato | null>(null);
  const [dependentes, setDependentes] = useState<Dependente[]>([]);
  const [planos, setPlanos] = useState<Plano[]>([]);
  const [vendedores, setVendedores] = useState<Vendedor[]>([]);
  const [titulares, setTitulares] = useState<Titular[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const [dtInicio, setDtInicio] = useState('');
  const [dtFim, setDtFim] = useState('');
  const [valor, setValor] = useState('');
  const [vlrDesconto, setVlrDesconto] = useState('');
  const [tipoPag, setTipoPag] = useState('');
  const [modo, setModo] = useState<'vista' | 'parcelado'>('vista');
  const [parcelas, setParcelas] = useState('2');
  const [codigo, setCodigo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [confirmed, setConfirmed] = useState(false);

  useEffect(() => {
    if (contratoId == null) {
      setLoading(false);
      setError('Contrato não encontrado.');
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    setContrato(null);
    setDependentes([]);
    getContrato(contratoId)
      .then((data) => {
        if (!cancelled) {
          setContrato(data);
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
    listDependentes(contratoId)
      .then((data) => {
        if (!cancelled) {
          setDependentes(data);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(errorMessage(cause));
        }
      });
    listPlanos()
      .then((data) => {
        if (!cancelled) {
          setPlanos(data);
        }
      })
      .catch(() => undefined);
    listVendedores()
      .then((data) => {
        if (!cancelled) {
          setVendedores(data);
        }
      })
      .catch(() => undefined);
    listTitulares()
      .then((data) => {
        if (!cancelled) {
          setTitulares(data);
        }
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [contratoId]);

  useEffect(() => {
    setDtInicio('');
    setDtFim('');
    setValor('');
    setVlrDesconto('');
    setTipoPag('');
    setModo('vista');
    setParcelas('2');
    setCodigo('');
    setDescricao('');
    setConfirmed(false);
    setFormError(null);
  }, [contratoId]);

  useEffect(() => {
    if (!contrato || contrato.id !== contratoId) {
      return;
    }
    const planoRenovacao = planos.find((item) => item.id === contrato.planoId)?.valorRenovacao;
    if (planoRenovacao == null) {
      return;
    }
    setValor((current) => (current === '' ? moneyInput(planoRenovacao) : current));
  }, [contrato, contratoId, planos]);

  const ativo = contrato?.status === 'ATIVO';
  const vigente = Boolean(contrato && ativo && periodoVigente(contrato.dtInicio, contrato.dtFim));
  const plano = planos.find((item) => item.id === contrato?.planoId);
  const limite = plano ? plano.limiteDependentes : undefined;
  const acimaDoLimite =
    limite === undefined ? Boolean(contrato?.pontoAtencao) : pontoAtencao(dependentes.length, limite);
  const vendedor = vendedores.find((item) => item.id === contrato?.vendedorId);
  const titular = titulares.find((item) => item.id === contrato?.titularId);
  const valorInformado = parseDecimal(valor);
  const descontoInformado = parseDecimal(vlrDesconto);
  const baseRenovacao =
    contrato == null
      ? undefined
      : valorInformado === undefined
        ? (contrato.valor ?? undefined)
        : Number.isNaN(valorInformado)
          ? undefined
          : valorInformado;
  const descontoAplicado =
    descontoInformado === undefined || Number.isNaN(descontoInformado) ? 0 : descontoInformado;
  const totalRenovacao =
    baseRenovacao === undefined || Number.isNaN(descontoInformado ?? 0)
      ? undefined
      : baseRenovacao - descontoAplicado;
  const quantidade = parseParcelas(parcelas);
  const cadaParcela =
    totalRenovacao !== undefined && quantidade !== undefined ? totalRenovacao / quantidade : undefined;
  const descontoMaior =
    descontoInformado !== undefined &&
    !Number.isNaN(descontoInformado) &&
    baseRenovacao !== undefined &&
    descontoInformado > baseRenovacao;
  const comissaoRenovacao =
    plano && totalRenovacao !== undefined && !descontoMaior
      ? calcularComissao(regraDoVendedor(plano, contrato?.vendedorId), totalRenovacao)
      : undefined;

  async function onRenew(event: FormEvent) {
    event.preventDefault();
    if (!contrato || !ativo) {
      return;
    }
    if (!confirmed) {
      setFormError('Confirme que o contrato atual ficará RENOVADO e que um contrato novo será criado.');
      return;
    }
    const valorNumero = parseDecimal(valor);
    if (Number.isNaN(valorNumero)) {
      setFormError('Informe um valor numérico ou deixe em branco.');
      return;
    }
    if (valorNumero !== undefined && valorNumero < 0) {
      setFormError('O valor da renovação não pode ser negativo.');
      return;
    }
    const desconto = parseDecimal(vlrDesconto);
    if (Number.isNaN(desconto)) {
      setFormError('Informe um desconto numérico ou deixe em branco.');
      return;
    }
    if (desconto !== undefined && baseRenovacao !== undefined && desconto > baseRenovacao) {
      setFormError('O desconto não pode ser maior que o valor.');
      return;
    }
    const quantidadeParcelas = modo === 'parcelado' ? parseParcelas(parcelas) : undefined;
    if (modo === 'parcelado' && (quantidadeParcelas === undefined || quantidadeParcelas < 2)) {
      setFormError('Informe a quantidade de parcelas, com 2 ou mais.');
      return;
    }
    if (comissaoRenovacao?.erro) {
      setFormError(comissaoRenovacao.erro);
      return;
    }
    setSaving(true);
    setFormError(null);
    try {
      const novo = await renewContrato(contrato.id, {
        dtInicio: toApiDate(dtInicio),
        dtFim: toApiDate(dtFim),
        valor: valorNumero,
        codigo: filled(codigo),
        descricao: filled(descricao),
        tipoPag: tipoPag ? (tipoPag as PagamentoType) : undefined,
        vlrDesconto: desconto,
        quantidadeParcelas,
      });
      navigate(`/contratos/${novo.id}`, {
        state: {
          notice: 'Contrato renovado. Os dependentes do contrato anterior foram copiados.',
        },
      });
    } catch (cause: unknown) {
      setFormError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  async function incluirDependente(item: DependenteDraft) {
    if (!contrato) {
      return;
    }
    const saved = await createDependente(contrato.id, item);
    setDependentes((current) => [...current, saved]);
  }

  async function alterarDependente(key: string, item: DependenteDraft) {
    if (!contrato) {
      return;
    }
    const dependenteId = Number(key);
    const atual = dependentes.find((dependente) => dependente.id === dependenteId);
    if (!atual) {
      return;
    }
    const saved = await updateDependente(contrato.id, atual.id, item);
    setDependentes((current) => current.map((dependente) => (dependente.id === atual.id ? saved : dependente)));
  }

  async function removerDependente(key: string) {
    if (!contrato) {
      return;
    }
    const dependenteId = Number(key);
    const atual = dependentes.find((dependente) => dependente.id === dependenteId);
    if (!atual) {
      return;
    }
    await deleteDependente(contrato.id, atual.id);
    setDependentes((current) => current.filter((dependente) => dependente.id !== atual.id));
  }

  return (
    <div className="page narrow">
      <header className="page-head">
        <div>
          <h1>Contrato {contrato ? textOrDash(contrato.codigo) : ''}</h1>
          {contrato && acimaDoLimite ? (
            <p className="badges">
              <PontoAtencao />
            </p>
          ) : null}
          <p className="lede">Detalhe da venda. Renovar cria outra linha; não estende este registro.</p>
        </div>
        {contrato?.titularId != null ? (
          <Link className="button secondary" to={`/titulares/${contrato.titularId}`}>
            Voltar à ficha
          </Link>
        ) : (
          <Link className="button secondary" to="/titulares">
            Titulares
          </Link>
        )}
      </header>
      <OkBanner message={notice || null} />
      <ErrorBanner message={error} />
      {loading ? <p className="muted">Carregando…</p> : null}
      {contrato ? (
        <div className="stack">
          <section className="panel">
            <h2>Dados</h2>
            <dl className="kv">
              <dt>Código</dt>
              <dd>{textOrDash(contrato.codigo)}</dd>
              <dt>Descrição</dt>
              <dd>{textOrDash(contrato.descricao)}</dd>
              <dt>Status</dt>
              <dd>
                <span className="badges">
                  <StatusBadge status={contrato.status} />
                  {acimaDoLimite ? <PontoAtencao /> : null}
                </span>
              </dd>
              <dt>Titular</dt>
              <dd>
                {contrato.titularId != null ? (
                  <Link to={`/titulares/${contrato.titularId}`}>
                    {labelOrId(titular?.nome, contrato.titularId, 'Titular')}
                  </Link>
                ) : (
                  '—'
                )}
              </dd>
              <dt>Plano</dt>
              <dd>{planoRotulo(plano?.nome, plano?.seguradoraNome, contrato.planoId)}</dd>
              <dt>Valor</dt>
              <dd>{formatMoney(contrato.valor)}</dd>
              <dt>Comissão do vendedor</dt>
              <dd>{formatMoney(contrato.valorComissao)}</dd>
              <dt>Receita da corretora</dt>
              <dd>{formatMoney(contrato.receitaCorretora)}</dd>
              <dt>Aquisição</dt>
              <dd>{formatDateTime(contrato.dtAquisicao)}</dd>
              <dt>Início</dt>
              <dd>{formatDate(contrato.dtInicio)}</dd>
              <dt>Fim</dt>
              <dd>{formatDate(contrato.dtFim)}</dd>
              <dt>Vendedor</dt>
              <dd>{labelOrId(vendedor?.nome, contrato.vendedorId, 'Vendedor')}</dd>
              <dt>Pagamento</dt>
              <dd>{contrato.pagamentoId ?? '—'}</dd>
              <dt>Histórico do plano</dt>
              <dd>{contrato.planoHistoricoId ?? '—'}</dd>
              <dt>Contrato anterior</dt>
              <dd>
                {contrato.contratoAnteriorId != null ? (
                  <Link to={`/contratos/${contrato.contratoAnteriorId}`}>
                    Abrir contrato {contrato.contratoAnteriorId}
                  </Link>
                ) : (
                  '—'
                )}
              </dd>
            </dl>
          </section>
          <section className="panel">
            <h2>Dependentes</h2>
            <DependentesEditor
              itens={dependentes.map((dependente) => ({
                key: String(dependente.id),
                nome: dependente.nome,
                parentesco: dependente.parentesco,
                dtNasc: dependente.dtNasc,
              }))}
              limite={limite}
              editavel={vigente}
              nota={
                acimaDoLimite
                  ? 'Ponto de atenção: a quantidade de dependentes passa do limite do plano. O aviso permanece até a quantidade voltar ao limite.'
                  : vigente
                    ? 'Inclusão, alteração e remoção enquanto o contrato está vigente.'
                    : 'Contrato fora da vigência. A lista fica só para consulta.'
              }
              onCreate={incluirDependente}
              onUpdate={alterarDependente}
              onRemove={removerDependente}
            />
          </section>
          <section className="panel">
            <h2>Renovar</h2>
            <p className="note">
              Campos vazios repetem o preço, o plano e o vendedor. Sem início, a vigência nova começa agora; o fim
              vazio dura um ano a partir desse início. O total já inclui o desconto. O anterior fica RENOVADO e o novo
              nasce ATIVO, ligado por este registro.
            </p>
            {!ativo ? <p className="note">Só um contrato ATIVO pode ser renovado.</p> : null}
            <div className="stack">
              <h3>Dependentes que serão copiados</h3>
              {dependentes.length === 0 ? <p className="empty">Nenhum dependente para copiar.</p> : null}
              {dependentes.length > 0 && acimaDoLimite ? (
                <p className="note">
                  A renovação copia todos os dependentes. O contrato novo nasce com ponto de atenção, porque a
                  quantidade passa do limite do plano.
                </p>
              ) : null}
              {dependentes.length > 0 && !acimaDoLimite ? (
                <p className="note">A renovação copia todos os dependentes.</p>
              ) : null}
              {dependentes.length > 0 ? (
                <ul className="dep-list">
                  {dependentes.map((item) => (
                    <li key={item.id} className="dep-row">
                      <div>
                        <h3>{item.nome}</h3>
                        <p>
                          {item.parentesco}
                          {' · '}
                          {formatDate(item.dtNasc)}
                        </p>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : null}
            </div>
            <ErrorBanner message={formError} />
            <form className="stack" noValidate onSubmit={onRenew}>
              <div className="form-grid">
                <Field label="Início">
                  <input
                    type="date"
                    value={dtInicio}
                    disabled={!ativo}
                    onChange={(event) => setDtInicio(event.target.value)}
                  />
                </Field>
                <Field label="Fim">
                  <input
                    type="date"
                    value={dtFim}
                    disabled={!ativo}
                    onChange={(event) => setDtFim(event.target.value)}
                  />
                </Field>
                <Field label="Valor (R$)">
                  <MoneyInput
                    value={valor}
                    disabled={!ativo}
                    placeholder={
                      plano?.valorRenovacao == null && contrato.valor != null
                        ? moneyInput(contrato.valor)
                        : undefined
                    }
                    onChange={setValor}
                  />
                </Field>
                <Field label="Desconto (R$)">
                  <MoneyInput value={vlrDesconto} disabled={!ativo} onChange={setVlrDesconto} />
                </Field>
                <Field label="Tipo de pagamento">
                  <select value={tipoPag} disabled={!ativo} onChange={(event) => setTipoPag(event.target.value)}>
                    <option value="">Dinheiro (padrão)</option>
                    {TIPOS.map((tipo) => (
                      <option key={tipo.value} value={tipo.value}>
                        {tipo.label}
                      </option>
                    ))}
                  </select>
                </Field>
                {plano?.valorRenovacao != null ? (
                  <p className="note wide">Preço de renovação do plano, já preenchido. Apagar o campo repete o valor deste contrato.</p>
                ) : null}
                {ativo ? (
                  <fieldset className="pay-options wide">
                    <legend>Forma de pagamento</legend>
                    <label className={modo === 'vista' ? 'pay-option active' : 'pay-option'}>
                      <input
                        type="radio"
                        name="modo-renovacao"
                        checked={modo === 'vista'}
                        onChange={() => setModo('vista')}
                      />
                      <span className="pay-copy">
                        <small>À vista</small>
                        <strong>{formatMoney(totalRenovacao)}</strong>
                      </span>
                    </label>
                    <label className={modo === 'parcelado' ? 'pay-option active' : 'pay-option'}>
                      <input
                        type="radio"
                        name="modo-renovacao"
                        checked={modo === 'parcelado'}
                        onChange={() => setModo('parcelado')}
                      />
                      <span className="pay-copy">
                        <small>Parcelado</small>
                        <strong>{formatMoney(cadaParcela ?? totalRenovacao)}</strong>
                        {totalRenovacao === undefined ? (
                          <small>Informe o valor</small>
                        ) : cadaParcela !== undefined && quantidade !== undefined ? (
                          <small>
                            {quantidade}× {formatMoney(cadaParcela)}
                          </small>
                        ) : (
                          <small>Informe a quantidade de parcelas</small>
                        )}
                      </span>
                    </label>
                  </fieldset>
                ) : null}
                {ativo && modo === 'parcelado' ? (
                  <Field label="Quantidade de parcelas">
                    <DigitsInput value={parcelas} onChange={setParcelas} />
                  </Field>
                ) : null}
                {ativo && totalRenovacao !== undefined ? (
                  <dl className="totals wide">
                    <div>
                      <dt>Valor total</dt>
                      <dd>{descontoMaior ? '—' : formatMoney(totalRenovacao)}</dd>
                    </div>
                    {modo === 'parcelado' ? (
                      <div>
                        <dt>Valor de cada parcela</dt>
                        <dd>{descontoMaior ? '—' : formatMoney(cadaParcela)}</dd>
                      </div>
                    ) : null}
                    {comissaoRenovacao ? (
                      <div>
                        <dt>Comissão do vendedor</dt>
                        <dd>{comissaoRenovacao.erro ? '—' : formatMoney(comissaoRenovacao.comissao)}</dd>
                      </div>
                    ) : null}
                    {comissaoRenovacao ? (
                      <div>
                        <dt>Receita da corretora</dt>
                        <dd>{comissaoRenovacao.erro ? '—' : formatMoney(comissaoRenovacao.receita)}</dd>
                      </div>
                    ) : null}
                  </dl>
                ) : null}
                {ativo && descontoMaior ? (
                  <p className="note wide">O desconto não pode ser maior que o valor.</p>
                ) : null}
                {ativo && comissaoRenovacao?.erro ? <p className="note wide">{comissaoRenovacao.erro}</p> : null}
                <Field label="Código">
                  <input
                    value={codigo}
                    disabled={!ativo}
                    onChange={(event) => setCodigo(event.target.value)}
                  />
                </Field>
                <Field label="Descrição" wide>
                  <input
                    value={descricao}
                    disabled={!ativo}
                    onChange={(event) => setDescricao(event.target.value)}
                  />
                </Field>
              </div>
              <label className="check">
                <input
                  type="checkbox"
                  checked={confirmed}
                  disabled={!ativo}
                  onChange={(event) => setConfirmed(event.target.checked)}
                />
                <span>Confirmo que o contrato atual ficará RENOVADO e que um contrato novo será criado.</span>
              </label>
              <div className="actions">
                <button
                  className="button"
                  type="submit"
                  disabled={!ativo || saving || descontoMaior || Boolean(comissaoRenovacao?.erro)}
                >
                  {saving ? 'Renovando…' : 'Renovar'}
                </button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
    </div>
  );
}
