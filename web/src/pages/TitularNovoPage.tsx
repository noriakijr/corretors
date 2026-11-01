import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { createTitular, errorMessage } from '../api.ts';
import type { ContatoInput, EnderecoInput } from '../api.ts';
import { filled, maskCep, maskDocumento, maskPhone, UFS } from '../format.ts';
import { DigitsInput, ErrorBanner, Field, MaskInput } from '../components/ui.tsx';

type FormState = {
  nome: string;
  cpf: string;
  rg: string;
  dtNasc: string;
  cartaoEnv: boolean;
  seguroVidaEnv: boolean;
  telFixo: string;
  telComercial: string;
  telFax: string;
  celular: string;
  email: string;
  pais: string;
  estado: string;
  cidade: string;
  bairro: string;
  rua: string;
  numero: string;
  cep: string;
};

const initial: FormState = {
  nome: '',
  cpf: '',
  rg: '',
  dtNasc: '',
  cartaoEnv: false,
  seguroVidaEnv: false,
  telFixo: '',
  telComercial: '',
  telFax: '',
  celular: '',
  email: '',
  pais: '',
  estado: '',
  cidade: '',
  bairro: '',
  rua: '',
  numero: '',
  cep: '',
};

function enderecoOf(form: FormState): EnderecoInput | undefined {
  const endereco: EnderecoInput = {
    pais: filled(form.pais),
    estado: filled(form.estado),
    cidade: filled(form.cidade),
    bairro: filled(form.bairro),
    rua: filled(form.rua),
    numero: filled(form.numero),
    cep: filled(form.cep),
  };
  if (Object.values(endereco).every((value) => value === undefined)) {
    return undefined;
  }
  return endereco;
}

function contatoOf(form: FormState): ContatoInput | undefined {
  const contato: ContatoInput = {
    telFixo: filled(form.telFixo),
    telComercial: filled(form.telComercial),
    telFax: filled(form.telFax),
    celular: filled(form.celular),
    email: filled(form.email),
    endereco: enderecoOf(form),
  };
  if (
    contato.telFixo === undefined &&
    contato.telComercial === undefined &&
    contato.telFax === undefined &&
    contato.celular === undefined &&
    contato.email === undefined &&
    contato.endereco === undefined
  ) {
    return undefined;
  }
  return contato;
}

