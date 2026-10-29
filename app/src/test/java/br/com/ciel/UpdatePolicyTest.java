package br.com.ciel;
import org.junit.Test;
import static org.junit.Assert.*;

public class UpdatePolicyTest {
    @Test public void acceptsOnlyExpectedHttpsHosts(){assertTrue(UpdatePolicy.trustedUrl("https://api.github.com/repos/a/b/releases/latest",true));assertTrue(UpdatePolicy.trustedUrl("https://release-assets.githubusercontent.com/a.apk",false));}
    @Test public void refusesLookalikesAndCredentials(){assertFalse(UpdatePolicy.trustedUrl("http://github.com/a.apk",false));assertFalse(UpdatePolicy.trustedUrl("https://github.com.evil.example/a.apk",false));assertFalse(UpdatePolicy.trustedUrl("https://github.com@evil.example/a.apk",false));assertFalse(UpdatePolicy.trustedUrl("https://user@github.com/a.apk",false));assertFalse(UpdatePolicy.trustedUrl("https://github.com:8443/a.apk",false));}
    @Test public void boundsDownloadSize(){assertFalse(UpdatePolicy.safeSize(-1));assertFalse(UpdatePolicy.safeSize(0));assertTrue(UpdatePolicy.safeSize(70L*1024*1024));assertFalse(UpdatePolicy.safeSize(181L*1024*1024));}
}
