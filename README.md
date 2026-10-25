# Corretors

API Spring Boot e aplicativo React Native para o domínio da corretora: Titular (cliente), Dependente, Vendedor, Usuario, Contato, Endereco, Plano, PlanoHistorico, Pagamento, Parcela e Contrato.

O app de 2016 era um WAR JSF/PrimeFaces em Maven e apontava para MySQL `fj21` com a senha de `root` vazia. Esse banco não sobe com o projeto. A API agora usa H2 em memória: `./gradlew bootRun` não precisa de MySQL.

## Versões

Conferidas em 25 de outubro de 2026.

| Peça | Versão |
| --- | --- |
| Spring Boot | 4.1.2 (estável; 4.2.0-RC1 não entra) |
| Java | 21 (toolchain). O Spring Boot 4.1.2 exige Java 17 ou superior e aceita até Java 27 |
| Gradle | 9.8.2, o wrapper que o Spring Initializr gera para o Boot 4.1.2 |
| Hibernate ORM | 7.4.13.Final |
| Spring Data JPA | 4.1.2 |
| Tomcat | 11.0.27 |
| Jackson | 3.1.7 |
| H2 | 2.4.240 |
| Node.js | 22.13 ou superior (neste ambiente, 22.23.4). Exigência do React Native 0.88 / Expo SDK 58 |
| React Native | 0.88.0 |
| React | 19.3.0 |
| Expo | 58.0.19 (SDK 58, toolchain do React Native 0.88) |
| expo-status-bar | 58.0.4 |
| TypeScript | 6.0.3 |
| @types/react | 19.3.0 |

Expo entra só como toolchain (`expo start` e `expo export`). A interface é React Native (`View`, `Text`, `TextInput`, `Pressable`), não um site.

## API

Na raiz do repositório:

```bash
./gradlew bootRun
```

Sobe em `http://localhost:8080` com o banco em memória `jdbc:h2:mem:corretors` (usuário `sa`, senha vazia). O console H2 fica em `http://localhost:8080/h2-console`.

Para gravar em arquivo, em vez de memória:

```bash
./gradlew bootRun --args='--spring.profiles.active=file'
```

O arquivo fica em `./data/corretors`. Esse diretório não entra no git.

Testes, inclusive a carga do contexto Spring:

```bash
./gradlew test
```

### Endpoints

| Método | Caminho | Uso |
| --- | --- | --- |
| GET | `/api/planos` | lista planos (`id`, `nome`, `descricao`) |
| POST | `/api/planos` | cria plano. `nome` é obrigatório |
| GET | `/api/titulares` | lista titulares, com Contato e Endereco |
| POST | `/api/titulares` | cria titular. `nome` é obrigatório |

Exemplo:

```bash
curl -s -X POST http://localhost:8080/api/planos \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Plano Essencial","descricao":"Cobertura basica"}'

curl -s -X POST http://localhost:8080/api/titulares \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Ana Souza","cpf":"12345678901","contato":{"celular":"31999998888","endereco":{"cidade":"Belo Horizonte"}}}'
```

As demais entidades (Dependente, Vendedor, Usuario, PlanoHistorico, Pagamento, Parcela, Contrato) estão mapeadas e são persistidas nos testes. `PagamentoType` continua gravado como `DINH`, `CHEQ`, `DEBI` e `CRED`. Datas que eram Joda `DateTime` passaram para `java.time` (`LocalDate` em `dtNasc`, `LocalDateTime` nas demais).

## Aplicativo

```bash
cd mobile
npm ci
npm start
```

`npm start` sobe o Metro (`expo start`) na porta 8081. Abra o projeto no Expo Go ou em um emulador. No emulador Android a API no host é `http://10.0.2.2:8080`:

```bash
EXPO_PUBLIC_API_URL=http://10.0.2.2:8080 npm start
```

No simulador iOS, `http://localhost:8080` serve. Em um aparelho físico, use o IP da máquina na rede.

A tela lista e cria planos e titulares. Ao abrir, chama `GET /api/planos` e `GET /api/titulares`. Os botões chamam os `POST`.

Sem emulador, o cliente HTTP da mesma tela pode falar com a API que já está no ar:

```bash
cd mobile
npm run call-api
```

Checagens que não exigem Android nem iOS:

```bash
cd mobile
npm run typecheck
npm run bundle:android
```

`bundle:android` gera o bundle Hermes em `mobile/dist` (não versionado). As pastas nativas `android/` e `ios/` não estão no repositório; o Expo as gera com `npx expo prebuild` quando houver SDK Android ou Xcode.

## O que este ambiente executou

- `./gradlew test` — passou, com `CorretorsApplicationTests.contextLoads`.
- `./gradlew bootRun` — a API subiu em cerca de 2,3 s na porta 8080, H2 em memória.
- `npm run typecheck` no app.
- `npm run bundle:android` — bundle Android Hermes, com `/api/planos` e `/api/titulares`.
- `npm run call-api` contra a API no ar — listou e criou um plano e um titular.
- `npm start` — Metro na porta 8081.

Não rodou build nativo Android nem iOS: a VM não tem Android SDK (`adb` ausente) nem Xcode.
