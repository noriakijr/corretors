export function filled(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

export function parseDecimal(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  const normalized = trimmed.replace(/\s/g, '').replace(',', '.');
  if (!/^[+-]?\d+(\.\d+)?$/.test(normalized)) {
    return Number.NaN;
  }
  return Number(normalized);
}

export function toApiDateTime(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(trimmed)) {
    return `${trimmed}:00`;
  }
  return trimmed;
}

export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  if (!match) {
    return value;
  }
  return `${match[3]}/${match[2]}/${match[1]}`;
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/.exec(value);
  if (!match) {
    return value;
  }
  return `${match[3]}/${match[2]}/${match[1]} ${match[4]}:${match[5]}`;
}

export function formatMoney(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) {
    return '—';
  }
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

export function yesNo(value: boolean | null | undefined): string {
  if (value == null) {
    return '—';
  }
  return value ? 'Sim' : 'Não';
}

export function textOrDash(value: string | null | undefined): string {
  if (!value || value.trim().length === 0) {
    return '—';
  }
  return value;
}

export function labelOrId(nome: string | null | undefined, id: number | null | undefined, kind: string): string {
  if (nome && nome.trim().length > 0) {
    return id == null ? nome : `${nome} (${id})`;
  }
  if (id == null) {
    return '—';
  }
  return `${kind} ${id}`;
}
