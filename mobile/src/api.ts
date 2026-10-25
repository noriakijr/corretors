export type Plano = {
  id: number;
  nome: string;
  descricao: string | null;
};

export type PlanoInput = {
  nome: string;
  descricao?: string;
};

export type Endereco = {
  id: number;
  pais: string | null;
  estado: string | null;
  cidade: string | null;
  bairro: string | null;
  rua: string | null;
  numero: string | null;
  latitude: string | null;
  longitude: string | null;
};

export type EnderecoInput = {
  pais?: string;
  estado?: string;
  cidade?: string;
  bairro?: string;
  rua?: string;
  numero?: string;
  latitude?: string;
  longitude?: string;
};

export type Contato = {
  id: number;
  telFixo: string | null;
  telComercial: string | null;
  telFax: string | null;
  celular: string | null;
  email: string | null;
  endereco: Endereco | null;
};

export type ContatoInput = {
  telFixo?: string;
  telComercial?: string;
  telFax?: string;
  celular?: string;
  email?: string;
  endereco?: EnderecoInput;
};

export type Titular = {
  id: number;
  nome: string;
  cpf: string | null;
  rg: string | null;
  dtNasc: string | null;
  cartaoEnv: boolean | null;
  seguroVidaEnv: boolean | null;
  contato: Contato | null;
};

export type TitularInput = {
  nome: string;
  cpf?: string;
  rg?: string;
  dtNasc?: string;
  cartaoEnv?: boolean;
  seguroVidaEnv?: boolean;
  contato?: ContatoInput;
};

export function apiBase(): string {
  const configured = process.env.EXPO_PUBLIC_API_URL;
  if (configured && configured.length > 0) {
    return configured.replace(/\/$/, '');
  }
  return 'http://localhost:8080';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBase()}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`HTTP ${response.status} ${path}: ${body}`);
  }
  return (await response.json()) as T;
}

export function listPlanos(): Promise<Plano[]> {
  return request<Plano[]>('/api/planos');
}

export function createPlano(input: PlanoInput): Promise<Plano> {
  return request<Plano>('/api/planos', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listTitulares(): Promise<Titular[]> {
  return request<Titular[]>('/api/titulares');
}

export function createTitular(input: TitularInput): Promise<Titular> {
  return request<Titular>('/api/titulares', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}
