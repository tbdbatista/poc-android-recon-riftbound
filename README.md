# Riftbound Recon — Mobile Edge-Vision & Collection System

> **Trabalho de Conclusão de Curso (TCC)**  
> **Curso:** Especialização em Desenvolvimento de Aplicativos Mobile  
> **Instituição:** Pontifícia Universidade Católica do Paraná (PUC-PR)  
> **Autor:** Thiago Batista  
> **Versão do Projeto:** 0.1.0 (ver `version.properties`)

---

## 1. Visão Geral do Projeto

O **Riftbound Recon** é uma Prova de Conceito (PoC) de alta fidelidade desenvolvida para dispositivos Android, voltada para o reconhecimento automatizado, catalogação e auditoria em tempo real de cartas físicas de *Trading Card Games* (TCG).

### 1.1 Contexto e Justificativa do Problema
A catalogação manual de acervos e coleções físicas de jogos de cartas colecionáveis é um processo historicamente lento, propenso a erros de digitação e ineficiente para jogadores competitivos, colecionadores e lojistas. Embora existam soluções de mercado para jogos tradicionais (e.g., Magic: The Gathering, Pokémon), a maioria depende exclusivamente de:
1. Conexão ininterrupta com a nuvem (APIs de visão computacional hospedadas em servidores remotos), gerando latência perceptível e impossibilitando o uso em feiras, torneios ou locais com conectividade instável.
2. Identificação baseada unicamente em *hashes* visuais de arte que falham diante de variantes holográficas (*foil*), impressões alternativas ou oclusões parciais causadas pelo manuseio.

### 1.2 Solução Proposta
O Riftbound Recon introduz uma arquitetura de **Computação de Borda (Edge AI)** no dispositivo móvel. Toda a cadeia de processamento — desde a captura de frames, extração de texto por Reconhecimento Óptico de Caracteres (OCR), inferência geométrica e correlação heurística — é executada **100% offline no dispositivo**, garantindo:
* Resposta de reconhecimento em milissegundos.
* Privacidade integral e autonomia operacional em ambientes sem sinal de internet.
* Baixo consumo de largura de banda e resiliência a variações físicas de manuseio.

---

## 2. Arquitetura de Software

A aplicação foi projetada seguindo rigorosamente os princípios da **Clean Architecture**, combinada com o padrão de apresentação **MVVM (Model-View-ViewModel)** e fluxo unidirecional de dados (**Unidirectional Data Flow - UDF**).

