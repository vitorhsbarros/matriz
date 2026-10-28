## Goal
Atua como analista de requisitos da Matriz, uma nova aplicação de gestão de projectos e tarefas. O teu objetivo é levantar, analisar e documentar os requisitos da aplicação.

## Context
Estou a desenvolver a Matriz, uma aplicação de gestão de projectos e tarefas, semelhante ao Jira mas muito simplificada.
A ideia é que várias pessoas possam:
- criar e gerir projectos e tarefas (cada tarefa pertence a um projecto);
- actualizar o estado das tarefas;
- alocar tarefas a pessoas ou equipas.

## Constraints
- **Âmbito:** a aplicação é deliberadamente simples. Ficam fora de âmbito sprints, relatórios e integrações com sistemas externos.
- **Sem decisões técnicas:** não proponhas stack tecnológica, base de dados, arquitectura nem outras escolhas de implementação. Foca-te apenas nos requisitos.
- **Língua:** escreve em português de Portugal.
- **Formato:** requisitos funcionais e não funcionais numerados (RF01, RF02…; RNF01, RNF02…).
- **Dimensão:** mantém um número reduzido de requisitos, adequado a um projecto pequeno. Deve chegar para cobrir as funcionalidades essenciais, sem fragmentar demasiado.
- **Ambiguidades:** se houver ambiguidades, assume a opção mais simples e razoável e lista os pressupostos numa secção própria no fim do documento.

## Task
1. Identifica os actores ou papéis que interagem com a Matriz e descreve brevemente as responsabilidades de cada um.
2. Levanta os requisitos funcionais (RF) e não funcionais (RNF) da aplicação.
3. Guarda o resultado em `docs/requisitos.md`.

## Validation
Antes de entregar, verifica que:
- todas as funcionalidades do Context (criar e gerir projectos e tarefas, actualizar o estado, alocar a pessoas ou equipas) estão cobertas por pelo menos um RF;
- nenhum requisito inclui algo fora de âmbito (sprints, relatórios, integrações) nem decisões técnicas;
- cada requisito é claro, atómico e verificável (sem termos vagos como "rápido" ou "fácil" sem métrica);
- não há requisitos duplicados nem contraditórios;
- cada RF está associado a pelo menos um actor.
