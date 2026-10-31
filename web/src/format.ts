export function filled(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

export function parseDecimal(value: string): number | undefined {
  const trimmed = value.trim().replace(/\s/g, '');
  if (!trimmed) {
    return undefined;
  }
  if (!/^[\d.,]+$/.test(trimmed)) {
    return Number.NaN;
  }

  let normalized: string;
  const commas = trimmed.split(',').length - 1;
  const dots = trimmed.split('.').length - 1;
  if (commas > 1) {
    return Number.NaN;
  }
  if (commas === 1) {
    normalized = trimmed.replace(/\./g, '').replace(',', '.');
  } else if (dots > 1) {
    normalized = trimmed.replace(/\./g, '');
  } else if (dots === 1) {
    const fraction = trimmed.split('.')[1] ?? '';
    normalized = fraction.length === 3 ? trimmed.replace('.', '') : trimmed;
  } else {
    normalized = trimmed;
  }
  if (normalized.endsWith('.')) {
    normalized = normalized.slice(0, -1);
  }
  if (!/^\d+(\.\d+)?$/.test(normalized)) {
    return Number.NaN;
  }
  return Number(normalized);
}

export function sanitizeMoneyTyping(value: string): string {
  const cleaned = value.replace(/[^\d.,]/g, '');
  const commaAt = cleaned.indexOf(',');
  if (commaAt === -1) {
    return cleaned;
  }
  const whole = cleaned.slice(0, commaAt + 1);
  const fraction = cleaned.slice(commaAt + 1).replace(/[.,]/g, '').slice(0, 2);
  return whole + fraction;
}

export function digitsOnly(value: string): string {
  return value.replace(/\D/g, '');
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

export function planoPreco(plano: { valor?: number | null } | null | undefined): number | undefined {
  const valor = plano?.valor;
  if (typeof valor === 'number' && Number.isFinite(valor)) {
    return valor;
  }
  return undefined;
}

export function moneyInput(value: number): string {
  return value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function parseParcelas(value: string): number | undefined {
  const trimmed = value.trim();
  if (!/^\d+$/.test(trimmed)) {
    return undefined;
  }
  const parsed = Number(trimmed);
  return parsed >= 1 ? parsed : undefined;
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