```
┌────────────────────────────────────────────────────────────────────────┐
│                        UI Layer (Presentation)                         │
│   Jetpack Compose (Material 3)  ◄──►  Navigation Compose               │
│                                 ▲                                      │
│                                 │ (StateFlow / UDF Events)             │
│                        MainViewModel                                   │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
                                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             Domain Layer                               │
│   • Domain Models (Card, Collection, CollectionCard)                   │
│   • Contracts / Interfaces (CardRepository)                            │
│   • Domain Services (CardScannerMatcher - Pure Kotlin Heuristics)     │
└─────────────────────────────────▲──────────────────────────────────────┘
                                  │
                                  │ (Implements)
┌─────────────────────────────────┴──────────────────────────────────────┐
│                              Data Layer                                │
│   • Local Storage: Room Database + SQLite (AppDatabase, CardDao)      │
│   • Asset Seeding: Ingestão de all_cards.json no primeiro boot         │
│   • Hardware / Vision: CameraX + Google ML Kit (OcrAnalyzer)           │
│   • Repository Implementation: CardRepositoryImpl                      │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.1 Separação de Responsabilidades por Camada

#### A. Domain Layer (`com.riftbound.recon.domain`)
* **Independência de Framework:** Não possui nenhuma dependência do Android SDK ou de bibliotecas de terceiros. Código Kotlin puro.
* **Modelos de Domínio (`model`):** Representações imutáveis das entidades centrais:
  * `Card`: Propriedades canônicas da carta (ID, nome, expansão, código de coleção, número de colecionador, custo de energia, poder, texto, tags e URL de asset).
  * `Collection`: Representação de agrupamentos (pastas/decks criados pelo usuário).
  * `CollectionCard`: Associação entre carta e coleção, preservando a ordem cronológica do escaneamento (`scanOrder`).
* **Regras de Negócio e Algoritmo (`scanner/CardScannerMatcher`):** Motor heurístico de casamento de padrões e desambiguação de cartas físicas.
* **Contratos (`repository/CardRepository`):** Interface que dita como o domínio consome dados persistidos e reativos (`kotlinx.coroutines.flow.Flow`).

#### B. Data Layer (`com.riftbound.recon.data`)
* **Persistência Relacional (`local`):** Implementada sobre o **Room Persistence Library**, encapsulando o SQLite com mapeamento objeto-relacional eficiente e integridade referencial com chaves estrangeiras e deleção em cascata (`CASCADE`).
* **Seeding de Dados (`repository/CardRepositoryImpl`):** Injeção assíncrona automática via Coroutines no `Dispatchers.IO` a partir do arquivo canônico `assets/all_cards.json` na primeira execução do aplicativo.
* **Visão Computacional e Câmera (`scanner/OcrAnalyzer`):** Implementação da interface `ImageAnalysis.Analyzer` do CameraX, conectada ao cliente do Google ML Kit.

#### C. Presentation Layer (`com.riftbound.recon.ui`)
* **Interface Declarativa:** Construída 100% em **Jetpack Compose** com tokens de design do **Material Design 3**.
* **Gestão de Estado Reativo (`MainViewModel`):**
  * Estados encapsulados em `MutableStateFlow` e expostos como `StateFlow` imutáveis para a interface.
  * Agregação e filtragem reativa através dos operadores `combine`, `map` e `stateIn(SharingStarted.Lazily)`.
  * Atualização atômica de badges de propriedade de cartas em tempo real enquanto o usuário navega ou escaneia.
* **Navegação (`MainActivity`):** Arquitetura de Atividade Única (*Single Activity*), orquestrada pelo `Navigation Compose` com barra de navegação inferior persistente nas telas primárias e ocultação automática em telas de detalhe.

#### D. Injeção de Dependências (`com.riftbound.recon.di`)
* Módulos do **Dagger Hilt** configurados em escopo de aplicação (`SingletonComponent`), provendo instâncias únicas de `AppDatabase`, `CardDao` e vinculando `CardRepositoryImpl` à interface `CardRepository` via `@Binds`.

---

## 3. Algoritmo de Visão Computacional e Reconhecimento (`CardScannerMatcher`)

O núcleo de inovação da PoC reside no motor `CardScannerMatcher`, responsável por correlacionar os blocos de texto brutos extraídos pelo OCR com a base canônica de cartas em frações de segundo.

O algoritmo opera em uma estratégia de **duas etapas (Two-Tier Architecture)**:

```
                      [ Frame da Câmera ]
                               │
                               ▼ (CameraX ImageAnalysis)
                   [ Google ML Kit OCR Engine ]
                               │
                               ▼ (List<OcrLine> com Bounding Boxes)
               ┌───────────────────────────────┐
               │    CardScannerMatcher Pipeline│
               └───────────────┬───────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
       [ TIER 1: Regex Match ]      [ TIER 2: Heurística Espacial ]
       (Set Code + Collector Num)   1. Análise de Quadrante (Top-Left 35% x 40%)
       • OGN, SFD, UNL, OGS, etc.      para detecção de Custo de Energia.
       • Confiança: 100%            2. Normalização e Stopword Blacklist.
                │                   3. Similaridade Levenshtein (Threshold >= 0.88).
                │                   4. Validação cruzada com Energia Detectada.
                │                             │
                └──────────────┬──────────────┘
                               │
                               ▼
                       [ Carta Identificada ]
                               │
                               ▼
             [ Auto-registro na Coleção Ativa ]
