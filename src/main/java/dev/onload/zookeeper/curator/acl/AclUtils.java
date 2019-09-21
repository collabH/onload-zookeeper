package dev.onload.zookeeper.curator.acl;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * @author echo huang
 * @version 1.0
 * @date 2019-09-22 00:12
 * @description
 */
public class AclUtils {
    public static String getDigestUserPwd(String id) {
        String digest = "";
        try {
            digest = DigestAuthenticationProvider.generateDigest(id);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digest;
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException, Exception {
        String id = "name:hsm";
        String idDigested = getDigestUserPwd(id);
        System.out.println(idDigested);
    }
}
