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

export function takeDigits(value: string, max: number): string {
  return digitsOnly(value).slice(0, max);
}

export function maskDocumento(value: string): string {
  const digits = takeDigits(value, 11);
  let masked = digits.slice(0, 3);
  if (digits.length > 3) {
    masked += `.${digits.slice(3, 6)}`;
  }
  if (digits.length > 6) {
    masked += `.${digits.slice(6, 9)}`;
  }
  if (digits.length > 9) {
    masked += `-${digits.slice(9)}`;
  }
  return masked;
}

export function maskPhone(value: string): string {
  const digits = takeDigits(value, 11);
  if (!digits) {
    return '';
  }
  if (digits.length <= 2) {
    return `(${digits}`;
  }
  const ddd = digits.slice(0, 2);
  const rest = digits.slice(2);
  if (digits.length <= 10) {
    return rest.length <= 4 ? `(${ddd}) ${rest}` : `(${ddd}) ${rest.slice(0, 4)}-${rest.slice(4)}`;
  }
  return `(${ddd}) ${rest.slice(0, 5)}-${rest.slice(5)}`;
}

export function maskCep(value: string): string {
  const digits = takeDigits(value, 8);
  if (digits.length <= 5) {
    return digits;
  }
  return `${digits.slice(0, 5)}-${digits.slice(5)}`;
}

export function formatDocumento(value: string | null | undefined): string {
  if (!value || value.trim().length === 0) {
    return '—';
  }
  const digits = digitsOnly(value);
  if (digits.length === 11 && /^[\d.\-\s]+$/.test(value.trim())) {
    return maskDocumento(digits);
  }
  return value;
}

export function formatPhone(value: string | null | undefined): string {
  if (!value || value.trim().length === 0) {
    return '—';
  }
  const digits = digitsOnly(value);
  if ((digits.length === 10 || digits.length === 11) && /^[\d()\-\s]+$/.test(value.trim())) {
    return maskPhone(digits);
  }
  return value;
}

export function formatCep(value: string | null | undefined): string {
  if (!value || value.trim().length === 0) {
    return '—';
  }
  const digits = digitsOnly(value);
  if (digits.length === 8 && /^[\d\-\s]+$/.test(value.trim())) {
    return maskCep(digits);
  }
  return value;
}

export const UFS = [
  'AC',
  'AL',
  'AP',
  'AM',
  'BA',
  'CE',
  'DF',
  'ES',
  'GO',
  'MA',
  'MT',
  'MS',
  'MG',
  'PA',
  'PB',
  'PR',
  'PE',
  'PI',
  'RJ',
  'RN',
  'RS',
  'RO',
  'RR',
  'SC',
  'SP',
  'SE',
  'TO',
] as const;

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

export function toApiDate(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  const match = /^(\d{4}-\d{2}-\d{2})(?:$|T)/.exec(trimmed);
  if (!match) {
    return trimmed;
  }
  return match[1];
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

export function planoRotulo(
  nome: string | null | undefined,
  seguradora: string | null | undefined,
  id: number | null | undefined,
): string {
  const base = labelOrId(nome, id, 'Plano');
  if (seguradora && seguradora.trim().length > 0) {
    return `${base} — ${seguradora.trim()}`;
  }
  return base;
}

export function hojeIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function plusYearsIso(isoDate: string, years: number): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  const date = new Date(year ?? 1970, (month ?? 1) - 1, day ?? 1);
  date.setFullYear(date.getFullYear() + years);
  const nextMonth = String(date.getMonth() + 1).padStart(2, '0');
  const nextDay = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${nextMonth}-${nextDay}`;
}

export function periodoVigente(inicio: string | null | undefined, fim: string | null | undefined): boolean {
  if (!inicio || !fim) {
    return false;
  }
  const dataInicio = inicio.slice(0, 10);
  const dataFim = fim.slice(0, 10);
  const hoje = hojeIso();
  return dataInicio <= hoje && hoje <= dataFim;
}

export function contadorDependentes(usados: number, limite: number | null | undefined): string {
  if (limite === undefined) {
    return String(usados);
  }
  if (limite === null) {
    return `${usados} / sem limite`;
  }
  return `${usados} / ${limite}`;
}

export type ModoComissao = 'PERCENTUAL' | 'VALOR_FIXO';

export type RegraComissao = {
  tipoComissao?: ModoComissao | null;
  percentual?: number | null;
  valorFixo?: number | null;
};

export type PlanoComComissao = RegraComissao & {
  excecoes?: Array<RegraComissao & { vendedorId: number }>;
};

export type ComissaoCalculada = {
  comissao: number;
  receita: number;
  erro?: string;
};

export function regraDoVendedor(
  plano: PlanoComComissao | null | undefined,
  vendedorId: number | null | undefined,
): RegraComissao | null {
  if (!plano) {
    return null;
  }
  const excecao = plano.excecoes?.find((item) => item.vendedorId === vendedorId);
  if (excecao) {
    return excecao;
  }
  if (plano.tipoComissao === 'PERCENTUAL' || plano.tipoComissao === 'VALOR_FIXO') {
    return plano;
  }
  return null;
}

export function calcularComissao(regra: RegraComissao | null, total: number): ComissaoCalculada {
  const totalCents = Math.round(total * 100);
  if (!regra?.tipoComissao) {
    return { comissao: 0, receita: totalCents / 100 };
  }
  if (regra.tipoComissao === 'PERCENTUAL') {
    const percentual = regra.percentual ?? 0;
    if (percentual > 100) {
      return {
        comissao: 0,
        receita: totalCents / 100,
        erro: 'O percentual da comissão não pode passar de 100.',
      };
    }
    const comissaoCents = Math.round((totalCents * percentual) / 100);
    return { comissao: comissaoCents / 100, receita: (totalCents - comissaoCents) / 100 };
  }
  const fixoCents = Math.round((regra.valorFixo ?? 0) * 100);
  if (fixoCents > totalCents) {
    return {
      comissao: fixoCents / 100,
      receita: totalCents / 100,
      erro: 'O valor fixo da comissão é maior que o total.',
    };
  }
  return { comissao: fixoCents / 100, receita: (totalCents - fixoCents) / 100 };
}

export function rotuloRegra(regra: RegraComissao | null | undefined): string {
  if (!regra?.tipoComissao) {
    return 'Sem comissão';
  }
  if (regra.tipoComissao === 'PERCENTUAL') {
    return `${(regra.percentual ?? 0).toLocaleString('pt-BR')}%`;
  }
  return formatMoney(regra.valorFixo);
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
