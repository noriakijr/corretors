import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import {
  createContrato,
  errorMessage,
  listPlanos,
  listSeguradoras,
  listTitulares,
  listVendedores,
} from '../api.ts';
import type { PagamentoType, Plano, Seguradora, Titular, Vendedor } from '../api.ts';
import {
  calcularComissao,
  filled,
  formatMoney,
  hojeIso,
  moneyInput,
  parseDecimal,
  parseParcelas,
  periodoVigente,
  planoPreco,
  plusYearsIso,
  regraDoVendedor,
  toApiDate,
  toApiDateTime,
} from '../format.ts';
import { DependentesEditor } from '../components/DependentesEditor.tsx';
import type { DependenteDraft } from '../components/DependentesEditor.tsx';
import { DigitsInput, ErrorBanner, Field, MoneyInput } from '../components/ui.tsx';

const TIPOS: { value: PagamentoType; label: string }[] = [
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },
  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },
  { value: 'PIX', label: 'Pix' },
];

type DependenteVenda = DependenteDraft & { key: string };

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
  const [seguradoras, setSeguradoras] = useState<Seguradora[]>([]);
  const [vendedores, setVendedores] = useState<Vendedor[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [seguradoraId, setSeguradoraId] = useState('');
  const [planoId, setPlanoId] = useState('');
  const [vendedorId, setVendedorId] = useState('');
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
  const [dependentes, setDependentes] = useState<DependenteVenda[]>([]);

  useEffect(() => {
    if (titularId == null) {
      setLoading(false);
      setMissingTitular(true);
      return;
    }
    let cancelled = false;
    Promise.all([listTitulares(), listPlanos(), listSeguradoras(), listVendedores()])
      .then(([titulares, planosData, seguradorasData, vendedoresData]) => {
        if (cancelled) {
          return;
        }
        const found = titulares.find((item) => item.id === titularId) ?? null;
        setTitular(found);
        setMissingTitular(found == null);
        setPlanos(planosData);
        setSeguradoras(seguradorasData);
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

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (titularId == null || titular == null) {
      setError('Titular não encontrado.');
      return;
    }
    if (!seguradoraId) {
      setError('Selecione a seguradora.');
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
    if (desconto !== undefined && desconto > valorNumero) {
      setError('O desconto não pode ser maior que o valor.');
      return;
    }
    const quantidadeParcelas = modo === 'parcelado' ? parseParcelas(parcelas) : undefined;
    if (modo === 'parcelado' && (quantidadeParcelas === undefined || quantidadeParcelas < 2)) {
      setError('Informe a quantidade de parcelas, com 2 ou mais.');
      return;
    }
    if (dependentes.length > 0 && !vendaVigente) {
      setError('Dependentes só podem ser incluídos com o contrato vigente. Ajuste as datas ou remova os dependentes.');
      return;
    }
    const limite = planoSelecionado?.limiteDependentes;
    if (limite != null && dependentes.length > limite) {
      setError('A quantidade de dependentes passa do limite do plano.');
      return;
    }
    const totalVenda = valorNumero - (desconto ?? 0);
    const previa = calcularComissao(regraDoVendedor(planoSelecionado, Number(vendedorId)), totalVenda);
    if (previa.erro) {
      setError(previa.erro);
      return;
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
        dtInicio: toApiDate(dtInicio),
        dtFim: toApiDate(dtFim),
        pagamentoDescricao: filled(pagamentoDescricao),
        tipoPag: tipoPag ? (tipoPag as PagamentoType) : undefined,
        vlrDesconto: desconto,
        quantidadeParcelas,
        dependentes:
          dependentes.length > 0
            ? dependentes.map(({ nome, parentesco, dtNasc }) => ({ nome, parentesco, dtNasc }))
            : undefined,
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

  const planosDaSeguradora = seguradoraId
    ? planos.filter((plano) => String(plano.seguradoraId) === seguradoraId)
    : [];
  const planoSelecionado = planosDaSeguradora.find((item) => String(item.id) === planoId);
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
  const comissaoVenda =
    planoSelecionado && vendedorId && total !== undefined && !Number.isNaN(total)
      ? calcularComissao(regraDoVendedor(planoSelecionado, Number(vendedorId)), total)
      : undefined;

  const aquisicaoEfetiva = dtAquisicao ? dtAquisicao.slice(0, 10) : hojeIso();
  const inicioEfetivo = dtInicio ? dtInicio.slice(0, 10) : aquisicaoEfetiva;
  const fimEfetivo = dtFim ? dtFim.slice(0, 10) : plusYearsIso(inicioEfetivo, 1);
  const vendaVigente = periodoVigente(inicioEfetivo, fimEfetivo);

  function incluirDependente(item: DependenteDraft) {
    setDependentes((current) => [...current, { ...item, key: crypto.randomUUID() }]);
  }

  function alterarDependente(key: string, item: DependenteDraft) {
    setDependentes((current) =>
      current.map((atual) => (atual.key === key ? { ...atual, ...item, key } : atual)),
    );
  }

  function removerDependente(key: string) {
    setDependentes((current) => current.filter((atual) => atual.key !== key));
  }

  function onSeguradoraChange(nextId: string) {
    setSeguradoraId(nextId);
    setPlanoId('');
    setValor('');
  }

  function onPlanoChange(nextId: string) {
    setPlanoId(nextId);
    const plano = planos.find((item) => String(item.id) === nextId);
    const preco = planoPreco(plano);
    const nextValor = preco === undefined ? '' : moneyInput(preco);
    setValor(nextValor);
    window.setTimeout(() => setValor(nextValor), 0);
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
        <div className="panel stack">
        <form id="venda" className="stack" noValidate onSubmit={onSubmit}>
          <dl className="kv">
            <dt>Titular</dt>
            <dd>{titular.nome}</dd>
          </dl>
          <div className="form-grid">
            <Field label="Seguradora" wide>
              <select value={seguradoraId} onChange={(event) => onSeguradoraChange(event.target.value)}>
                <option value="">Selecione a seguradora</option>
                {seguradoras.map((seguradora) => (
                  <option key={seguradora.id} value={seguradora.id}>
                    {seguradora.nome}
                  </option>
                ))}
              </select>
            </Field>
            {seguradoras.length === 0 ? (
              <p className="note wide">
                Nenhuma seguradora cadastrada. <Link to="/seguradoras">Cadastrar uma seguradora</Link> antes de vender.
              </p>
            ) : null}
            <Field label="Plano" wide>
              <select
                value={planoId}
                disabled={!seguradoraId}
                onChange={(event) => onPlanoChange(event.target.value)}
              >
                <option value="">
                  {seguradoraId ? 'Selecione o plano' : 'Selecione a seguradora primeiro'}
                </option>
                {planosDaSeguradora.map((plano) => (
                  <option key={plano.id} value={plano.id}>
                    {plano.nome} — {plano.valor == null ? 'Sem preço' : formatMoney(plano.valor)}
                  </option>
                ))}
              </select>
            </Field>
            {seguradoraId && planosDaSeguradora.length === 0 ? (
              <p className="note wide">
                Esta seguradora não tem planos.{' '}
                <Link to={`/seguradoras/${seguradoraId}`}>Cadastrar um plano</Link>.
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
            <Field label="Valor (R$)">
              <MoneyInput value={valor} onChange={setValor} />
            </Field>
            <Field label="Desconto (R$)">
              <MoneyInput value={vlrDesconto} onChange={setVlrDesconto} />
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
            <Field label="Descrição do pagamento">
              <input
                value={pagamentoDescricao}
                onChange={(event) => setPagamentoDescricao(event.target.value)}
              />
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
                {comissaoVenda ? (
                  <div>
                    <dt>Comissão do vendedor</dt>
                    <dd>{comissaoVenda.erro ? '—' : formatMoney(comissaoVenda.comissao)}</dd>
                  </div>
                ) : null}
                {comissaoVenda ? (
                  <div>
                    <dt>Receita da corretora</dt>
                    <dd>{comissaoVenda.erro ? '—' : formatMoney(comissaoVenda.receita)}</dd>
                  </div>
                ) : null}
              </dl>
            ) : null}
            {comissaoVenda?.erro ? <p className="note wide">{comissaoVenda.erro}</p> : null}
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
              <input type="date" value={dtInicio} onChange={(event) => setDtInicio(event.target.value)} />
            </Field>
            <Field label="Fim">
              <input type="date" value={dtFim} onChange={(event) => setDtFim(event.target.value)} />
            </Field>
            <p className="note wide">Se o fim ficar vazio, a vigência dura um ano a partir do início.</p>
          </div>
        </form>
        <section className="stack">
          <h2>Dependentes</h2>
          <DependentesEditor
            itens={dependentes}
            limite={planoSelecionado ? planoSelecionado.limiteDependentes : undefined}
            editavel={Boolean(planoSelecionado) && vendaVigente}
            nota={
              !planoSelecionado
                ? 'Selecione o plano para incluir dependentes.'
                : vendaVigente
                  ? 'Eles entram neste contrato, até o limite do plano. Alterar um dependente não ocupa vaga nova.'
                  : 'O contrato só recebe dependentes enquanto estiver vigente: ativo e com hoje entre o início e o fim.'
            }
            onCreate={incluirDependente}
            onUpdate={alterarDependente}
            onRemove={removerDependente}
          />
        </section>
        <div className="actions">
          <button className="button" type="submit" form="venda" disabled={saving || Boolean(comissaoVenda?.erro)}>
            {saving ? 'Salvando…' : 'Registrar venda'}
          </button>
        </div>
        </div>
      ) : null}
    </div>
  );
}