export function TitularNovoPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>(initial);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cepNote, setCepNote] = useState<string | null>(null);

  function setText(key: Exclude<keyof FormState, 'cartaoEnv' | 'seguroVidaEnv'>) {
    return (event: { target: { value: string } }) => {
      setForm((current) => ({ ...current, [key]: event.target.value }));
    };
  }

  function setDigits(
    key: 'cpf' | 'rg' | 'telFixo' | 'telComercial' | 'telFax' | 'celular' | 'numero' | 'cep',
  ) {
    return (value: string) => {
      setForm((current) => ({ ...current, [key]: value }));
    };
  }

  useEffect(() => {
    if (form.cep.length !== 8) {
      setCepNote(null);
      return;
    }
    let cancelled = false;
    setCepNote('Buscando CEP…');
    fetch(`https://viacep.com.br/ws/${form.cep}/json/`)
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('cep');
        }
        return response.json() as Promise<{
          erro?: boolean;
          logradouro?: string;
          bairro?: string;
          localidade?: string;
          uf?: string;
        }>;
      })
      .then((data) => {
        if (cancelled) {
          return;
        }
        if (data.erro) {
          setCepNote('CEP não encontrado. O endereço continua editável.');
          return;
        }
        setForm((current) => ({
          ...current,
          rua: data.logradouro?.trim() || current.rua,
          bairro: data.bairro?.trim() || current.bairro,
          cidade: data.localidade?.trim() || current.cidade,
          estado: data.uf?.trim() || current.estado,
          pais: current.pais.trim() ? current.pais : 'Brasil',
        }));
        setCepNote(null);
      })
      .catch(() => {
        if (!cancelled) {
          setCepNote('Não foi possível consultar o CEP. O endereço continua editável.');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [form.cep]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    const nome = filled(form.nome);
    if (!nome) {
      setError('Informe o nome do titular.');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const created = await createTitular({
        nome,
        cpf: filled(form.cpf),
        rg: filled(form.rg),
        dtNasc: filled(form.dtNasc),
        cartaoEnv: form.cartaoEnv,
        seguroVidaEnv: form.seguroVidaEnv,
        contato: contatoOf(form),
      });
      navigate(`/titulares/${created.id}`);
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page narrow">
      <header className="page-head">
        <div>
          <h1>Novo titular</h1>
          <p className="lede">Nome é obrigatório. Contato e endereço seguem com a ficha.</p>
        </div>
        <Link className="button secondary" to="/titulares">
          Voltar
        </Link>
      </header>
      <ErrorBanner message={error} />
      <form className="panel stack" noValidate onSubmit={onSubmit}>
        <h2>Identificação</h2>
        <div className="form-grid">
          <Field label="Nome" wide>
            <input value={form.nome} onChange={setText('nome')} autoComplete="name" />
          </Field>
          <Field label="CPF">
            <MaskInput value={form.cpf} onChange={setDigits('cpf')} mask={maskDocumento} maxDigits={11} />
          </Field>
          <Field label="RG">
            <MaskInput value={form.rg} onChange={setDigits('rg')} mask={maskDocumento} maxDigits={11} />
          </Field>
          <Field label="Data de nascimento">
            <input type="date" value={form.dtNasc} onChange={setText('dtNasc')} />
          </Field>
          <div className="checks wide">
            <label className="check">
              <input
                type="checkbox"
                checked={form.cartaoEnv}
                onChange={(event) => setForm((current) => ({ ...current, cartaoEnv: event.target.checked }))}
              />
              <span>Cartão enviado</span>
            </label>
            <label className="check">
              <input
                type="checkbox"
                checked={form.seguroVidaEnv}
                onChange={(event) =>
                  setForm((current) => ({ ...current, seguroVidaEnv: event.target.checked }))
                }
              />
              <span>Seguro de vida enviado</span>
            </label>
          </div>
        </div>
        <h2>Contato</h2>
        <div className="form-grid">
          <Field label="Telefone fixo">
            <MaskInput value={form.telFixo} onChange={setDigits('telFixo')} mask={maskPhone} maxDigits={11} />
          </Field>
          <Field label="Telefone comercial">
            <MaskInput value={form.telComercial} onChange={setDigits('telComercial')} mask={maskPhone} maxDigits={11} />
          </Field>
          <Field label="Fax">
            <MaskInput value={form.telFax} onChange={setDigits('telFax')} mask={maskPhone} maxDigits={11} />
          </Field>
          <Field label="Celular">
            <MaskInput value={form.celular} onChange={setDigits('celular')} mask={maskPhone} maxDigits={11} />
          </Field>
          <Field label="E-mail" wide>
            <input type="email" value={form.email} onChange={setText('email')} autoComplete="email" />
          </Field>
        </div>
        <h2>Endereço</h2>
        <div className="form-grid">
          <Field label="CEP">
            <MaskInput value={form.cep} onChange={setDigits('cep')} mask={maskCep} maxDigits={8} />
          </Field>
          {cepNote ? <p className="note wide">{cepNote}</p> : null}
          <Field label="País">
            <input value={form.pais} onChange={setText('pais')} />
          </Field>
          <Field label="Estado">
            <select value={form.estado} onChange={setText('estado')}>
              <option value="">Selecione</option>
              {UFS.map((uf) => (
                <option key={uf} value={uf}>
                  {uf}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Cidade">
            <input value={form.cidade} onChange={setText('cidade')} />
          </Field>
          <Field label="Bairro">
            <input value={form.bairro} onChange={setText('bairro')} />
          </Field>
          <Field label="Rua">
            <input value={form.rua} onChange={setText('rua')} />
          </Field>
          <Field label="Número">
            <DigitsInput value={form.numero} onChange={setDigits('numero')} />
          </Field>
        </div>
        <div className="actions">
          <button className="button" type="submit" disabled={saving}>
            {saving ? 'Salvando…' : 'Criar titular'}
          </button>
        </div>
      </form>
    </div>
  );
}
