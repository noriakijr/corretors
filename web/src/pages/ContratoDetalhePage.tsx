import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import {
  errorMessage,
  getContrato,
  listPlanos,
  listTitulares,
  listVendedores,
  renewContrato,
} from '../api.ts';
import type { Contrato, Plano, Titular, Vendedor } from '../api.ts';
import { filled, formatDateTime, formatMoney, labelOrId, moneyInput, parseDecimal, textOrDash, toApiDateTime } from '../format.ts';
import { ErrorBanner, Field, MoneyInput, StatusBadge } from '../components/ui.tsx';

function parseId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null;
  }
  return Number(value);
}

export function ContratoDetalhePage() {
  const params = useParams();
  const navigate = useNavigate();
  const contratoId = parseId(params.id);

  const [contrato, setContrato] = useState<Contrato | null>(null);
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
    if (!contrato) {
      return;
    }
    const planoRenovacao = planos.find((item) => item.id === contrato.planoId)?.valorRenovacao;
    if (planoRenovacao == null) {
      return;
    }
    setValor((current) => (current === '' ? moneyInput(planoRenovacao) : current));
  }, [contrato, planos]);

  const ativo = contrato?.status === 'ATIVO';
  const plano = planos.find((item) => item.id === contrato?.planoId);
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
      await renewContrato(contrato.id, {
        dtInicio: toApiDateTime(dtInicio),
        dtFim: toApiDateTime(dtFim),
        valor: valorNumero,
        codigo: filled(codigo),
        descricao: filled(descricao),
      });
      const destino = contrato.titularId != null ? `/titulares/${contrato.titularId}` : '/titulares';
      navigate(destino, {
        state: {
          notice: 'Contrato renovado. O anterior ficou RENOVADO e o novo está ATIVO.',
        },
      });
    } catch (cause: unknown) {
      setFormError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
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
              <dd>{labelOrId(plano?.nome, contrato.planoId, 'Plano')}</dd>
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
            <h2>Renovar</h2>
            <p className="note">
              Campos vazios repetem o preço, o plano e o vendedor. A vigência nova começa na data de fim do
              contrato atual. O anterior fica RENOVADO e o novo nasce ATIVO, ligado por este registro.
            </p>
            {!ativo ? <p className="note">Só um contrato ATIVO pode ser renovado.</p> : null}
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
