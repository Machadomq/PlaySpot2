# PlaySpot: Plano do app

> Última atualização: 2026-11-08
> Cada fase termina com testes passando e um PR de `gabriel` para `Main`.

## Decisões de produto

| Tema | Decisão |
|---|---|
| Pagamento no lançamento | No local. O dono registra o recebimento na agenda. |
| Receita | Grátis no início; a forma de cobrança será decidida com dados de uso. |
| Confirmação da reserva | Automática, se o horário estiver livre. |
| Cancelamento | Cada empresa define a antecedência mínima. Depois do prazo, o jogador fala com o local. |
| Cadastro da empresa | Depois do registro do dono, numa tela "Minha empresa". Uma empresa por conta. |
| Antecedência | O jogador reserva com até 60 dias de antecedência. |
| Busca | Por cidade ou por proximidade, com mapa OpenStreetMap. |
| Notificações | Sem serviço de envio por enquanto. Sem e-mail, o administrador gera uma senha temporária para quem perdeu o acesso. |
| Fotos | Até 5 por quadra no lançamento. Pasta local em desenvolvimento; provedor definido na Fase 7. |

## Já pronto

- Banco PostgreSQL via Docker e migração V1
- Cadastro, login com JWT e rota `/api/me`
- Telas de cadastro, login e conta

## Ajustes necessários no modelo de dados (migração V2)

| Ajuste | Motivo |
|---|---|
| Prazo de cancelamento na empresa | Decisão de produto acima |
| Fuso horário na empresa (padrão `America/Sao_Paulo`) | Converter a faixa de horário local em data e hora reais |
| `usuario_id` opcional na reserva e campo `cliente_nome` | O dono precisa lançar na agenda clientes que reservaram por telefone ou WhatsApp |
| Status `FALTOU` na reserva; `PENDENTE` deixa de ser usado no lançamento | Medir faltas; a confirmação é automática |
| Impedir faixas sobrepostas na mesma quadra e dia | Hoje duas faixas poderiam dar preços conflitantes |
| Permitir faixa que termina à meia-noite | A regra atual `hora_fim > hora_inicio` impede 18h às 00h |

## Fases

### Fase 0: Base do projeto
- Publicar os commits atuais (PR #2)
- CI no GitHub Actions: testes do back-end, build e lint do front
- Testes em banco separado do de desenvolvimento (Testcontainers)
- Formato único de erro da API, com mensagens em português
- README com instruções para rodar o projeto
- Layout comum do front (cabeçalho e navegação por perfil)

### Fase 1: Empresa e quadras (dono)
- Migração V2
- Criar e editar empresa
- Criar, editar e desativar quadras, escolhendo esportes
- Lista inicial de esportes
- Regra de acesso: o dono só altera os próprios dados

### Fase 2: Horários e preços (dono)
- Tela semanal de faixas de horário e preço por quadra
- Copiar a configuração de um dia para outros
- Cálculo dos horários livres: funcionamento menos reservas

### Fase 3: Busca e reserva (jogador)
- Busca por cidade, esporte e data, com comparação de preços
- Página da empresa com quadras e horários livres
- Reserva com confirmação automática e preço calculado por faixa
- "Minhas reservas" e cancelamento dentro do prazo
- Tratamento de duas pessoas reservando o mesmo horário ao mesmo tempo

### Fase 4: Agenda e financeiro (dono)
- Agenda diária e semanal por quadra
- Reserva manual e bloqueio de horário
- Marcar como pago, faltou ou cancelada
- Resumo financeiro: recebido, a receber e faltas por período

### Fase 5: Conta e avisos (adiada, depende de um serviço de envio)
- E-mail de confirmação e de cancelamento da reserva
- Recuperação de senha
- Confirmação de e-mail

### Fase 6: Administração
- Painel do admin: cadastro de esportes
- Aprovação de empresas antes de aparecerem na busca
- Métricas básicas: reservas, empresas ativas, faltas

### Fase 7: Produção
- Hospedagem, domínio e HTTPS
- Provedor de armazenamento das fotos
- Provedor de mapas e de busca de endereço com volume compatível (os servidores públicos do OpenStreetMap têm limite de uso)
- Busca feita no banco, em vez de carregar todas as empresas na memória
- Segredos fora do código (`JWT_SECRET` e banco)
- Revisão do armazenamento do token (cookie `HttpOnly`)
- Limite de tentativas de login
- Backup do banco, logs e monitoramento de erros
- LGPD: termos de uso, política de privacidade, exclusão de conta
- Revisão de segurança antes de abrir ao público

### Fase 8: Piloto
- Entrevistar donos de quadra e revisar `docs/01-visao-e-escopo.md`
- Colocar de 3 a 5 quadras reais em uso
- Corrigir o que o uso real mostrar

## Depois do lançamento

- Pix e cartão online, com repasse aos donos
- Cobrança dos donos
- Avaliações
- Preço dinâmico e relatórios avançados
- Mensalistas
- App instalável (PWA)

## Perguntas que ficam para a fase em que importarem

| Fase | Pergunta |
|---|---|
| 5 | Quando retomar: avisos só por e-mail ou também por WhatsApp (custo por mensagem)? |
| 7 | Qual o orçamento mensal de hospedagem? |
