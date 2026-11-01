import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';

import { errorMessage } from '../api.ts';
import { contadorDependentes, filled, formatDate } from '../format.ts';
import { ErrorBanner, Field } from './ui.tsx';

export type DependenteDraft = {
  nome: string;
  parentesco: string;
  dtNasc: string;
};

type Item = DependenteDraft & { key: string };

const NOTA_LIMITE = 'O limite de dependentes deste plano foi atingido.';

export function DependentesEditor({
  itens,
  limite,
  editavel,
  nota,
  onCreate,
  onUpdate,
  onRemove,
}: {
  itens: Item[];
  limite: number | null | undefined;
  editavel: boolean;
  nota?: string | null;
  onCreate: (item: DependenteDraft) => Promise<void> | void;
  onUpdate: (key: string, item: DependenteDraft) => Promise<void> | void;
  onRemove: (key: string) => Promise<void> | void;
}) {
  const [editing, setEditing] = useState<string | null>(null);
  const [nome, setNome] = useState('');
  const [parentesco, setParentesco] = useState('');
  const [dtNasc, setDtNasc] = useState('');
  const [saving, setSaving] = useState(false);
  const [removing, setRemoving] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const noLimite = limite == null;
  const cheio = !noLimite && itens.length >= limite;
  const incluindo = editing == null;

  function reset() {
    setEditing(null);
    setNome('');
    setParentesco('');
    setDtNasc('');
    setError(null);
  }

  useEffect(() => {
    if (!editavel) {
      reset();
    }
  }, [editavel]);

  useEffect(() => {
    if (editing != null && !itens.some((item) => item.key === editing)) {
      reset();
    }
  }, [editing, itens]);

  function beginEdit(key: string) {
    const item = itens.find((atual) => atual.key === key);
    if (!item) {
      return;
    }
    setEditing(key);
    setNome(item.nome);
    setParentesco(item.parentesco);
    setDtNasc(item.dtNasc.slice(0, 10));
    setError(null);
  }

  async function onSave(event?: FormEvent) {
    event?.preventDefault();
    const nomeDependente = filled(nome);
    const parentescoDependente = filled(parentesco);
    if (!nomeDependente) {
      setError('Informe o nome do dependente.');
      return;
    }
    if (!parentescoDependente) {
      setError('Informe o parentesco.');
      return;
    }
    if (!dtNasc) {
      setError('Informe a data de nascimento.');
      return;
    }
    const draft = { nome: nomeDependente, parentesco: parentescoDependente, dtNasc };
    setSaving(true);
    setError(null);
    try {
      if (editing == null) {
        await onCreate(draft);
      } else {
        await onUpdate(editing, draft);
      }
      reset();
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setSaving(false);
    }
  }

  async function remove(key: string) {
    setRemoving(key);
    setError(null);
    try {
      await onRemove(key);
      if (editing === key) {
        reset();
      }
    } catch (cause: unknown) {
      setError(errorMessage(cause));
    } finally {
      setRemoving(null);
    }
  }

  return (
    <div className="stack">
      <p className="counter">Dependentes: {contadorDependentes(itens.length, limite)}</p>
      {nota ? <p className="note">{nota}</p> : null}
      <ErrorBanner message={error} />
      {itens.length === 0 ? <p className="empty">Nenhum dependente neste contrato.</p> : null}
      {itens.length > 0 ? (
        <ul className="dep-list">
          {itens.map((item) => (
            <li key={item.key} className="dep-row">
              <div>
                <h3>{item.nome}</h3>
                <p>
                  {item.parentesco}
                  {' · '}
                  {formatDate(item.dtNasc)}
                </p>
              </div>
              {editavel ? (
                <div className="dep-actions">
                  <button className="button secondary small" type="button" onClick={() => beginEdit(item.key)}>
                    Editar
                  </button>
                  <button
                    className="button secondary small"
                    type="button"
                    disabled={removing === item.key}
                    onClick={() => void remove(item.key)}
                  >
                    {removing === item.key ? 'Removendo…' : 'Remover'}
                  </button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
      {editavel ? (
        <form className="stack" noValidate onSubmit={(event) => void onSave(event)}>
          <div className="form-grid">
            <Field label="Nome">
              <input value={nome} onChange={(event) => setNome(event.target.value)} autoComplete="off" />
            </Field>
            <Field label="Parentesco">
              <input
                value={parentesco}
                onChange={(event) => setParentesco(event.target.value)}
                autoComplete="off"
              />
            </Field>
            <Field label="Data de nascimento">
              <input type="date" value={dtNasc} onChange={(event) => setDtNasc(event.target.value)} />
            </Field>
          </div>
          {incluindo && cheio ? <p className="note">{NOTA_LIMITE}</p> : null}
          <div className="actions">
            <button className="button" type="submit" disabled={saving || (incluindo && cheio)}>
              {saving ? 'Salvando…' : incluindo ? 'Incluir dependente' : 'Salvar dependente'}
            </button>
            {editing != null ? (
              <button className="button secondary" type="button" onClick={reset}>
                Cancelar
              </button>
            ) : null}
          </div>
        </form>
      ) : null}
    </div>
  );
}
