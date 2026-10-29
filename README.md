# Ciel · Android · prévia com atualizações

[Baixar a versão mais recente](https://github.com/brandtgustavo2001-afk/ciel-assistente/releases/latest/download/Ciel.apk) · [Todas as versões](https://github.com/brandtgustavo2001-afk/ciel-assistente/releases)

## Atualizações no celular

Instale `Ciel.apk` da versão mais recente uma vez. Se já instalou a prévia 0.1, esta instalação atualiza por cima: o pacote e a assinatura foram preservados.

Depois disso, a Ciel verifica e baixa atualizações por Wi-Fi aproximadamente a cada 24 horas usando WorkManager. O Android pode adiar a execução por bateria, ausência de Wi-Fi, restrições do HyperOS ou interrupção forçada do aplicativo. A busca é programada ao abrir a Ciel pela primeira vez. Permita notificações para receber o aviso de atualização pronta.

Toque no aviso ou em **Instalar atualização baixada** e confirme no Android. Na primeira vez, autorize a Ciel em “Instalar apps desconhecidos”. **A instalação não é silenciosa:** esse fluxo precisa da sua confirmação. Não precisa desinstalar nem procurar um novo APK manualmente. As configurações e a conexão Spotify são mantidas.

O botão **Verificar atualização agora** também funciona com dados móveis, após confirmação do download. O APK inclui o modelo de voz e tem cerca de 70 MB.

O atualizador aceita somente HTTPS em hosts específicos do GitHub, limita tamanho, verifica SHA-256, identificador de pacote, versão crescente e a mesma assinatura do app instalado. O Android faz a validação final ao instalar. Nenhum token GitHub é armazenado no celular.

## Publicação de novas versões

Cada push em `main` executa `.github/workflows/android.yml`: testes, lint, build, assinatura e publicação de uma GitHub Release com `Ciel.apk`, `update.json` e SHA-256. Se alguma etapa falhar, a publicação não ocorre. Pull requests executam verificação sem receber a chave de assinatura nem publicar.

O `versionCode` de publicação é `1000 + GITHUB_RUN_NUMBER`, e o nome `0.2.GITHUB_RUN_NUMBER`. Preserve o workflow e sua sequência: se recriá-lo ou migrar o repositório, aumente a base para ficar acima das versões já instaladas. Reexecutar uma publicação já existente não substitui os arquivos automaticamente; faça novo push para gerar a próxima versão.

A chave privada de continuidade está nos GitHub Actions Secrets, com cópia local ignorada pelo Git em `.tools/ciel-signing.jks`. Essa chave foi preservada do primeiro APK experimental para permitir atualizar a instalação existente. Não a publique nem a substitua: perder a chave impede atualizar instalações existentes sem desinstalar. Esta é uma distribuição experimental fora da loja, ainda não uma configuração definitiva de assinatura para Google Play.

Assistente de comandos em português para teste no Poco X7 Pro (Android 13 ou superior). Sem chave OpenAI, sem conversa de IA e sem acesso à conta ChatGPT. O áudio é reconhecido localmente com Vosk; apenas buscas e comandos musicais usam a internet e a API Spotify.

## Estado real

- Serviço de microfone iniciado por ação do usuário, notificação persistente com Parar e encerramento automático em 8 horas. Não reinicia no boot nem após encerramento pelo sistema.
- Reconhecimento de fala local com modelo português. A detecção de `Ciel` / `Siel` é experimental: o nome depende do vocabulário e da transcrição do modelo e NÃO foi validado. Veja “Ouvi” no aplicativo. Não é um detector de palavra dedicado e pode não reconhecer o nome ou pode ativar por engano.
- Abrir WhatsApp/Spotify e criar alarme: gera notificação tocável. NÃO abre aplicativos automaticamente com a tela bloqueada. O Android pode exigir senha. Alarme só é solicitado após o toque; não diga que foi criado antes da confirmação no relógio.
- Spotify: OAuth PKCE sem segredo embutido, busca por nome, reprodução do primeiro resultado, pausar/continuar, próxima/anterior. Exige Premium, Client ID e permissão da conta no aplicativo cadastrado. Comandos afetam o dispositivo ativo da conta, que pode ser outro aparelho. Comece reproduzindo no Spotify do Poco para torná-lo ativo.
- “Feche Spotify” pausa a reprodução; não encerra o processo à força.
- O modelo português pequeno pode errar nomes de músicas e artistas; o primeiro resultado pode não ser a música desejada.
- Volume altera a saída de música do celular.

**Esta prévia NÃO atende ainda ao requisito completo de ativação confiável por Ciel e execução de todas as ações sem toque com o aparelho bloqueado. Compilar não comprova funcionamento em aparelho real.**

## Spotify (configuração necessária)

1. Em https://developer.spotify.com/dashboard crie um aplicativo com Web API.
2. Cadastre exatamente `br.com.ciel://oauth/callback` como Redirect URI.
3. Se estiver em modo de desenvolvimento, confira se sua conta está autorizada nas configurações do aplicativo e se seu cadastro atende às regras atuais do Spotify.
4. Copie SOMENTE o Client ID para o campo no app Ciel. Não forneça Client Secret, senha ou token.
5. Toque em Conectar Spotify e autorize no navegador oficial.
6. Abra o Spotify no Poco e reproduza algo uma vez. Depois teste Ciel.

Tokens ficam nos dados privados do app, excluídos de backup. Desconectar apaga a cópia local; para revogar a autorização no Spotify use a página de aplicativos da conta Spotify.

## Instalar e testar

Baixe `Ciel.apk` na seção Releases do GitHub, transfira para o Poco e abra para instalar a prévia. O Android poderá pedir autorização de instalação para o aplicativo que abriu o arquivo. É uma distribuição experimental, não uma versão de loja.

1. Abra Ciel e permita microfone e notificações. Toque novamente em Ativar escuta se a permissão foi recém-concedida.
2. Aguarde o carregamento do modelo. Diga “Ciel, próxima música” e observe “Ouvi”. Teste em silêncio e com música para medir erros.
3. Teste “Ciel” sozinho; após “Pode falar”, diga o comando.
4. Apague a tela por 1, 5 e 15 minutos e teste novamente. Repita com o aparelho bloqueado por senha. Registre resultados; o HyperOS pode interromper o serviço por bateria.
5. Confirme que Desligar microfone e Parar na notificação removem o indicador de microfone.
6. Ao abrir aplicativos ou pedir alarme, toque na notificação e desbloqueie se solicitado. Confira o horário na tela do relógio.
7. Verifique o comportamento sem internet e com o Spotify desconectado. O app deve explicar a falha sem afirmar sucesso.

Não há bypass de senha nem serviço de acessibilidade para clicar em telas. Os últimos textos de diagnóstico ficam localmente até serem substituídos ou os dados do aplicativo serem apagados. Áudio não é gravado em arquivo.

## Compilar

JDK 17, Android SDK 35 e Gradle 8.9. Defina `sdk.dir` em `local.properties` ou ANDROID_HOME. Use `./gradlew testDebugUnitTest assembleDebug lintDebug`. Saída: `app/build/outputs/apk/debug/app-debug.apk`. Para publicação, use o workflow e os segredos de assinatura. Builds debug feitos em outra máquina têm outra assinatura e não atualizam o app publicado.

O modelo Vosk `vosk-model-small-pt-0.3` deve estar em `app/src/main/assets/model-pt/`, incluindo arquivo `uuid`. Fonte: https://alphacephei.com/vosk/models (licença do modelo listada pelo fornecedor: Apache 2.0). Vosk e JNA são dependências Gradle; conserve seus avisos de licença na distribuição. Não publique APKs com tokens ou identificadores privados preenchidos.

## Referências consultadas

- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/guide/components/activities/background-starts
- https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow
- https://alphacephei.com/vosk/models

## Próximos critérios de conclusão

Medir detecção de Ciel no Poco; testar bateria e áudio com tela bloqueada; validar autorização e reprodução Spotify com conta real; avaliar integração de assistente do sistema para reduzir os toques permitidos pelo Android. Sem esses testes, a versão permanece experimental.
