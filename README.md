# Riftbound Recon — Mobile Edge-Vision & TCG Collection

> **Nota de Apresentação Acadêmica:**  
> Este projeto é parte integrante a ser entregue como **Trabalho de Conclusão de Curso (TCC)** da **Especialização em Desenvolvimento de Aplicativos Mobile** da **Pontifícia Universidade Católica do Paraná (PUCPR - Curitiba, Paraná)**.  
> **Autor:** Thiago Batista  
> **Versão Atual do App:** `0.1.1` (versionCode: `2` — gerenciado centralizadamente em [`version.properties`](./version.properties))

---

## 1. Descrição do Projeto

O **Riftbound Recon** é uma aplicação Android nativa (Prova de Conceito - PoC) desenvolvida para resolver o problema de catalogação, conferência e busca manual de cartas físicas de *Trading Card Games* (TCG).

Utilizando uma abordagem de **Computação de Borda (Edge AI)**, o aplicativo realiza o reconhecimento óptico de cartas em tempo real diretamente na câmera do celular, **100% offline**, sem enviar dados ou imagens para servidores externos. Além do scanner, o aplicativo oferece um sistema completo de gerenciamento de coleções/decks físicos e um compêndio canônico com busca reversa de inventário.

---

## 2. Módulos e Funcionalidades

* **📷 Capturar (Scanner):** Abertura de câmera com pré-visualização contínua via CameraX e Google ML Kit OCR. Identifica a carta apontada através de correspondência heurística (código da coleção/número ou nome e custo) e a insere instantaneamente na coleção ativa selecionada pelo usuário.
* **📖 Compêndio:** Catálogo canônico contendo todas as cartas do jogo, busca por nome e texto de regras, filtros por expansão e *badges* dinâmicos indicando quantas cópias daquela carta o usuário possui somadas em todas as suas coleções.
* **📂 Coleções:** Criação, edição, exclusão e visualização de pastas e decks físicos, preservando a ordem cronológica em que as cartas foram lidas (*scanOrder*).
* **🔍 Buscar (Localizador de Acervo):** Busca reversa que indica em quais coleções físicas ou pastas uma determinada carta está armazenada.

---

## 3. Arquitetura e Padrões de Projeto

A estrutura do código segue os princípios de **Clean Architecture**, **MVVM (Model-View-ViewModel)** e **Unidirectional Data Flow (UDF)**:

```
┌─────────────────────────────────────────────────────────────┐
│                    UI (Apresentação)                        │
│   Jetpack Compose (Material 3) + Navigation Compose         │
│                 ▲                         │                 │
│      StateFlow  │                         │ Eventos         │
│                 │                         ▼                 │
│                         MainViewModel                       │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain (Negócio)                      │
│   • Modelos puros (Card, Collection, CollectionCard)        │
│   • Regras de Reconhecimento (CardScannerMatcher)           │
│   • Contrato de Repositório (CardRepository)                │
└─────────────────────────────────▲───────────────────────────┘
                                  │
                                  │ Implementa
┌─────────────────────────────────┴───────────────────────────┐
│                        Data (Dados)                         │
│   • Banco de dados local: Room (SQLite) + CardDao           │
│   • Ingestão de carga inicial (assets/all_cards.json)       │
│   • Pipeline de Câmera: CameraX + Google ML Kit OCR         │
│   • Implementação do repositório (CardRepositoryImpl)       │
└─────────────────────────────────────────────────────────────┘
```

* **Desacoplamento:** O módulo `domain` não possui nenhuma dependência do Android SDK, permitindo alta testabilidade e independência de frameworks.
* **Injeção de Dependências:** Orquestrada via **Dagger Hilt**, provendo instâncias únicas em escopo de aplicação (`SingletonComponent`).
* **Reatividade:** Comunicação assíncrona entre camadas estruturada sobre **Kotlin Coroutines** e **StateFlow/Flow**.

---

## 4. Tecnologias e Bibliotecas Principais

