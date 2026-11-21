# KeyHistory

**Teclado Android (IME) com histórico pessoal, local e criptografado do que você digita.**

Kotlin · Jetpack Compose · InputMethodService · Room · Android Keystore + AES‑256‑GCM · DataStore · Navigation Compose · MVVM · Android 10+ (API 29)

---

## 1. Download do APK

O APK instalável fica em [`apk/KeyHistory-1.0.0.apk`](apk/) (gerado e testado automaticamente pelo GitHub Actions a cada envio).
Também está disponível como artefato `KeyHistory-apk` em cada execução da aba **Actions**.

## 2. Instalação

1. Copie `KeyHistory-1.0.0.apk` para o celular (ou baixe direto pelo navegador do celular).
2. Abra o arquivo. Se o Android pedir, permita **"Instalar apps desconhecidos"** para o app usado (Arquivos/Chrome).
3. Toque em **Instalar** e depois em **Abrir**.

Pelo computador (opcional): `adb install -r apk/KeyHistory-1.0.0.apk`

## 3. Ativar o teclado

1. Abra o KeyHistory. No card **Status do teclado**, toque em **1. Ativar teclado** (abre *Configurações › Sistema › Idiomas e entrada › Teclado na tela › Gerenciar teclados*).
2. Ative **KeyHistory Teclado**. O Android mostra um aviso padrão para qualquer teclado de terceiros — confirme.
3. Volte ao app e toque em **Selecionar teclado** → escolha **KeyHistory Teclado**.
4. Toque em **Ativar registro** e leia/aceite o termo de consentimento.

Os três indicadores (instalado, ativado, selecionado) ficam verdes no painel.

## 4. Manual rápido

