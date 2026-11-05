import type { ReactNode } from 'react';

import type { ContratoStatus, ParcelaStatus } from '../api.ts';
import { digitsOnly, moneyInput, parseDecimal, sanitizeMoneyTyping, takeDigits } from '../format.ts';

const STATUS_LABEL: Record<ContratoStatus, string> = {
  ATIVO: 'Ativo',
  ENCERRADO: 'Encerrado',
  RENOVADO: 'Renovado',
  CANCELADO: 'Cancelado',
};

export function ErrorBanner({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return (
    <div className="banner error" role="alert">
      {message}
    </div>
  );
}

export function OkBanner({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return (
    <div className="banner ok" role="status">
      {message}
    </div>
  );
}

const PARCELA_LABEL: Record<ParcelaStatus, string> = {
  ABERTA: 'Em aberto',
  PAGA: 'Paga',
  VENCIDA: 'Vencida',
};

export function StatusBadge({ status }: { status: ContratoStatus }) {
  return <span className={`status ${status}`}>{STATUS_LABEL[status]}</span>;
}

export function PontoAtencao() {
  return <span className="status atencao">Ponto de atenção</span>;
}

export function ParcelaBadge({ status }: { status: ParcelaStatus }) {
  return <span className={`status ${status}`}>{PARCELA_LABEL[status]}</span>;
}

export function Field({
  label,
  children,
  wide,
}: {
  label: string;
  children: ReactNode;
  wide?: boolean;
}) {
  return (
    <label className={wide ? 'field wide' : 'field'}>
      <span>{label}</span>
      {children}
    </label>
  );
}

export function MoneyInput({
  value,
  onChange,
  disabled,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
}) {
  return (
    <input
      value={value}
      disabled={disabled}
      placeholder={placeholder}
      inputMode="decimal"
      autoComplete="off"
      onChange={(event) => onChange(sanitizeMoneyTyping(event.target.value))}
      onBlur={() => {
        const parsed = parseDecimal(value);
        if (parsed !== undefined && !Number.isNaN(parsed)) {
          onChange(moneyInput(parsed));
        }
      }}
    />
  );
}

export function DigitsInput({
  value,
  onChange,
  disabled,
}: {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
}) {
  return (
    <input
      value={value}
      disabled={disabled}
      inputMode="numeric"
      autoComplete="off"
      onChange={(event) => onChange(digitsOnly(event.target.value))}
    />
  );
}

export function MaskInput({
  value,
  onChange,
  mask,
  maxDigits,
}: {
  value: string;
  onChange: (value: string) => void;
  mask: (value: string) => string;
  maxDigits: number;
}) {
  return (
    <input
      value={mask(value)}
      inputMode="numeric"
      autoComplete="off"
      onChange={(event) => onChange(takeDigits(event.target.value, maxDigits))}
    />
  );
}
