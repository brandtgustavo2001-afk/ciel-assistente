Ciel — prévia experimental para Android 13 ou superior.

Melhoria do entendimento dos comandos:

- Nova opção **Usar voz do Android**, ativada somente após aviso e escolha do usuário. Esse serviço pode enviar o áudio do comando ao provedor pela internet. A chamada Ciel continua local; o serviço Android não é usado para escuta contínua.
- Ao ativar, aguarde **Android pronto. Fale o comando agora**, então diga **Aumente o volume**. A linha **Última fala ouvida** não desaparece quando um comando é recusado.
- Mensagens específicas para falha de rede, microfone ocupado, ausência de serviço ou idioma e falta de correspondência.
- Correção de “baixar o volume”, que podia aumentar em vez de diminuir, e reconhecimento de variações como “Whats App” e “continuar a música”.

A disponibilidade e a precisão do serviço do Android precisam ser confirmadas no Poco. O reconhecimento local segue disponível, com as limitações do seu vocabulário.

Correção da chamada que não respondia:

- A versão anterior procurava “Ciel” na transcrição, mas o nome não existe no vocabulário do modelo. Esse defeito foi reproduzido com síntese de voz em português.
- Agora a chamada usa uma calibração acústica local: toque em **Calibrar Ciel**, diga o nome três vezes aguardando as indicações. Depois diga **Ciel**, espere o sinal e fale o comando. Não precisa dizer “Ei”.
- Novo botão **Falar comando** permite testar sem chamada. Para começar, diga **Aumente o volume**, que não requer Spotify.
- Mensagens de microfone/modelo são preservadas, e a captura começa após conceder permissão. A escuta é suspensa apenas durante a resposta falada, sem a antiga espera fixa que descartava falas.
- Referências de voz ficam apenas no aparelho e podem ser apagadas pelo aplicativo. Nenhum áudio bruto é salvo.

O detector é experimental: passou pelos testes de áudio sintetizado, mas a precisão com sua voz, ruído e aparelho bloqueado precisa ser validada no Poco.

- Atualizações verificadas e baixadas pelo próprio aplicativo por Wi-Fi, aproximadamente a cada 24 horas.
- Instalação com confirmação do Android e preservação das configurações.
- Conferência de pacote, versão, SHA-256 e assinatura digital antes de instalar.
- Comandos em português e integração Spotify Premium (requer configurar Client ID).

**Limitações:** detecção do nome Ciel e escuta no Poco bloqueado continuam sem validação no aparelho. Abrir aplicativos e criar alarmes usa notificação tocável. Não há conversa com ChatGPT nesta prévia. “Fechar Spotify” pausa a reprodução. Consulte o README antes de usar.

Baixe `Ciel.apk` para a primeira instalação. Versões posteriores podem ser obtidas pelo aplicativo. A instalação não é silenciosa: confirme quando o Android solicitar.
