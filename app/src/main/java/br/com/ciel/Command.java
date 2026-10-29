package br.com.ciel;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.*;

public record Command(String kind, String value, int hour, int minute) {
    public static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9: ]", " ").replaceAll("\\s+", " ").trim();
    }
    public static String afterWake(String raw) {
        String s = normalize(raw);
        Matcher m = Pattern.compile("\\b(ciel|siel|s i el)\\b").matcher(s);
        return m.find() ? s.substring(m.end()).trim() : null;
    }
    public static Command parse(String raw) {
        String s = normalize(raw);
        if (s.matches(".*\\b(ab(ra|rir|re))\\b.*(?:whats ?app|zap).*")) return new Command("whatsapp", "", 0, 0);
        if (s.contains("alarme")) {
            Matcher m = Pattern.compile("\\b([0-9]{1,2})(?:(?:[:h]| horas?(?: e)? | e | )([0-9]{1,2}))?\\b").matcher(s);
            if (m.find()) {
                int h = Integer.parseInt(m.group(1)), min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
                if (h < 24 && min < 60) return new Command("alarm", "", h, min);
                return new Command("invalid", "Horário inválido. Use horas de zero a 23 e minutos de zero a 59.", 0, 0);
            }
            String[] words = {"zero", "uma", "duas", "tres", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez", "onze", "doze"};
            for (int i=0;i<words.length;i++) if (s.matches(".*\\b"+words[i]+"\\b.*")) {
                int min=s.contains("e meia")||s.contains("e trinta")?30:s.contains("e quinze")?15:s.contains("e quarenta e cinco")?45:0;
                if (s.contains(" e ") && min==0) return new Command("invalid", "Diga o horário em números, como 7 e 30.", 0, 0);
                return new Command("alarm", "", i, min);
            }
            return new Command("invalid", "Diga criar alarme para 7 horas ou 7 e 30 em números.", 0, 0);
        }
        if (s.matches(".*\\b(feche|fechar|pare|parar|pause|pausar)\\b.*")) return new Command("pause", "", 0, 0);
        if (s.contains("proxima") || s.contains("pule")) return new Command("next", "", 0, 0);
        if (s.contains("anterior")) return new Command("previous", "", 0, 0);
        if (s.contains("continue") || s.contains("continuar") || s.contains("retome") || s.contains("retomar")) return new Command("resume", "", 0, 0);
        if (s.contains("volume")) {
            if(s.matches(".*\\b(?:abaix[a-z]*|baix[a-z]*|dimin[a-z]*|reduz[a-z]*)\\b.*"))return new Command("volumeDown", "", 0, 0);
            if(s.matches(".*\\b(?:aument[a-z]*|sub[a-z]*|elev[a-z]*)\\b.*"))return new Command("volumeUp", "", 0, 0);
            return new Command("invalid","Diga aumentar volume ou diminuir volume.",0,0);
        }
        if (s.matches(".*\\b(ab(ra|rir|re))\\b.*spotify.*")) return new Command("spotify", "", 0, 0);
        Matcher play = Pattern.compile("^(?:toque|tocar|reproduza|coloque) (?:a )?(?:musica )?(.+)$").matcher(s);
        if (play.find()) return new Command("play", play.group(1), 0, 0);
        return new Command("invalid", "Não entendi. Peça abrir WhatsApp, criar alarme ou tocar uma música.", 0, 0);
    }
}