| Categoria | Tecnologia / Biblioteca | Versão | Função Principal |
| :--- | :--- | :--- | :--- |
| **Linguagem & Runtime** | Kotlin | `1.9.22` | Linguagem base moderna para Android |
| **JVM Target** | Java | `21` | Plataforma de compilação e execução bytecode |
| **Build & Tooling** | Android Gradle Plugin (AGP) / KSP | `8.2.2` / `1.0.17` | Automação de compilação e processamento de anotações |
| **Interface de Usuário** | Jetpack Compose (BOM) / Material 3 | `2024.02.00` | UI 100% declarativa e tokens de design Material You |
| **Navegação** | Navigation Compose | `2.7.7` | Roteamento declarativo de telas com backstack |
| **Injeção de Dependência**| Dagger Hilt | `2.50` | Injeção de dependências e ViewModels |
| **Persistência Local** | Room Database | `2.6.1` | Abstração relacional do SQLite com integridade referencial |
| **Câmera & Imagem** | CameraX | `1.3.1` | Gerenciamento de ciclo de vida e captura de frames de vídeo |
| **Visão Computacional** | Google ML Kit Text Recognition | `16.0.0` | Extração de OCR local *on-device* em tempo real |
| **Carregamento de Imagens**| Coil Compose | `2.5.0` | Carregamento assíncrono e cache de arte das cartas |

---

## 5. Requisitos e Como Rodar o Projeto

### 5.1 Requisitos do Ambiente de Desenvolvimento
Para compilar e executar o projeto, seu ambiente precisa atender aos seguintes pré-requisitos:

* **IDE Recomendada:** Android Studio Iguana (2023.2.1) ou superior (e.g. Ladybug / Jellyfish).
* **Java Development Kit (JDK):** Versão **21** (configurado em *Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK*).
  > **Nota de compatibilidade da Gradle JVM:** O Gradle 8.5 suporta até o Java 21. Caso o Android Studio alerte sobre incompatibilidade com uma versão superior instalada no sistema (ex.: JVM 25), basta selecionar a opção sugerida **Use JVM 21** (ou `jbr-21`).
* **Android SDK:**
  * **Compile SDK:** `34` (Android 14)
  * **Target SDK:** `34` (Android 14)
  * **Min SDK:** `24` (Android 7.0 Nougat ou superior)
* **Hardware para Execução:**
  * **Dispositivo Físico Android (Recomendado):** Com câmera traseira funcional e permissão concedida no primeiro acesso para testar a detecção em tempo real.
  * **Emulador Android (AVD):** API 24 a 34, configurado com suporte a câmera virtual (*VirtualScene* ou webcam integrada do computador) para que o módulo de captura consiga emitir frames para o OCR.

### 5.2 Passo a Passo para Compilar e Executar

1. **Abrir o Projeto no Android Studio:**
   * Abra o Android Studio, selecione **Open** e aponte para a pasta raiz deste repositório.
   * Aguarde a sincronização do Gradle (*Gradle Sync*). O arquivo [`version.properties`](./version.properties) e os arquivos de compilação serão lidos automaticamente.

2. **Compilar via Terminal (Opcional):**
   ```bash
   # Compilar o APK de desenvolvimento (Debug)
   ./gradlew assembleDebug

   # Executar a suíte de testes unitários
   ./gradlew testDebugUnitTest
   ```

3. **Executar no Aparelho ou Emulador:**
   * Conecte o aparelho físico com depuração USB ativada (ou inicie o emulador).
   * No Android Studio, selecione a configuração **`app`** na barra superior e clique no botão **Run ▶** (ou utilize o atalho `Shift + F10` / `Control + R` no macOS).
   * Na primeira abertura da tela **Capturar**, confirme a permissão de acesso à câmera solicitada pelo sistema operacional.

---

## 6. Governança de Código e Versionamento

O projeto segue padrões formais de controle de versão descritos em detalhes nos documentos internos:
* **[`AGENTS.md`](./AGENTS.md):** Diretrizes de Git Flow, branches protegidas (`main` e `develop`), convenções de commits semânticos em inglês (`[Context] Type: Description`) e ciclo de vida de releases.
* **[`CHANGELOG.md`](./CHANGELOG.md):** Registro cronológico de todas as melhorias e correções integradas em cada versão.
* **[`version.properties`](./version.properties):** Arquivo centralizado de definição de versão (`versionCode` e `versionName`), consumido automaticamente pelo script de build do Gradle.

---

## 7. Créditos

* **Autor:** Thiago Batista (2026).
* **Trabalho Final:** Especialização em Desenvolvimento de Aplicativos Mobile — **PUCPR (Curitiba - PR)**.
