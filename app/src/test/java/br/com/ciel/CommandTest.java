package br.com.ciel;
import org.junit.Test;
import static org.junit.Assert.*;
public class CommandTest {
    @Test public void wakeMustBeAWord() { assertNull(Command.afterWake("especial toque algo")); assertEquals("abra spotify",Command.afterWake("Ciel, abra Spotify")); }
    @Test public void routes() { assertEquals("whatsapp",Command.parse("abra o WhatsApp").kind()); assertEquals("play",Command.parse("toque a música Evidências").kind()); assertEquals("evidencias",Command.parse("toque a música Evidências").value()); assertEquals("pause",Command.parse("feche Spotify").kind()); }
    @Test public void alarms() { assertEquals(7,Command.parse("crie alarme para sete horas").hour()); assertEquals(30,Command.parse("alarme 07:30").minute()); assertEquals("invalid",Command.parse("alarme 25:99").kind()); }
    @Test public void controls() { assertEquals("next",Command.parse("próxima música").kind()); assertEquals("resume",Command.parse("continue").kind()); }
    @Test public void alarmMinutesAreNotDropped() { assertEquals(30,Command.parse("alarme 7 e 30").minute()); assertEquals(30,Command.parse("alarme sete e meia").minute()); assertEquals("invalid",Command.parse("alarme sete e vinte").kind()); }
    @Test public void naturalVolumePhrasesKeepDirection() {for(String phrase:new String[]{"baixar o volume","baixa o volume","abaixe o volume","diminua o volume","reduzir o volume"})assertEquals(phrase,"volumeDown",Command.parse(phrase).kind());for(String phrase:new String[]{"aumenta o volume","aumentar o volume","subir o volume"})assertEquals(phrase,"volumeUp",Command.parse(phrase).kind());assertEquals("invalid",Command.parse("volume").kind());}
    @Test public void androidTranscriptionVariations() {assertEquals("whatsapp",Command.parse("abra o Whats App").kind());assertEquals("whatsapp",Command.parse("abrir o zap").kind());assertEquals("resume",Command.parse("continuar a música").kind());}
}
