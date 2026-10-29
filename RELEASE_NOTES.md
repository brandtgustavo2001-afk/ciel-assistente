Ciel — prévia experimental para Android 13 ou superior.

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
