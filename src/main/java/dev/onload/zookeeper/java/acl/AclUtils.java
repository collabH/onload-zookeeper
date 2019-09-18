package dev.onload.zookeeper.java.acl;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.io.IOException;

public class AclUtils {

    public static String getDigestUserPwd(String id) throws Exception {
        return DigestAuthenticationProvider.generateDigest(id);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException, Exception {
        String id = "name:name";
        String idDigested = getDigestUserPwd(id);
        System.out.println(idDigested);
    }
}
