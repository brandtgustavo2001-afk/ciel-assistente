package br.com.ciel;

import java.net.URI;
import java.util.Set;

final class UpdatePolicy {
    static boolean trustedUrl(String address,boolean api){
        try{URI uri=URI.create(address);
            return "https".equals(uri.getScheme())&&uri.getUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443)&&
                (api?"api.github.com".equals(uri.getHost()):Set.of("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com").contains(uri.getHost()));
        }catch(Exception e){return false;}
    }
    static boolean safeSize(long size){return size>0&&size<=180L*1024*1024;}
}
