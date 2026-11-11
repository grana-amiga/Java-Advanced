# Grana Amiga — API Java

API de educação e organização financeira da Global Solution 2026/2. Este módulo cuida de cadastro, lançamentos, dívidas, consumo de energia e simuladores de juros e poupança. As trilhas e os desafios educativos ficam na aplicação .NET do grupo.

## Equipe

| Integrante | RM |
| --- | --- |
| Pedro Gonçalves Rodrigues | 567651 |
| Davi Isac Colin Pereira | 567265 |
| Hugo Souza de Jesus | 568542 |
| Matheus Peres Moraes de Jesus | 567300 |
| Cassio Ribeiro | 567295 |

Pitch: a incluir. Contas de demonstração (`USER` e `ADMIN`): a definir.

## Tecnologias

Java 17, Spring Boot 4.1.1, Maven, Spring Data JPA, Spring Security, H2, Caffeine e springdoc OpenAPI. As senhas são protegidas com BCrypt e o acesso às rotas privadas usa JWT.

## Como executar

É necessário ter JDK 17 ou superior e Maven instalados. Na raiz do projeto:

```bash
mvn spring-boot:run
```

A API fica em `http://localhost:8080`. O Swagger fica em `http://localhost:8080/swagger-ui.html`. Para executar localmente, o projeto usa um banco H2 criado na pasta `data/`.

Configure `APP_JWT_SECRET` com pelo menos 32 bytes para manter os tokens válidos entre reinícios. Para criar uma conta ADMIN de demonstração, defina `APP_BOOTSTRAP_ADMIN_PHONE` e `APP_BOOTSTRAP_ADMIN_PASSWORD` antes de iniciar a aplicação. Não há credenciais padrão.

No Windows, `.\scripts\rodar-local.ps1 -Acao run` também encontra o JDK do IntelliJ e o Maven em cache, caso não estejam no `PATH`.

## Acesso e rotas

O cadastro cria uma conta `USER`. O login devolve um JWT; nas rotas protegidas, envie `Authorization: Bearer <token>`. Lançamentos, dívidas e dispositivos são acessíveis apenas ao dono da conta. A consulta de categorias e os simuladores são públicos; alterar categorias exige `ADMIN`.

| Recurso | Rotas |
| --- | --- |
| Cadastro e login | `POST /api/v1/auth/cadastro` e `POST /api/v1/auth/login` |
| Categorias | `/api/v1/categorias` |
| Lançamentos, extrato e resumo mensal | `/api/v1/transacoes` |
| Dívidas e plano de quitação | `/api/v1/dividas` |
| Dispositivos e leituras de energia | `/api/v1/dispositivos` |
| Simuladores de juros e poupança | `POST /api/v1/educacao/simuladores/juros` e `/poupanca` |
| Exclusão da conta | `DELETE /api/v1/usuarios/me` |

O Swagger apresenta os métodos, parâmetros e exemplos de cada rota. Faça o cadastro e o login por lá; depois, use o token no botão **Authorize**. A renda informada no cadastro serve como referência; para entrar no saldo do mês, registre uma transação do tipo `ENTRADA`.

## Consultas e cache

- O extrato busca as transações de um usuário dentro do período informado.
- O resumo mensal usa JPQL para somar valores por categoria. A listagem combina filtros de tipo, categoria, período e valor com paginação.
- O cache Caffeine guarda categorias e resumos mensais por até cinco minutos. Alterações nos dados invalidam as informações afetadas.

## Cadastro e acesso

`POST /api/v1/auth/cadastro` recebe nome, telefone, senha e renda mensal opcional. O telefone é normalizado; a senha fica armazenada como hash. Contas criadas pela API recebem o papel `USER`. `POST /api/v1/auth/login` devolve um JWT válido por duas horas. Envie `Authorization: Bearer <token>` nas rotas protegidas.

Para criar o ADMIN da demonstração, defina `APP_BOOTSTRAP_ADMIN_PHONE` e `APP_BOOTSTRAP_ADMIN_PASSWORD` antes de iniciar. Não há senha padrão. Se o papel de uma conta mudar, os tokens antigos são rejeitados e é necessário entrar novamente.

A API limita transações, dívidas e dispositivos ao dono da conta. Categorias podem ser consultadas sem login; alterações exigem ADMIN. O Actuator expõe `health` e `info` publicamente e `metrics` apenas para ADMIN.

## Consultas e cache

* **Derived query:** extrato por usuário e intervalo de datas.
* **JPQL:** totais mensais por categoria para montar o resumo.
* **Specification:** combina tipo, categoria, período e faixa de valor; listagens usam paginação e ordenação.
* **Cache:** Caffeine guarda categorias e resumos mensais. Alterações em categorias, lançamentos e exclusão de conta invalidam as entradas afetadas.