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
import type { Contrato, Dependente, Plano, Titular, Vendedor } from '../api.ts';
import { filled, formatDate, formatDateTime, formatMoney, labelOrId, moneyInput, parseDecimal, periodoVigente, planoRotulo, textOrDash, toApiDateTime } from '../format.ts';
import { DependentesEditor } from '../components/DependentesEditor.tsx';
import type { DependenteDraft } from '../components/DependentesEditor.tsx';
import { ErrorBanner, Field, MoneyInput, OkBanner, StatusBadge } from '../components/ui.tsx';

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
  const copiados = limite == null ? dependentes : dependentes.slice(0, limite);
  const vendedor = vendedores.find((item) => item.id === contrato?.vendedorId);
  const titular = titulares.find((item) => item.id === contrato?.titularId);

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
    setSaving(true);
    setFormError(null);
    try {
      const novo = await renewContrato(contrato.id, {
        dtInicio: toApiDateTime(dtInicio),
        dtFim: toApiDateTime(dtFim),
        valor: valorNumero,
        codigo: filled(codigo),
        descricao: filled(descricao),
      });
      navigate(`/contratos/${novo.id}`, {
        state: {
          notice: 'Contrato renovado. Os dependentes do contrato anterior foram copiados, dentro do limite do plano.',
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
                <StatusBadge status={contrato.status} />
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
              <dt>Aquisição</dt>
              <dd>{formatDateTime(contrato.dtAquisicao)}</dd>
              <dt>Início</dt>
              <dd>{formatDateTime(contrato.dtInicio)}</dd>
              <dt>Fim</dt>
              <dd>{formatDateTime(contrato.dtFim)}</dd>
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
                vigente
                  ? 'Inclusão, alteração e remoção enquanto o contrato está vigente. Remover libera uma vaga.'
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
              vazio dura um ano a partir desse início. O anterior fica RENOVADO e o novo nasce ATIVO, ligado por este
              registro.
            </p>
            {!ativo ? <p className="note">Só um contrato ATIVO pode ser renovado.</p> : null}
            <div className="stack">
              <h3>Dependentes que serão copiados</h3>
              {dependentes.length === 0 ? <p className="empty">Nenhum dependente para copiar.</p> : null}
              {limite != null && dependentes.length > limite ? (
                <p className="note">
                  O plano renovado permite {limite}. Os primeiros {limite} serão copiados.
                </p>
              ) : null}
              {copiados.length > 0 ? (
                <ul className="dep-list">
                  {copiados.map((item) => (
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
                    type="datetime-local"
                    value={dtInicio}
                    disabled={!ativo}
                    onChange={(event) => setDtInicio(event.target.value)}
                  />
                </Field>
                <Field label="Fim">
                  <input
                    type="datetime-local"
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
                {plano?.valorRenovacao != null ? (
                  <p className="note wide">Preço de renovação do plano, já preenchido. Apagar o campo repete o valor deste contrato.</p>
                ) : null}
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
                <button className="button" type="submit" disabled={!ativo || saving}>
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
