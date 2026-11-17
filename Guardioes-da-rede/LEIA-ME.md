# Guardiões da Rede — Casos Digitais (Android/Kotlin)

Aplicativo educativo nativo para Android e TV Box, feito em Kotlin e Views da plataforma, sem bibliotecas de terceiros, WebView ou acesso à internet. Os três casos são carregados localmente de `app/src/main/assets/casos.json`; o progresso é salvo no armazenamento privado do aplicativo.

## Requisitos

- Android Studio com JDK 17 ou superior
- Android SDK Platform 34 e Build Tools 34
- Gradle Wrapper incluído no projeto

O app oferece alvos grandes, navegação por toque, mouse e teclado/controle remoto, foco visível e suporte a telas com rolagem. Os personagens, mensagens e pistas são fictícios. Nenhuma ferramenta abre links reais, instala aplicativos, lê imagens ou solicita senhas e códigos.

## Abrir e executar

1. Extraia o ZIP e abra a pasta `guardioes-android` no Android Studio.
2. Aguarde a sincronização do Gradle e selecione um dispositivo Android ou emulador.
3. Execute a configuração `app` ou use **Build > Build APK(s)**.

Pelo terminal, na raiz do projeto:

```bash
./gradlew assembleDebug
```

O APK de depuração fica em `app/build/outputs/apk/debug/app-debug.apk`.

Para gerar uma versão de distribuição, configure uma chave de assinatura no Android Studio e use **Build > Generate Signed Bundle / APK**. Não distribua o APK de depuração como versão final.

## Casos no JSON

Edite `casos.json` para mudar ambientes, personagens, pistas, perícias simuladas, alertas, tarefas e soluções. O formato atual contém três casos e não exige recompilar conteúdo fora desse arquivo. Preserve IDs e referências cruzadas entre pistas, ambientes, suspeitos, técnicas e soluções.

## Compatibilidade e memória

- `minSdk = 22` (Android 5.1 ou superior)
- Sem bibliotecas externas, imagens em runtime, rede ou serviços em segundo plano
- Uma única Activity nativa; somente o estado do caso atual e pontuações pequenas são mantidos em memória
- Para medir em um dispositivo conectado:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n br.cefetmg.guardioes/.MainActivity
adb shell dumpsys meminfo br.cefetmg.guardioes
```

Anote o `TOTAL PSS` após abrir e percorrer as telas. O resultado depende do aparelho, da versão do Android e do estado do processo.

## Imagens
Ficam em `app/src/main/res/drawable-nodpi/` (WebP, 360 a 560 px, ~270 KB no total). Nomes: mascote_acolhedora (início), mascote_agente (desfecho), mascote_permissoes (Caso 1), mascote_repost e mascote_localizacao (Caso 2), chat_igualdade, chat_brincadeira e chat_apoio (Caso 3), menino_escolar (reserva).
Para trocar uma imagem: salve outro WebP com o mesmo nome (letras minúsculas, números e _). Mantenha até ~600 px para não pesar na RAM de 1 GB.
