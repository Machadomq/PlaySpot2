# PlaySpot

Marketplace de reserva de quadras esportivas.

- Back-end: Java 25, Spring Boot 4, PostgreSQL, Flyway
- Front-end: React, TypeScript, Vite
- Documentação de produto: [docs/](docs/)

## Pré-requisitos

- JDK 25
- Node.js 24
- Docker Desktop em execução

## Como rodar

Em três terminais, a partir da raiz do repositório:

```
docker compose up -d
```

```
cd backend
./mvnw spring-boot:run
```

```
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173. O Vite repassa as chamadas `/api` para o back-end na porta 8080.

## Testes

```
cd backend
./mvnw test
```

Os testes sobem um PostgreSQL próprio em Docker e não tocam no banco de desenvolvimento.

```
cd frontend
npm run lint
npm run build
```

## Fluxo de trabalho

O trabalho acontece na branch `gabriel` e entra na `Main` por pull request. O GitHub Actions roda os testes, o lint e o build em cada PR.

Depois de cada merge:

```
git switch Main
git pull
git switch gabriel
git merge Main
```

## Migrações de banco

Ficam em `backend/src/main/resources/db/migration`. Uma migração já aplicada nunca é editada: cada mudança é um novo arquivo `V<número>__descricao.sql`.
