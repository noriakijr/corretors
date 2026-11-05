export type TipoComissao = 'PERCENTUAL' | 'VALOR_FIXO';

export type ComissaoVendedor = {
  vendedorId: number;
  vendedorNome: string | null;
  tipoComissao: TipoComissao;
  percentual: number | null;
  valorFixo: number | null;
};

export type ComissaoInput = {
  tipoComissao?: TipoComissao | null;
  percentual?: number | null;
  valorFixo?: number | null;
};

export type Plano = {
  id: number;
  nome: string;
  descricao: string | null;
  valor: number | null;
  valorRenovacao: number | null;
  seguradoraId: number | null;
  seguradoraNome: string | null;
  limiteDependentes: number | null;
  tipoComissao: TipoComissao | null;
  percentual: number | null;
  valorFixo: number | null;
  excecoes: ComissaoVendedor[];
};

export type Seguradora = {
  id: number;
  nome: string;
};

export type PlanoInput = {
  nome: string;
  descricao?: string;
  valor?: number;
  valorRenovacao?: number;
  limiteDependentes?: number | null;
  tipoComissao?: TipoComissao;
  percentual?: number;
  valorFixo?: number;
};

export type PlanoPreco = {
  id: number;
  valor: number;
  dtModif: string;
  renovacao: boolean;
};

