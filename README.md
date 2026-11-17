# Grana Amiga — API Java

API de educação e organização financeira desenvolvida para a Global Solution 2026/2. Este módulo cuida de contas, lançamentos, dívidas, consumo de energia e simuladores de juros e poupança. As trilhas e os desafios educativos ficam na aplicação .NET do grupo.

## Equipe

| Integrante | RM |
| --- | --- |
| Pedro Gonçalves Rodrigues | 567651 |
| Davi Isac Colin Pereira | 567265 |
| Hugo Souza de Jesus | 568542 |
| Matheus Peres Moraes de Jesus | 567300 |
| Cassio Ribeiro | 567295 |

Pitch: **adicionar o link do vídeo**.

## Tecnologias e organização

Java 25, Spring Boot 4.1.1, Maven Wrapper, Spring Data JPA, Spring Security, Bean Validation, H2, Oracle Database, Caffeine, HATEOAS, Actuator e Swagger/OpenAPI. A execução local usa H2 em arquivo; o Docker/Render usa Oracle; os testes automatizados usam H2 em memória.

A aplicação segue o fluxo `controller → service → repository`. O pacote `model` agrupa entidades e records por funcionalidade (`usuario`, `categoria`, `transacao`, `divida`, `energia` e `educacao`); `security` contém a configuração do JWT; `config` reúne as configurações e os dados iniciais; `exception` trata os erros da API. As senhas são armazenadas com BCrypt.

## Modelo de dados

```mermaid
erDiagram
    USUARIO {
        long id_usuario PK
        string nome
        string telefone
        string papel
        decimal renda_mensal
    }
    CATEGORIA {
        long id_categoria PK
        string nome
        string icone
        string tipo
    }
    TRANSACAO {
        long id_transacao PK
        long id_usuario FK
        long id_categoria FK
        decimal valor
        string tipo
        datetime dt_transacao
    }
    DIVIDA {
        long id_divida PK
        long id_usuario FK
        decimal saldo_devedor
        decimal taxa_juros_mes
        decimal valor_parcela
        string status
    }
    DISPOSITIVO {
        long id_dispositivo PK
        long id_usuario FK
        string codigo
        decimal tarifa_kwh
        decimal meta_mensal_reais
    }
    LEITURA_ENERGIA {
        long id_leitura PK
        long id_dispositivo FK
        datetime dt_leitura
        decimal kwh
        decimal custo_estimado
    }
    USUARIO ||--o{ TRANSACAO : registra
    CATEGORIA ||--o{ TRANSACAO : classifica
    USUARIO ||--o{ DIVIDA : possui
    USUARIO ||--o{ DISPOSITIVO : cadastra
    DISPOSITIVO ||--o{ LEITURA_ENERGIA : recebe
```

Na execução local, o H2 cria as tabelas e sequências das seis entidades. No Oracle, a API valida as estruturas já existentes. As associações são carregadas sob demanda (`LAZY`). Ao excluir uma conta, o serviço remove seus registros dependentes antes de excluir o usuário, sem exclusão em cascata automática.

## Como executar

Instale o JDK 25, configure `JAVA_HOME` e abra o terminal na raiz do projeto (a pasta que contém `pom.xml` e `mvnw.cmd`). Confira se `java -version` mostra a versão 25. Sem perfil Spring ativo, a API usa H2 em arquivo e não precisa de banco externo.

Windows / PowerShell:

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
$env:APP_BOOTSTRAP_ADMIN_PHONE = "11000000002"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = "DemoAdmin@GS2026"
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
unset SPRING_PROFILES_ACTIVE
APP_BOOTSTRAP_ADMIN_PHONE=11000000002 APP_BOOTSTRAP_ADMIN_PASSWORD=DemoAdmin@GS2026 sh mvnw spring-boot:run
```

Na primeira execução, o Maven Wrapper baixa o Maven e as dependências, se necessário. Aguarde a mensagem `Started GranaAmigaApplication` e abra `http://localhost:8080/swagger-ui.html`. Pare a API com `Ctrl+C`.

Se a política do Windows bloquear `mvnw.cmd`, instale o Maven 3.9.16 e execute `mvn spring-boot:run` no mesmo terminal, com as variáveis de ambiente definidas acima.

O H2 cria as tabelas e grava os dados em `data/grana-amiga.mv.db`, dentro da pasta do projeto. Os dados permanecem após reiniciar a API; a pasta `data` não vai para o Git. As dez categorias iniciais são cadastradas automaticamente. Os comandos acima também criam o `ADMIN` de teste quando ele ainda não existe.