```

### 3.1 Tier 1: Casamento Determinístico de Alta Confiança
O algoritmo prioriza a busca por metadados de impressão contidos na borda inferior da carta:
* **Expressão Regular:**
  $$\text{Regex: } \verb`\b(OGN|SFD|UNL|OGS|OPP|JDG|PR|VEN)\b[^\d]*?\b([0-9]{1,4}[a-z]?)\b`$$
* Ao detectar a combinação exata entre o código da coleção oficial (e.g., `OGN`, `SFD`, `UNL`) e o número identificador do colecionador (e.g., `001`, `045a`), a carta é resolvida instantaneamente com **100% de precisão**, descartando a necessidade de análises fonéticas ou fuzzy.

### 3.2 Tier 2: Heurística Espacial e Casamento Fuzzy (NLP)
Quando a iluminação, reflexos ou oclusão física impedem a leitura clara da borda inferior, o sistema entra automaticamente no modo heurístico:

1. **Análise de Quadrante Geométrico (Custo de Energia):**
   * O algoritmo calcula o retângulo envolvente de todos os textos detectados no frame:
     $$\text{BoundingFrame} = (\min(\text{left}), \min(\text{top}), \max(\text{right}), \max(\text{bottom}))$$
   * Isola a região superior esquerda da carta onde as regras do jogo padronizam a impressão do custo de invocação:
     $$\text{Região de Interesse (ROI)} = \begin{cases} \text{Eixo Y:} & \text{top} \le \min(\text{top}) + (0.35 \times \text{altura total}) \\ \text{Eixo X:} & \text{left} \le \min(\text{left}) + (0.40 \times \text{largura total}) \end{cases}$$
   * Identifica dígitos de energia candidatos no intervalo numérico $[1 \dots 10]$.

2. **Normalização e Extração de Nome-Base:**
   * Remoção de pontuações, caracteres especiais e normalização para caixa baixa via Regex `[^a-z0-9 ]`.
   * Extração de títulos compostos: divide strings em delimitadores como vírgulas e parênteses (e.g., *"Lux, Crownguard"* $\rightarrow$ *"Lux"*), aumentando a tolerância a quebras de linha introduzidas pelo OCR.

3. **Filtragem Lexical (Stopword Blacklist):**
   * Descarta vocabulário recorrente do livro de regras que polui a análise de texto das cartas (e.g., `unit`, `spell`, `damage`, `might`, `energy`, `ready`, `accelerate`).

4. **Cálculo da Distância de Edição de Levenshtein:**
   * Implementação via programação dinâmica para calcular a distância mínima de edições (inserções, remoções, substituições) entre o texto reconhecido $s_1$ e o título canônico $s_2$:
     $$\text{Similaridade}(s_1, s_2) = \frac{\max(|s_1|, |s_2|) - \text{Levenshtein}(s_1, s_2)}{\max(|s_1|, |s_2|)}$$
   * **Critério de Aceitação:** Similaridade calculada $\ge 0.88$.

5. **Validação Cruzada de Desambiguação:**
   * Para evitar falsos positivos causados por cartas de nomes similares ou títulos derivados, se um custo de energia foi detectado no quadrante superior esquerdo, a carta candidata **só é aceita se o seu custo nominal bater exatamente com o valor observado espacialmente**.

---

## 4. Matriz Tecnológica e Dependências

Todas as bibliotecas e ferramentas foram selecionadas visando desempenho nativo, compatibilidade a longo prazo e alinhamento com os padrões recomendados pelo Google para Android moderno:

| Componente / Biblioteca | Versão | Propósito / Justificativa Técnica |
| :--- | :--- | :--- |
| **Kotlin** | `1.9.22` | Linguagem base com suporte pleno a Coroutines, Flow e sintaxe idiomática moderna. |
| **Java JVM Target** | `21` | Bytecode atualizado com suporte aos recursos modernos de runtime e otimização. |
| **Android Gradle Plugin (AGP)** | `8.2.2` | Pipeline oficial de compilação, otimização e geração de artefatos APK/AAB. |
| **Kotlin Symbol Processing (KSP)** | `1.9.22-1.0.17` | Processamento de anotações até 2x mais rápido que KAPT para Dagger Hilt e Room. |
| **Android Compile / Target SDK** | `34` (Android 14) | Conformidade com as políticas de segurança e ciclo de vida do Android 14. |
| **Android Min SDK** | `24` (Android 7.0) | Compatibilidade ampla com mais de 95% da base ativa de dispositivos Android mundiais. |
| **Jetpack Compose BOM** | `2024.02.00` | Alinhamento determinístico de versões de todo o ecossistema Compose (UI, Material 3, Foundation). |
| **Kotlin Compiler Extension** | `1.5.8` | Backend do compilador Compose compatível com o Kotlin 1.9.22. |
| **Jetpack Compose Material 3** | `1.2.0` (BOM) | Sistema de design declarativo Material You com suporte a temas dinâmicos e acessibilidade. |
| **Compose Navigation** | `2.7.7` | Orquestração declarativa de rotas, pilha de telas (*backstack*) e passagem de argumentos. |
| **Dagger Hilt** | `2.50` | Injeção de dependências modular, eliminando boilerplate e garantindo testes unitários desacoplados. |
| **Room Database** | `2.6.1` | Abstração sobre SQLite com validação de queries em tempo de compilação e suporte a `Flow`. |
| **CameraX (Core, Camera2, Lifecycle, View)** | `1.3.1` | Interface estável de câmera com gerenciamento automático do ciclo de vida da Activity. |
| **Google ML Kit Text Recognition** | `16.0.0` | Rede neural leve de visão computacional otimizada para execução local em hardware móvel (CPU/NPU). |
| **Coil Compose** | `2.5.0` | Carregamento assíncrono de imagens com cache automático de memória e disco para o Compose. |
| **Kotlinx Coroutines & Flow** | Integrado | Concorrência assíncrona reativa estruturada para execução de I/O sem bloqueio de UI thread. |

---

## 5. Modelo de Dados e Persistência Relacional (Room)

A base de dados local (`riftbound_database`) é gerenciada pelo Room com integridade referencial configurada entre três entidades relacionais:

```
┌─────────────────────────┐           ┌────────────────────────────────────────┐
│       cards             │           │           collection_cards             │
├─────────────────────────┤           ├────────────────────────────────────────┤
│ PK  id (Int)            │◄──────┐   │ PK  id (Long - autoGenerate)           │
│     name (String)       │       └───┼─FK  cardId (Int - CASCADE)             │
│     cardSet (String)    │           │ FK  collectionId (Long - CASCADE)      ├──┐
│     setCode (String)    │           │     scanOrder (Int)                    │  │
│     collectorNumber     │           └────────────────────────────────────────┘  │
│     energyCost (Int)    │                                                       │
│     power (Int)         │           ┌────────────────────────────────────────┐  │
│     tags (String)       │           │              collections               │  │
│     text (String)       │           ├────────────────────────────────────────┤  │
│     imageUrl (String)   │           │ PK  id (Long - autoGenerate)           │◄─┘
└─────────────────────────┘           │     name (String)                      │
                                      │     description (String)               │
                                      │     createdAt (Long)                   │
                                      └────────────────────────────────────────┘
