import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';

import { errorMessage, listContratosByTitular, listPlanos, listTitulares } from '../api.ts';
import type { Contrato, Plano, Titular } from '../api.ts';
import { formatCep, formatDate, formatDocumento, formatMoney, formatPhone, planoRotulo, textOrDash, yesNo } from '../format.ts';
import { ErrorBanner, OkBanner, PontoAtencao, StatusBadge } from '../components/ui.tsx';

function parseId(value: string | undefined): number | null {
  if (!value || !/^\d+$/.test(value)) {
    return null;
  }
  return Number(value);
}

export function TitularFichaPage() {
  const params = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const titularId = parseId(params.id);
  const notice =
    location.state && typeof location.state === 'object' && 'notice' in location.state
      ? String((location.state as { notice?: unknown }).notice ?? '')
      : '';

  const [titular, setTitular] = useState<Titular | null>(null);
  const [planos, setPlanos] = useState<Plano[]>([]);
  const [contratos, setContratos] = useState<Contrato[]>([]);
  const [loading, setLoading] = useState(true);
  const [missing, setMissing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [contratosError, setContratosError] = useState<string | null>(null);

  useEffect(() => {
    if (titularId == null) {
      setLoading(false);
      setMissing(true);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setMissing(false);
    setError(null);
    setContratosError(null);

    listTitulares()
      .then((data) => {
        if (cancelled) {
          return;
        }
        const found = data.find((item) => item.id === titularId) ?? null;
        setTitular(found);
        setMissing(found == null);
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

    listContratosByTitular(titularId)
      .then((data) => {
        if (!cancelled) {
          setContratos(data);
        }
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setContratosError(errorMessage(cause));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [titularId]);

  const contato = titular?.contato;
  const endereco = contato?.endereco;

  return (
    <div className="page">
      <header className="page-head">
        <div>
          <h1>{titular ? titular.nome : 'Titular'}</h1>
          <p className="lede">Ficha do cliente e os contratos desta pessoa. Vários ativos podem coexistir.</p>
        </div>
        <div className="actions">
          <Link className="button secondary" to="/titulares">
            Lista
          </Link>
          {titular ? (
            <Link className="button" to={`/titulares/${titular.id}/contratos/novo`}>
              Nova venda
            </Link>
          ) : null}
        </div>
      </header>
      <OkBanner message={notice || null} />
      <ErrorBanner message={error} />
      {loading ? <p className="muted">Carregando…</p> : null}
      {!loading && missing ? <p className="empty">Titular não encontrado.</p> : null}
      {titular ? (
        <div className="ficha">
          <section className="panel">
            <h2>Dados</h2>
            <dl className="kv">
              <dt>Nome</dt>
              <dd>{titular.nome}</dd>
              <dt>CPF</dt>
              <dd>{formatDocumento(titular.cpf)}</dd>
              <dt>RG</dt>
              <dd>{formatDocumento(titular.rg)}</dd>
              <dt>Nascimento</dt>
              <dd>{formatDate(titular.dtNasc)}</dd>
              <dt>Cartão enviado</dt>
              <dd>{yesNo(titular.cartaoEnv)}</dd>
              <dt>Seguro de vida enviado</dt>
              <dd>{yesNo(titular.seguroVidaEnv)}</dd>
              <dt>Telefone fixo</dt>
              <dd>{formatPhone(contato?.telFixo)}</dd>
              <dt>Comercial</dt>
              <dd>{formatPhone(contato?.telComercial)}</dd>
              <dt>Fax</dt>
              <dd>{formatPhone(contato?.telFax)}</dd>
              <dt>Celular</dt>
              <dd>{formatPhone(contato?.celular)}</dd>
              <dt>E-mail</dt>
              <dd>{textOrDash(contato?.email)}</dd>
              <dt>Endereço</dt>
              <dd>
                {endereco
                  ? [
                      formatCep(endereco.cep) === '—' ? null : formatCep(endereco.cep),
                      endereco.rua,
                      endereco.numero,
                      endereco.bairro,
                      endereco.cidade,
                      endereco.estado,
                      endereco.pais,
                    ]
                      .filter((part) => part && part.trim().length > 0)
                      .join(', ') || '—'
                  : '—'}
              </dd>
            </dl>
          </section>
          <section className="panel">
            <h2>Contratos</h2>
            <ErrorBanner message={contratosError} />
            {!contratosError && contratos.length === 0 ? (
              <p className="empty">Este titular ainda não tem contratos.</p>
            ) : null}
            {contratos.length > 0 ? (
              <table className="table">
                <thead>
                  <tr>
                    <th>Código</th>
                    <th>Status</th>
                    <th>Plano</th>
                    <th className="num">Valor</th>
                    <th>Início</th>
                    <th>Fim</th>
                  </tr>
                </thead>
                <tbody>
                  {contratos.map((contrato) => {
                    const plano = planos.find((item) => item.id === contrato.planoId);
                    return (
                      <tr
                        key={contrato.id}
                        className="clickable"
                        onClick={() => navigate(`/contratos/${contrato.id}`)}
                      >
                        <td>
                          <Link to={`/contratos/${contrato.id}`}>{textOrDash(contrato.codigo)}</Link>
                        </td>
                        <td>
                          <span className="badges">
                            <StatusBadge status={contrato.status} />
                            {contrato.pontoAtencao ? <PontoAtencao /> : null}
                          </span>
                        </td>
                        <td>{planoRotulo(plano?.nome, plano?.seguradoraNome, contrato.planoId)}</td>
                        <td className="num">{formatMoney(contrato.valor)}</td>
                        <td>{formatDate(contrato.dtInicio)}</td>
                        <td>{formatDate(contrato.dtFim)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ) : null}
          </section>
        </div>
      ) : null}
    </div>
  );
}