O H2 local e o Oracle online são bancos separados: contas e lançamentos criados em um deles não aparecem automaticamente no outro.

O Maven Wrapper usa a versão 3.9.16 do Maven. O endereço `http://localhost:8080` redireciona para o Swagger. A variável `APP_JWT_SECRET` é opcional no computador; sem ela, os tokens deixam de valer após reiniciar a API e basta fazer login novamente. Para manter os tokens válidos entre reinícios, defina um segredo com pelo menos 32 bytes.

## Publicação no Render

No serviço existente do Render, mantenha o tipo **Docker**. O `Dockerfile` compila a API com Java 25 e ativa o perfil `oracle`. **Antes do próximo deploy**, confirme `SPRING_PROFILES_ACTIVE=oracle` no painel **Environment** e configure `DB_URL` (URL JDBC completa), `DB_USERNAME`, `DB_PASSWORD` e `APP_JWT_SECRET` (pelo menos 32 bytes). A URL do banco não está no código; se `DB_URL` faltar, a API não inicia. Guarde os valores reais somente no ambiente do Render. Para Flutter Web, configure também `APP_CORS_ORIGINS` com a origem HTTPS do site; no aplicativo mobile nativo, CORS não é necessário.

Defina **Health Check Path** como `/actuator/health`. A API usa a porta `PORT` fornecida pelo Render e continua na porta 8080 quando executada localmente. O esquema Oracle deve conter as tabelas e sequências da aplicação, pois ele é validado na inicialização. A instância publicada está em [grana-amiga-api.onrender.com](https://grana-amiga-api.onrender.com/), endereço-base para as chamadas do Flutter.

## Usuários de teste

As contas abaixo são fictícias e servem para demonstrar os dois papéis da API. As senhas são apenas do aplicativo, não do banco de dados.

| Papel | Telefone | Senha de teste | Demonstração |
| --- | --- | --- | --- |
| `USER` | `11000000001` | `DemoUser@GS2026` | Registrar lançamentos, dívidas, dispositivos e leituras de energia |
| `ADMIN` | `11000000002` | `DemoAdmin@GS2026` | Manutenção de categorias e consulta de `/actuator/metrics` |

No H2 novo, os comandos de inicialização criam o `ADMIN`. Cadastre o `USER` uma vez pelo Swagger em `POST /api/v1/auth/cadastro` com este JSON:

```json
{
  "nome": "Usuário de teste",
  "telefone": "11000000001",
  "senha": "DemoUser@GS2026",
  "rendaMensal": 2000.00
}
```

Nas próximas execuções, pule o cadastro e faça login: as contas continuam no arquivo H2. No Oracle da demonstração, use as contas da tabela se já estiverem cadastradas; em um esquema novo, cadastre o `USER` pelo mesmo endpoint e configure `APP_BOOTSTRAP_ADMIN_PHONE` e `APP_BOOTSTRAP_ADMIN_PASSWORD` no ambiente do Render para criar o `ADMIN` ao iniciar. O cadastro público cria somente `USER`.

## Testar pelo Swagger

1. Em um H2 novo, cadastre o `USER` com o JSON acima. Se o telefone já estiver cadastrado, siga direto para o login.
2. Abra `POST /api/v1/auth/login`, entre com a conta `USER` e copie o token retornado.
3. Selecione **Authorize** no Swagger e informe o token. Consulte `GET /api/v1/transacoes`, `GET /api/v1/dividas` e `GET /api/v1/dispositivos`. No H2 local, as listas começam vazias.
4. Consulte `GET /api/v1/categorias` e escolha o ID de uma categoria do tipo `SAIDA`. Registre um gasto em `POST /api/v1/transacoes` e consulte `GET /api/v1/transacoes/resumo` com o ano e o mês atuais.
5. Faça login com a conta `ADMIN` e substitua o token em **Authorize**. Em `GET /api/v1/categorias/{id}`, o retorno passa a incluir links `atualizar` e `excluir`. Para testar uma operação restrita no Swagger, use `POST /api/v1/categorias` com um nome novo, um ícone e o tipo `SAIDA`.

A renda mensal é uma referência para planejamento; ela só entra no saldo quando registrada como transação do tipo `ENTRADA`. Para mostrar HATEOAS, consulte uma transação e observe os links `self`, `categoria`, `atualizar` e `excluir`. O resumo mensal usa cache: depois de adicionar, editar ou excluir uma transação, a consulta seguinte reflete a alteração. `GET /actuator/health` mostra a saúde da API.

## Endpoints

| Método | Caminho | Função |
| --- | --- | --- |
| POST | `/api/v1/auth/cadastro` | Criar conta `USER` |
| POST | `/api/v1/auth/login` | Receber JWT |
| GET | `/api/v1/categorias`, `/api/v1/categorias/{id}` | Consultar categorias sem login |
| POST | `/api/v1/categorias` | Criar categoria como `ADMIN` |
| PUT, DELETE | `/api/v1/categorias/{id}` | Atualizar ou excluir categoria como `ADMIN` |
| GET, POST | `/api/v1/transacoes` | Listar com filtros ou registrar lançamento |
| GET, PUT, DELETE | `/api/v1/transacoes/{id}` | Consultar, atualizar ou excluir lançamento |
| GET | `/api/v1/transacoes/extrato`, `/api/v1/transacoes/resumo` | Consultar extrato e resumo mensal |
| GET, POST | `/api/v1/dividas` | Listar ou cadastrar dívidas |
| GET, PUT, DELETE | `/api/v1/dividas/{id}` | Consultar, atualizar ou excluir dívida |
| GET | `/api/v1/dividas/{id}/plano` | Calcular plano de quitação |
| GET | `/api/v1/dividas/termometro` | Ver o comprometimento da renda com parcelas abertas |
| GET | `/api/v1/dividas/alertas` | Consultar próximos vencimentos; aceita `dataReferencia` e `dias` |
| GET, POST | `/api/v1/dispositivos` | Listar ou cadastrar dispositivos |
| GET, PUT, DELETE | `/api/v1/dispositivos/{id}` | Consultar, atualizar ou excluir dispositivo |
| GET, POST | `/api/v1/dispositivos/{id}/leituras` | Listar ou registrar leituras |
| GET | `/api/v1/dispositivos/{id}/leituras/{leituraId}` | Consultar leitura |
| GET | `/api/v1/dispositivos/{id}/resumo` | Consultar consumo e custo mensal |
| POST | `/api/v1/educacao/simuladores/juros`, `/api/v1/educacao/simuladores/poupanca` | Simular juros ou poupança sem login |
| GET | `/api/v1/usuarios/me` | Consultar perfil da conta autenticada |
| DELETE | `/api/v1/usuarios/me` | Excluir a conta e seus dados |

As rotas privadas usam `Authorization: Bearer <token>`. Lançamentos, dívidas e dispositivos são limitados ao dono da conta. O Actuator disponibiliza `health` e `info` publicamente; `metrics` exige `ADMIN`.

O termômetro divide a soma das parcelas de dívidas abertas pela renda mensal: até 30% é verde, até 50% é amarelo e acima disso é vermelho. Sem renda informada, a faixa é `SEM_RENDA`. Os alertas consideram os próximos 15 dias por padrão e ajustam vencimentos em dias 29 a 31 ao último dia de meses mais curtos.

## Consultas e cache

`DividaRepository.findByUsuarioId` é uma consulta derivada suficiente para listar as dívidas de uma conta. O extrato usa JPQL com intervalo do início do mês até o início do mês seguinte, sem perder lançamentos no último dia, e pagina o resultado. O resumo usa JPQL com `GROUP BY` para somar valores por categoria no próprio banco, evitando carregar todos os lançamentos na aplicação.

A listagem de lançamentos usa `Specification` porque os filtros de tipo, categoria, datas e valor são opcionais e podem aparecer em qualquer combinação. Toda consulta inclui o ID da conta autenticada; paginação e ordenação limitam o volume retornado quando o histórico cresce.

O Caffeine guarda o catálogo de categorias, lido com frequência e alterado raramente, e o resumo mensal, que repete agregações por conta e período. A chave do resumo inclui usuário, ano e mês. Os dados expiram após cinco minutos; `@CacheEvict` também limpa os caches quando categorias ou transações mudam, para que as próximas consultas reflitam as alterações. Lançamentos individuais não são colocados em cache porque mudam com mais frequência.

## Uso de IA generativa

ChatGPT e Codex foram utilizados como apoio na análise do enunciado, implementação, revisão de código, testes e documentação. Os resultados foram conferidos com o PDF da disciplina, a suíte automatizada e chamadas da API.

## Gerar o JAR

Windows / PowerShell: `.\mvnw.cmd package`. Linux / macOS: `sh mvnw package`.

O arquivo gerado fica em `target/grana-amiga-api-1.0.0.jar` e pode ser executado com `java -jar target/grana-amiga-api-1.0.0.jar`.

## Testes

Execute `.\mvnw.cmd test` no Windows ou `sh mvnw test` no Linux/macOS. A suíte usa H2 em memória e não modifica o H2 em arquivo nem o Oracle.

