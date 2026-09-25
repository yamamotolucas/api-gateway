<div align="center">

# 🌐 API Gateway (`api-gateway`)

<p align="center">
  <b>Porta de entrada única e Edge Service reativo da plataforma, centralizando roteamento dinâmico, segurança perimetral, validação de tokens JWT e enriquecimento de contexto downstream via headers HTTP.</b>
</p>

<!-- Badges -->
<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.10-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot 3.3.10" />
  <img src="https://img.shields.io/badge/Spring_Cloud-2023.0.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Cloud 2023" />
  <img src="https://img.shields.io/badge/Spring_Cloud_Gateway-Reactive_WebFlux-blue?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Cloud Gateway" />
  <img src="https://img.shields.io/badge/Netflix_Eureka-Discovery_Client-brightgreen?style=for-the-badge&logo=spring&logoColor=white" alt="Eureka Client" />
  <img src="https://img.shields.io/badge/Auth0_JWT-HMAC256-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/Swagger_UI-Springdoc_WebFlux_2.6.0-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger WebFlux" />
</p>

</div>

---

## 📌 Visão Geral

O **`api-gateway`** atua como o **Edge Service / Single Entry Point** de todo o ecossistema distribuído da barbearia. Construído sobre o motor reativo e assíncrono do **Spring Cloud Gateway (WebFlux / Netty)**, ele é responsável por interceptar 100% do tráfego externo proveniente do cliente frontend (Next.js) ou aplicações parceiras.

Além de gerenciar o roteamento dinâmico integrado ao **Netflix Eureka**, o gateway assume o papel crítico de **guardião de segurança perimetral**: valida a autenticidade e a expiração do token JWT na borda e propaga a identidade do usuário diretamente para os microsserviços internos através de headers HTTP customizados.

---

## 🔐 Destaque de Arquitetura: Segurança Perimetral & Enriquecimento de Contexto

Um dos diferenciais técnicos mais expressivos deste gateway é a sua **esteira de autenticação stateless reativa**, eliminando a necessidade de cada microsserviço interno reimplementar a decodificação de criptografia do token.

```text
                     [ CLIENTE / FRONTEND ]
                               │
               (Requisição com Bearer Token)
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Borda)                       │
│                                                              │
│  1. RouterValidator (Predicate reativo)                      │
│     └── Rota pública? ──(SIM)──▶ Passa direto                │
│     └── Rota protegida? ─(NÃO)─▶ 2. AuthenticationFilter     │
│                                      │                       │
│  2. JwtService (HMAC-256 + Issuer 'identity-ms')             │
│     └── Token inválido/expirado? ──▶ Retorna 401 Unauthorized│
│     └── Token válido?                                        │
│          ├── Extrai: id, subject (email), name, roles        │
│          └── Enriquecimento (request.mutate()):              │
│              • X-User-Id: 42                                 │
│              • X-User-Email: dev@barbearia.com               │
│              • X-User-Name: Lucas Yamamoto                   │
│              • X-User-Roles: ROLE_CLIENTE,ROLE_ADMIN         │
└──────────────────────────────┬───────────────────────────────┘
                               │
               (Requisição enriquecida com Headers)
                               │
                               ▼
                 [ MICROSSERVIÇOS INTERNOS ]
        (booking-ms / identity-ms lêem direto os headers
         sem precisar validar chave criptográfica novamente)
```

### Componentes Chave da Segurança:

1. **`RouterValidator` (Whitelist Inteligente):**
    - Utiliza um `Predicate<ServerHttpRequest>` funcional para identificar rotas públicas e endpoints abertos de documentação (como OpenAPI specs `/v3/api-docs`, rotas de cadastro/login e listagem pública de serviços).
    - Qualquer requisição fora dessa lista é compulsoriamente interceptada para validação de identidade.

2. **`AuthenticationFilter` (GlobalFilter Reativo - Order `-1`):**
    - Executa na mais alta prioridade do ciclo de filtros do Netty.
    - Extrai o cabeçalho `Authorization: Bearer <token>`, valida a presença e sintaxe do token.
    - Se inválido ou expirado, encerra a requisição imediatamente devolvendo um payload JSON padronizado com status **`401 Unauthorized`**, aliviando o tráfego nos microsserviços internos.

3. **`JwtService` & Injeção de Contexto Downstream:**
    - Valida a integridade da assinatura usando `HMAC256` e confirma o emissor com `.withIssuer("identity-ms")`.
    - Decodifica as claims (`id`, `subject`, `name`, `roles`) e as injeta na requisição HTTP reativa através dos cabeçalhos:
        - `X-User-Id`
        - `X-User-Email`
        - `X-User-Name`
        - `X-User-Roles`
    - **Benefício de Engenharia:** Os serviços internos (como o `booking-ms`) podem simplesmente ler `@RequestHeader("X-User-Id")` nos seus Controllers sem carregar o overhead de decodificar tokens ou bater no banco repetidamente.

---

## 🏗️ Estrutura de Pastas

A estrutura segue com rigor a organização do projeto em pacotes semânticos:

```text
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/br/com/api_gateway/
│   │   │   ├── config/
│   │   │   │   └── RouterValidator.java     # Predicate de rotas públicas vs protegidas
│   │   │   ├── filter/
│   │   │   │   └── AuthenticationFilter.java # GlobalFilter (Auth & Header Mutation)
│   │   │   ├── service/
│   │   │   │   └── JwtService.java          # Decodificação e verificação Auth0 JWT
│   │   │   └── ApiGatewayApplication.java   # Entrypoint da aplicação Spring Boot
│   │   └── resources/
│   │       └── application.properties       # Configurações de rotas, Eureka e JWT
│   └── test/
│       └── java/br/com/api_gateway/
├── pom.xml
└── Dockerfile
```

---

## ⚙️ Variáveis de Ambiente Esperadas

As configurações gerenciadas no `application.properties` aceitam as seguintes variáveis de ambiente:

| Variável | Descrição | Valor Padrão / Exemplo |
| :--- | :--- | :--- |
| `SERVER_PORT` | Porta de entrada externa do Gateway | `8080` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL do servidor Eureka para descoberta dinâmica de serviços | `http://localhost:8081/eureka` |
| `JWT_SECRET` | Chave secreta de assinatura HMAC-SHA256 (compartilhada com `identity-ms`) | `SUA_CHAVE_JWT_SECRETA_LOCAL_COM_NO_MINIMO_32_CARACTERES` |
| `FRONTEND_URL` | Origem autorizada no CORS para requisições do cliente web | `http://localhost:3000` |

---

## 🔌 Endpoints da API

> 💡 A documentação interativa com Swagger UI pode ser acessada em:  
> `http://localhost:8080/webjars/swagger-ui/index.html` (via API Gateway)

---

## ⚠️ Observações de Execução no Ecossistema

> **Atenção:** Este microsserviço integra uma arquitetura distribuída. Para o seu funcionamento pleno (comunicação entre serviços, descoberta de rotas e persistência de dados), é indispensável que o **`eureka-server`**, uma instância ativa do **MySQL** e os demais microsserviços dependentes estejam em execução.
>
> Para executar ou clonar o ecossistema completo com todas as dependências e submódulos configurados, acesse o repositório central:  
> 🔗 [**barbearia-network-main**](https://github.com/yamamotolucas/barbearia-network-main)