export type Endereco = {
  id: number;
  pais: string | null;
  estado: string | null;
  cidade: string | null;
  bairro: string | null;
  rua: string | null;
  numero: string | null;
  cep: string | null;
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
  cep?: string;
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

export type Vendedor = {
  id: number;
  nome: string;
};

export type VendedorInput = {
  nome: string;
};

export type ContratoStatus = 'ATIVO' | 'ENCERRADO' | 'RENOVADO' | 'CANCELADO';

export type PagamentoType = 'DINHEIRO' | 'CHEQUE' | 'CARTAO_DEBITO' | 'CARTAO_CREDITO' | 'PIX';

export type Contrato = {
  id: number;
  codigo: string | null;
  descricao: string | null;
  dtAquisicao: string | null;
  dtInicio: string | null;
  dtFim: string | null;
  status: ContratoStatus;
  titularId: number | null;
  vendedorId: number | null;
  planoHistoricoId: number | null;
  planoId: number | null;
  valor: number | null;
  pagamentoId: number | null;
  contratoAnteriorId: number | null;
  valorComissao: number | null;
  receitaCorretora: number | null;
};

export type Dependente = {
  id: number;
  nome: string;
  parentesco: string;
  dtNasc: string;
  contratoId: number | null;
};

export type DependenteInput = {
  nome: string;
  parentesco: string;
  dtNasc: string;
};

export type ContratoInput = {
  titularId: number;
  vendedorId: number;
  planoId: number;
  valor: number;
  codigo?: string;
  descricao?: string;
  dtAquisicao?: string;
  dtInicio?: string;
  dtFim?: string;
  pagamentoDescricao?: string;
  tipoPag?: PagamentoType;
  vlrDesconto?: number;
  quantidadeParcelas?: number;
  dependentes?: DependenteInput[];
};

export type RenovarContratoInput = {
  dtInicio?: string;
  dtFim?: string;
  valor?: number;
  codigo?: string;
  descricao?: string;
  tipoPag?: PagamentoType;
  vlrDesconto?: number;
  quantidadeParcelas?: number;
};

export function apiBase(): string {
  const configured = import.meta.env.VITE_API_URL;
  if (configured && configured.length > 0) {
    return configured.replace(/\/$/, '');
  }
  return 'http://localhost:8080';
}

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function errorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return 'Erro inesperado';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${apiBase()}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...(init?.headers ?? {}),
      },
    });
  } catch {
    throw new ApiError(
      0,
      `Não foi possível falar com a API em ${apiBase()}. Suba a API com ./gradlew bootRun.`,
    );
  }

  if (!response.ok) {
    const body = await response.text();
    let message = `Erro ${response.status}`;
    if (body) {
      try {
        const parsed = JSON.parse(body) as { message?: unknown };
        if (typeof parsed.message === 'string' && parsed.message.length > 0) {
          message = parsed.message;
        } else {
          message = body;
        }
      } catch {
        message = body;
      }
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function listPlanos(): Promise<Plano[]> {
  return request<Plano[]>('/api/planos');
}

export function listSeguradoras(): Promise<Seguradora[]> {
  return request<Seguradora[]>('/api/seguradoras');
}

export function getSeguradora(id: number): Promise<Seguradora> {
  return request<Seguradora>(`/api/seguradoras/${id}`);
}

export function createSeguradora(nome: string): Promise<Seguradora> {
  return request<Seguradora>('/api/seguradoras', {
    method: 'POST',
    body: JSON.stringify({ nome }),
  });
}

export function listPlanosDaSeguradora(seguradoraId: number): Promise<Plano[]> {
  return request<Plano[]>(`/api/seguradoras/${seguradoraId}/planos`);
}

export function createPlano(seguradoraId: number, input: PlanoInput): Promise<Plano> {
  return request<Plano>(`/api/seguradoras/${seguradoraId}/planos`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updatePlanoLimite(planoId: number, limiteDependentes: number | null): Promise<Plano> {
  return request<Plano>(`/api/planos/${planoId}`, {
    method: 'PUT',
    body: JSON.stringify({ limiteDependentes }),
  });
}

export function saveComissaoPadrao(planoId: number, input: ComissaoInput): Promise<Plano> {
  return request<Plano>(`/api/planos/${planoId}/comissao`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function saveComissaoVendedor(planoId: number, vendedorId: number, input: ComissaoInput): Promise<Plano> {
  return request<Plano>(`/api/planos/${planoId}/comissoes/${vendedorId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function deleteComissaoVendedor(planoId: number, vendedorId: number): Promise<Plano> {
  return request<Plano>(`/api/planos/${planoId}/comissoes/${vendedorId}`, {
    method: 'DELETE',
  });
}

export function listPrecos(planoId: number): Promise<PlanoPreco[]> {
  return request<PlanoPreco[]>(`/api/planos/${planoId}/precos`);
}

export function addPreco(planoId: number, valor: number, renovacao = false): Promise<PlanoPreco> {
  return request<PlanoPreco>(`/api/planos/${planoId}/precos`, {
    method: 'POST',
    body: JSON.stringify({ valor, renovacao }),
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

export function listVendedores(): Promise<Vendedor[]> {
  return request<Vendedor[]>('/api/vendedores');
}

export function createVendedor(input: VendedorInput): Promise<Vendedor> {
  return request<Vendedor>('/api/vendedores', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listContratosByTitular(titularId: number): Promise<Contrato[]> {
  return request<Contrato[]>(`/api/titulares/${titularId}/contratos`);
}

export function getContrato(id: number): Promise<Contrato> {
  return request<Contrato>(`/api/contratos/${id}`);
}

export function createContrato(input: ContratoInput): Promise<Contrato> {
  return request<Contrato>('/api/contratos', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function renewContrato(id: number, input: RenovarContratoInput): Promise<Contrato> {
  return request<Contrato>(`/api/contratos/${id}/renovar`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listDependentes(contratoId: number): Promise<Dependente[]> {
  return request<Dependente[]>(`/api/contratos/${contratoId}/dependentes`);
}

export function createDependente(contratoId: number, input: DependenteInput): Promise<Dependente> {
  return request<Dependente>(`/api/contratos/${contratoId}/dependentes`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updateDependente(
  contratoId: number,
  dependenteId: number,
  input: DependenteInput,
): Promise<Dependente> {
  return request<Dependente>(`/api/contratos/${contratoId}/dependentes/${dependenteId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function deleteDependente(contratoId: number, dependenteId: number): Promise<void> {
  return request<void>(`/api/contratos/${contratoId}/dependentes/${dependenteId}`, {
    method: 'DELETE',
  });
}