| Onde | O que fazer |
|---|---|
| **Teclado** | Layout ABNT2 com **Ç**. Toque longo numa letra mostra acentos (á ã â à…) e o número da linha de cima. `?123` → números/símbolos; `=\<` → mais símbolos e acentos mortos (´ ` ^ ~ ¨ + vogal). Shift: 1 toque = maiúscula, 2 toques = caps lock. Toque longo no espaço troca de teclado. Apagar repete se segurado. |
| **Barra do teclado** | Indicador permanente: 🟢 *Registro ativo* · 🟠 *Pausado* · ⚪ *Desativado* · 🔴 *Não registrado – campo protegido*. Botões: **Salvar agora**, **Pausar/Retomar**, **Abrir KeyHistory**, **Trocar teclado**. |
| **Quando salva** | Ao tocar Enviar/OK/Buscar, ao sair do campo, quando o app limpa o campo (ex.: botão enviar do WhatsApp) ou pelo botão Salvar. Desative "Salvar ao concluir o campo" para salvar só manualmente. |
| **Painel** | KPIs (total, hoje, apps, armazenamento), status do teclado, atalhos e gráficos (por dia, por app, semanal) — todos com dados reais do banco. |
| **Histórico** | Filtros Hoje/Ontem/7 dias/30 dias/Tudo, agrupado cronologicamente. Toque = detalhes; toque longo = seleção múltipla para **exportar** ou **excluir**. |
| **Detalhes** | ID, conteúdo, data, horário, app, pacote, categoria, indicador de criptografia; **Copiar** e **Excluir** (com confirmação). |
| **Pesquisar** | Palavra (ignora acentos/maiúsculas, aceita início de palavra), **Frase exata**, aplicativo, período rápido ou **intervalo personalizado**. |
| **Exportar** | CSV (`Data;Horário;Aplicativo;Conteúdo;Categoria`, UTF‑8, compatível com Excel) salvo pelo seletor de arquivos do Android. |
| **Configurações** | Ativar/pausar registro, salvamento automático, vibração, ativar/selecionar teclado, indicador de privacidade, armazenamento, limpar histórico, exportar, política de privacidade, sobre, versão. |
| **Armazenamento** | Tamanho do banco, apagar registros > 30/90 dias, apagar por app, **excluir apps do registro**, apagar tudo. |

## 5. Privacidade e segurança

- **Sem permissão INTERNET** no manifesto: o app é tecnicamente incapaz de transmitir dados. Nenhum servidor, nenhum SDK de terceiros.
- **Consentimento**: o registro vem **desligado**; só liga após aceitar o termo. Pausa a qualquer momento no próprio teclado.
- **Somente o que você digita com este teclado** é registrado — o conteúdo pré‑existente do campo nunca é lido.
- **Nunca registrado**: senhas (todas as variações), campos numéricos/PIN/telefone, campos com `IME_FLAG_NO_PERSONALIZED_LEARNING` (abas anônimas, etc.), campos cuja dica indique dado sensível (senha, código, cartão, CPF, token, chave Pix, CVV…), campos do próprio KeyHistory, origens não verificáveis e apps excluídos pelo usuário.
- **Filtro de conteúdo**: descarta textos que parecem números de cartão (validação Luhn), CPF/CNPJ, códigos de verificação (4–8 dígitos) e tokens/chaves.
- **Criptografia**: cada registro é cifrado com **AES‑256‑GCM** (IV aleatório por registro, tag de 128 bits). A chave de dados é embrulhada por uma chave mestra **não exportável do Android Keystore**. A pesquisa usa um **índice cego HMAC‑SHA256** — o banco nunca contém texto legível.
- **Sem backup**: `allowBackup=false` e regras de extração excluem tudo de backup em nuvem/transferência.
- Cópia para a área de transferência marcada como sensível (Android 13+). Exportação apenas por ação explícita.

## 6. Limitações do Android (e alternativas adotadas)

| Limitação | Alternativa |
|---|---|
| Um teclado só vê o que é digitado **por ele**; não há forma legítima (nem desejável) de capturar texto de outros teclados, colagens ou ditado. | O histórico cobre apenas digitação pelo KeyHistory. Accessibility Service **não** é usado. |
| O Android não informa ao teclado quando o usuário toca no botão "enviar" de um app (ex.: WhatsApp). | O teclado detecta quando o app **limpa o campo** e salva naquele momento; há também o botão **Salvar agora**. |
| No Android 11+, o nome de um app só pode ser lido se ele for visível ao KeyHistory. | Declarada visibilidade apenas de apps com ícone no launcher (`<queries>`), sem `QUERY_ALL_PACKAGES`. Se a origem não puder ser verificada, o registro é **bloqueado**. |
| Se o usuário mover o cursor e editar no meio do texto, o buffer do teclado pode divergir do campo. | O registro reflete a sequência digitada; o conteúdo do campo não é lido por design de privacidade. |
| O CSV exportado não é criptografado (precisa ser legível em planilhas). | Aviso explícito na tela de exportação. |

## 7. Compilar

Requisitos: Android Studio (Ladybug ou mais novo) ou JDK 17 + Android SDK 35.

```bash
./gradlew :app:assembleRelease        # APK em app/build/outputs/apk/release/
./gradlew :app:testDebugUnitTest      # testes unitários (JVM)
./gradlew :testfield:installDebug :app:connectedDebugAndroidTest   # testes no aparelho/emulador
```

O APK é assinado com a chave de distribuição do projeto (`signing/keyhistory.jks`). **Para publicar na Play Store use a sua própria chave**: defina `KEYHISTORY_KEYSTORE`, `KEYHISTORY_STORE_PASSWORD`, `KEYHISTORY_KEY_ALIAS`, `KEYHISTORY_KEY_PASSWORD`.

## 8. Estrutura

```
app/src/main/java/com/keyhistory/
├── MainActivity.kt / KeyHistoryApp.kt   # entrada + contêiner de dependências
├── keyboard/     # InputMethodService, view do teclado, layouts ABNT2, elegibilidade de campos
├── database/     # Room: entidades, DAO, database
├── repository/   # HistoryRepository (cripto + índice), SettingsRepository (DataStore)
├── viewmodel/    # ViewModels MVVM
├── security/     # CryptoManager (Keystore/AES-GCM/HMAC), filtro de conteúdo sensível
├── navigation/   # Navigation Compose
├── screens/      # Painel, Histórico, Pesquisa, Detalhes, Exportar, Configurações, Armazenamento, Privacidade, Sobre
├── components/   # Tema, cards, gráficos (Canvas), itens
└── utils/        # Datas, CSV, tokenizador de pesquisa, status do teclado
testfield/        # app auxiliar com campos comuns e protegidos, usado nos testes E2E
```

## 9. Relatório de testes

Ver [`docs/RELATORIO_TESTES.md`](docs/RELATORIO_TESTES.md).