```

### 5.1 Características Relacionais Relevantes
* **Foreign Keys com `CASCADE`:** A exclusão de uma coleção ou de uma carta do sistema acarreta automaticamente a exclusão de todas as instâncias associadas em `collection_cards`, evitando registros órfãos.
* **Índices de Busca Rápida:** Criados explicitamente nas colunas `collectionId` e `cardId` em `collection_cards` para otimizar queries `JOIN` frequentes.
* **Preservação de Ordenação Cronológica:** O campo `scanOrder` garante que o aplicativo memorize a ordem exata em que as cartas físicas foram lidas pela câmera durante o processo de auditoria de um fichário ou caixa.
* **Agregações Reativas em Tempo Real:**
  ```sql
  SELECT c.id, c.name, c.description, c.createdAt, COUNT(cc.id) as cardsCount
  FROM collections c
  LEFT JOIN collection_cards cc ON c.id = cc.collectionId
  GROUP BY c.id
  ORDER BY c.createdAt DESC
  ```

---

## 6. Módulos Funcionais Implementados

A aplicação divide-se em 4 módulos principais acessíveis pela barra inferior:

### 6.1 Módulo Scanner (`ScanScreen`)
* **Camera Viewfinder:** Visualizador contínuo em tempo real utilizando `PreviewView` do CameraX integrado via `AndroidView` no Compose.
* **Análise de Frames em Segundo Plano:** O `OcrAnalyzer` processa cada frame em um executor assíncrono independente, liberando a thread principal de renderização.
* **Feedback de Reconhecimento:** Destaque visual da carta detectada e inserção imediata no banco de dados vinculada à coleção selecionada pelo usuário.

### 6.2 Módulo Compêndio (`CompendiumScreen`)
* **Catálogo Canônico Completo:** Exibição em grade (*LazyVerticalGrid*) de todas as cartas catalogadas no sistema.
* **Filtros Combinados:** Campo de busca textual por nome/tags acoplado a seletor horizontal de coleções (*FilterChips*).
* **Badge de Propriedade:** Cada card no catálogo exibe dinamicamente um indicador numérico informando quantas cópias daquela carta o usuário possui somadas em todas as suas coleções.
* **Modal de Detalhes:** Diálogo detalhado com visualização da imagem ampliada via Coil, atributos de combate e listagem de quais coleções físicas possuem instâncias da carta.

### 6.3 Módulo Coleções (`CollectionsScreen` & `CollectionDetailScreen`)
* **Gerenciamento de Pastas/Decks:** Criação, edição de nome/descrição e exclusão de coleções com contagem de cartas atualizada via `Flow`.
* **Detalhamento de Coleção:** Visualização individualizada das cartas lidas na coleção com opção de remoção atômica de instâncias ou limpeza completa.

### 6.4 Módulo Busca Global (`SearchScreen`)
* **Localizador de Inventário:** Busca reversa que permite ao usuário pesquisar qualquer carta e identificar imediatamente em quais coleções físicas ou caixas ela está guardada, facilitando a montagem física de decks para torneios.

---

## 7. Governança de Engenharia e Padrões de Desenvolvimento

O projeto adota uma política rigorosa de controle de versão e qualidade de código, detalhada formalmente no documento [AGENTS.md](./AGENTS.md):

* **Estratégia de Branches (Git Flow):**
  * `main`: Exclusiva para versões estáveis e releases de produção. Commits diretos são estritamente proibidos.
  * `develop`: Branch central de integração contínua. Commits diretos são proibidos; a integração ocorre exclusivamente via Pull Requests (PRs).
  * `feature/*`, `bugfix/*`, `config/*`, `refactor/*`, `bump/*`: Branches de trabalho dedicadas derivadas de `develop` e integradas de volta via PR.
  * `epic/*`: Branches de contexto estendido para grandes funcionalidades, operando como uma develop paralela.
* **Padrão de Mensagens de Commit (Conventional Commits):**
  * Estrutura formal obrigatória em língua inglesa: `[<Context>] <Type>: <Description>`
  * Exemplos: `[Scanner] Bugfix: correct text recognition bounding box`, `[GitFlow] Config: define project guidelines`.
* **Versionamento Semântico e Auditoria de Releases:**
  * O versionamento segue o padrão SemVer (`MAJOR.MINOR.PATCH`) e está centralizado no arquivo [`version.properties`](./version.properties).
  * O arquivo [`CHANGELOG.md`](./CHANGELOG.md) mantém o registro histórico de todas as alterações introduzidas na branch `develop` (seção `[Unreleased]`) e consolidadas na `main`.

---

## 8. Licença e Créditos

Este projeto foi desenvolvido como parte dos requisitos acadêmicos da **Especialização em Desenvolvimento de Aplicativos Mobile** da **Pontifícia Universidade Católica do Paraná (PUC-PR)**.

* **Direitos Reservados:** Thiago Batista (2026).
* O conteúdo visual e marcas de cartas utilizadas como amostra no catálogo destinam-se exclusivamente a fins de estudo, demonstração acadêmica e validação da Prova de Conceito de Visão Computacional.
