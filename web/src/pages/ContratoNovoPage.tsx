import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import {
  createContrato,
  createVendedor,
  errorMessage,
  listPlanos,
  listTitulares,
  listVendedores,
} from '../api.ts';
import type { PagamentoType, Plano, Titular, Vendedor } from '../api.ts';
import { filled, formatMoney, moneyInput, parseDecimal, parseParcelas, planoPreco, toApiDateTime } from '../format.ts';
import { DigitsInput, ErrorBanner, Field, MoneyInput } from '../components/ui.tsx';

const TIPOS: { value: PagamentoType; label: string }[] = [
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
];

function parseId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null;
  }
  return Number(value);
}

export function ContratoNovoPage() {
  const params = useParams();
  const navigate = useNavigate();
  const titularId = parseId(params.id);

  const [titular, setTitular] = useState<Titular | null>(null);
  const [missingTitular, setMissingTitular] = useState(false);
  const [planos, setPlanos] = useState<Plano[]>([]);
  const [vendedores, setVendedores] = useState<Vendedor[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [creatingSeller, setCreatingSeller] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [planoId, setPlanoId] = useState('');
  const [vendedorId, setVendedorId] = useState('');
  const [novoVendedor, setNovoVendedor] = useState('');
  const [valor, setValor] = useState('');
  const [modo, setModo] = useState<'vista' | 'parcelado'>('vista');
  const [parcelas, setParcelas] = useState('2');
  const [codigo, setCodigo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [dtAquisicao, setDtAquisicao] = useState('');
  const [dtInicio, setDtInicio] = useState('');
  const [dtFim, setDtFim] = useState('');
  const [pagamentoDescricao, setPagamentoDescricao] = useState('');
  const [tipoPag, setTipoPag] = useState('');
  const [vlrDesconto, setVlrDesconto] = useState('');

  useEffect(() => {
    if (titularId == null) {
      setLoading(false);
      setMissingTitular(true);
      return;
    }
    let cancelled = false;
    Promise.all([listTitulares(), listPlanos(), listVendedores()])
      .then(([titulares, planosData, vendedoresData]) => {
        if (cancelled) {
          return;
        }
        const found = titulares.find((item) => item.id === titularId) ?? null;
        setTitular(found);
        setMissingTitular(found == null);
        setPlanos(planosData);
        setVendedores(vendedoresData);
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
  }, [titularId]);

  async function onCreateVendedor() {
    const nome = filled(novoVendedor);
    if (!nome) {
      setError('Informe o nome do vendedor.');
      return;
    }
    setCreatingSeller(true);
    setError(null);
    try {
      const created = await createVendedor({ nome });
      setVendedores((current) => [...current, created]);
      setVendedorId(String(created.id));
      setNovoVendedor('');
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setCreatingSeller(false);
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (titularId == null || titular == null) {
      setError('Titular não encontrado.');
      return;
    }
    if (!planoId) {
      setError('Selecione o plano.');
      return;
    }
    if (!vendedorId) {
      setError('Selecione o vendedor que fechou o contrato.');
      return;
    }
    const valorNumero = parseDecimal(valor);
    if (valorNumero === undefined || Number.isNaN(valorNumero)) {
      setError('Informe o valor do contrato.');
      return;
    }
    const desconto = parseDecimal(vlrDesconto);
    if (Number.isNaN(desconto)) {
      setError('Informe um desconto numérico ou deixe em branco.');
      return;
    }
    if (modo === 'parcelado') {
      const quantidade = parseParcelas(parcelas);
      if (quantidade === undefined || quantidade < 2) {
        setError('Informe a quantidade de parcelas, com 2 ou mais.');
        return;
      }
    }
    setSaving(true);
    setError(null);
    try {
      await createContrato({
        titularId,
        vendedorId: Number(vendedorId),
        planoId: Number(planoId),
        valor: valorNumero,
        codigo: filled(codigo),
        descricao: filled(descricao),
        dtAquisicao: toApiDateTime(dtAquisicao),
        dtInicio: toApiDateTime(dtInicio),
        dtFim: toApiDateTime(dtFim),
        pagamentoDescricao: filled(pagamentoDescricao),
        tipoPag: tipoPag ? (tipoPag as PagamentoType) : undefined,
        vlrDesconto: desconto,
      });
      navigate(`/titulares/${titularId}`, {
        state: { notice: 'Contrato ativo criado. O preço ficou no snapshot da venda.' },
      });
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  const planoSelecionado = planos.find((item) => String(item.id) === planoId);
  const precoCatalogo = planoPreco(planoSelecionado);
  const base = parseDecimal(valor);
  const descontoInformado = parseDecimal(vlrDesconto);
  const descontoAplicado =
    descontoInformado === undefined || Number.isNaN(descontoInformado) ? 0 : descontoInformado;
  const total =
    base === undefined || Number.isNaN(base) || Number.isNaN(descontoInformado ?? 0)
      ? undefined
      : base - descontoAplicado;
  const quantidade = parseParcelas(parcelas);
  const cadaParcela = total !== undefined && quantidade !== undefined ? total / quantidade : undefined;

  function onPlanoChange(nextId: string) {
    setPlanoId(nextId);
    const plano = planos.find((item) => String(item.id) === nextId);
    const preco = planoPreco(plano);
    if (preco !== undefined) {
      setValor(moneyInput(preco));
    }
  }

  return (
    <div className="page narrow">
      <header className="page-head">
        <div>
          <h1>Nova venda</h1>
          <p className="lede">
            Titular fixo nesta ficha. À vista e parcelado usam o preço do plano, ou o valor informado quando o
            catálogo não tem preço. O total já inclui o desconto.
          </p>
        </div>
        {titularId != null ? (
          <Link className="button secondary" to={`/titulares/${titularId}`}>
            Voltar à ficha
          </Link>
        ) : null}
      </header>
      <ErrorBanner message={error} />
      {loading ? <p className="muted">Carregando…</p> : null}
      {!loading && missingTitular ? <p className="empty">Titular não encontrado.</p> : null}
      {titular ? (
        <form className="panel stack" noValidate onSubmit={onSubmit}>
          <dl className="kv">
            <dt>Titular</dt>
            <dd>{titular.nome}</dd>
          </dl>
          <div className="form-grid">
            <Field label="Plano" wide>
              <select value={planoId} onChange={(event) => onPlanoChange(event.target.value)}>
                <option value="">Selecione o plano</option>
                {planos.map((plano) => (
                  <option key={plano.id} value={plano.id}>
                    {plano.nome}
                  </option>
                ))}
              </select>
            </Field>
            {planos.length === 0 ? (
              <p className="note wide">
                Nenhum plano cadastrado. <Link to="/planos">Criar um plano</Link> antes de vender.
              </p>
            ) : null}
            <Field label="Vendedor que fechou o contrato" wide>
              <select value={vendedorId} onChange={(event) => setVendedorId(event.target.value)}>
                <option value="">Selecione o vendedor</option>
                {vendedores.map((vendedor) => (
                  <option key={vendedor.id} value={vendedor.id}>
                    {vendedor.nome}
                  </option>
                ))}
              </select>
            </Field>
            <div className="inline-create wide">
              <Field label="Cadastrar vendedor agora">
                <input
                  value={novoVendedor}
                  onChange={(event) => setNovoVendedor(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault();
                      void onCreateVendedor();
                    }
                  }}
                  placeholder="Nome"
                  autoComplete="off"
                />
              </Field>
              <button
                className="button secondary"
                type="button"
                onClick={onCreateVendedor}
                disabled={creatingSeller}
              >
                {creatingSeller ? 'Salvando…' : 'Cadastrar'}
              </button>
            </div>
            <Field label="Valor (R$)">
              <MoneyInput value={valor} onChange={setValor} />
            </Field>
            <Field label="Desconto (R$)">
              <MoneyInput value={vlrDesconto} onChange={setVlrDesconto} />
            </Field>
            {planoId ? (
              <p className="note wide">
                {precoCatalogo !== undefined
                  ? 'À vista veio do preço atual do plano. Parcelado usa essa base, o desconto e a quantidade de parcelas.'
                  : 'Este plano não tem preço de catálogo. O valor informado é a base de à vista e parcelado.'}
              </p>
            ) : null}
            {planoId ? (
              <fieldset className="pay-options wide">
                <legend>Forma de pagamento</legend>
                <label className={modo === 'vista' ? 'pay-option active' : 'pay-option'}>
                  <input
                    type="radio"
                    name="modo-pagamento"
                    checked={modo === 'vista'}
                    onChange={() => setModo('vista')}
                  />
                  <span className="pay-copy">
                    <small>À vista</small>
                    <strong>{formatMoney(total)}</strong>
                  </span>
                </label>
                <label className={modo === 'parcelado' ? 'pay-option active' : 'pay-option'}>
                  <input
                    type="radio"
                    name="modo-pagamento"
                    checked={modo === 'parcelado'}
                    onChange={() => setModo('parcelado')}
                  />
                  <span className="pay-copy">
                    <small>Parcelado</small>
                    <strong>{formatMoney(cadaParcela ?? total)}</strong>
                    {total === undefined ? (
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
            {planoId && modo === 'parcelado' ? (
              <Field label="Quantidade de parcelas">
                <DigitsInput value={parcelas} onChange={setParcelas} />
              </Field>
            ) : null}
            {planoId && total !== undefined ? (
              <dl className="totals wide">
                <div>
                  <dt>Valor total</dt>
                  <dd>{formatMoney(total)}</dd>
                </div>
                {modo === 'parcelado' ? (
                  <div>
                    <dt>Valor de cada parcela</dt>
                    <dd>{formatMoney(cadaParcela)}</dd>
                  </div>
                ) : null}
              </dl>
            ) : null}
            <Field label="Código">
              <input value={codigo} onChange={(event) => setCodigo(event.target.value)} />
            </Field>
            <Field label="Descrição" wide>
              <input value={descricao} onChange={(event) => setDescricao(event.target.value)} />
            </Field>
            <Field label="Aquisição">
              <input
                type="datetime-local"
                value={dtAquisicao}
                onChange={(event) => setDtAquisicao(event.target.value)}
              />
            </Field>
            <Field label="Início">
              <input type="datetime-local" value={dtInicio} onChange={(event) => setDtInicio(event.target.value)} />
            </Field>
            <Field label="Fim">
              <input type="datetime-local" value={dtFim} onChange={(event) => setDtFim(event.target.value)} />
            </Field>
            <p className="note wide">Se o fim ficar vazio, a vigência dura um ano a partir do início.</p>
            <Field label="Descrição do pagamento">
              <input
                value={pagamentoDescricao}
                onChange={(event) => setPagamentoDescricao(event.target.value)}
              />
            </Field>
            <Field label="Tipo de pagamento">
              <select value={tipoPag} onChange={(event) => setTipoPag(event.target.value)}>
                <option value="">Dinheiro (padrão)</option>
                {TIPOS.map((tipo) => (
                  <option key={tipo.value} value={tipo.value}>
                    {tipo.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <div className="actions">
            <button className="button" type="submit" disabled={saving || planos.length === 0}>
              {saving ? 'Salvando…' : 'Registrar venda'}
            </button>
          </div>
        </form>
      ) : null}
    </div>
  );
}
