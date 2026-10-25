import { createPlano, createTitular, listPlanos, listTitulares } from '../src/api.ts';

const stamp = Date.now();

const plano = await createPlano({
  nome: `Plano ${stamp}`,
  descricao: 'criado pelo app',
});
const planos = await listPlanos();
if (!planos.some((item) => item.id === plano.id)) {
  throw new Error('plano criado nao apareceu na lista');
}

const titular = await createTitular({
  nome: `Titular ${stamp}`,
  cpf: '12345678901',
  rg: 'MG123',
  dtNasc: '1990-01-15',
  cartaoEnv: false,
  seguroVidaEnv: true,
  contato: {
    telFixo: '3133334444',
    celular: '31999998888',
    email: 'titular@example.com',
    endereco: {
      pais: 'Brasil',
      estado: 'MG',
      cidade: 'Belo Horizonte',
      rua: 'Afonso Pena',
      numero: '100',
    },
  },
});
const titulares = await listTitulares();
if (!titulares.some((item) => item.id === titular.id)) {
  throw new Error('titular criado nao apareceu na lista');
}

console.log(
  `ok planos=${planos.length} novoPlano=${plano.id} nomePlano=${plano.nome} titulares=${titulares.length} novoTitular=${titular.id} nomeTitular=${titular.nome} cidade=${titular.contato?.endereco?.cidade}`,
);
