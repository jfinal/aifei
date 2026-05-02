/*
 * Copyright 2011-2035 詹波 (aifei.cn)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.aifei.server.undertow.ssl;

import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import org.xnio.Options;
import org.xnio.Sequence;
import cn.aifei.server.undertow.UndertowConfig;
import io.undertow.Undertow.Builder;

/**
 * SslBuilder
 */
public class SslBuilder {

    protected Builder builder;
    protected UndertowConfig undertowConfig;
    protected SslConfig sslConfig;

    public SslBuilder(Builder builder, UndertowConfig undertowConfig) {
        this.builder = builder;
        this.undertowConfig = undertowConfig;
        this.sslConfig = undertowConfig.getSslConfig();
    }

    public void build() {
        try {
            SSLContext sslContext = SSLContext.getInstance(sslConfig.getProtocol());
            sslContext.init(getKeyManagers(sslConfig), null, null);
            builder.addHttpsListener(sslConfig.port, undertowConfig.getHost(), sslContext);

            // 配置 ciphers
            if (sslConfig.getCiphers() != null) {
                builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of(sslConfig.getCiphers()));
            }

            // 配置 enabledProtocols
            if (sslConfig.getEnabledProtocols() != null) {
                builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(sslConfig.getEnabledProtocols()));
            }
        }
        catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected KeyManager[] getKeyManagers(SslConfig sslConfig) {
        try {
            KeyStore keyStore = loadKeyStore(sslConfig);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            char[] keyPassword = (sslConfig.getKeyPassword() != null) ? sslConfig.getKeyPassword().toCharArray() : null;

            if (keyPassword == null && sslConfig.getKeyStorePassword() != null) {
                keyPassword = sslConfig.getKeyStorePassword().toCharArray();
            }

            keyManagerFactory.init(keyStore, keyPassword);
            if (sslConfig.getKeyAlias() != null) {
                return getConfigurableAliasKeyManagers(sslConfig, keyManagerFactory.getKeyManagers());
            } else {
                return keyManagerFactory.getKeyManagers();
            }
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * 1：优先从 class path 与 jar 包之中获取密钥，然后从文件系统中获取密钥
     *
     * 2：开发环境下 class path 包括 target/classes、jar 包。由于 src/main/resources
     *    目录下的资源会被 copy 到 target/classes 目录之下，因此该目录可看成是 class path
     *
     * 3：部署环境下 class path 包括 config 目录、jar 包
     */
    protected KeyStore loadKeyStore(SslConfig sslConfig) throws Exception {
        String keyStoreType = sslConfig.getKeyStoreType();
        String keyStore = sslConfig.getKeyStore();
        String keyStorePassword = sslConfig.getKeyStorePassword();

        InputStream stream = undertowConfig.getClassLoader().getResourceAsStream(keyStore);

        if (stream == null) {
            // stream = new File(name).toURI().toURL().openStream();
            stream = Files.newInputStream(Paths.get(keyStore));
        }

        if (stream == null) {
            throw new RuntimeException("Could not load keystore");
        }

        try (InputStream is = stream) {
            KeyStore ret;
            if (keyStoreType != null) {
                //  "JKS"、"PKCS12"(.pfx)
                ret = KeyStore.getInstance(keyStoreType);
            } else {
                // 下面代码并不能自动探测密钥库类型
                ret = KeyStore.getInstance(KeyStore.getDefaultType());
            }

            ret.load(is, (keyStorePassword != null) ? keyStorePassword.trim().toCharArray() : null);
            return ret;
        }
    }

    protected KeyManager[] getConfigurableAliasKeyManagers(SslConfig sslConfig, KeyManager[] keyManagers) {
        for (int i = 0; i < keyManagers.length; i++) {
            if (keyManagers[i] instanceof X509ExtendedKeyManager) {
                keyManagers[i] = new ConfigurableAliasKeyManager((X509ExtendedKeyManager)keyManagers[i], sslConfig.getKeyAlias());
            }
        }
        return keyManagers;
    }

    /**
     * 支持 alias 别名配置
     */
    protected static class ConfigurableAliasKeyManager extends X509ExtendedKeyManager {

        protected X509ExtendedKeyManager keyManager;
        protected String alias;

        protected ConfigurableAliasKeyManager(X509ExtendedKeyManager keyManager, String alias) {
            this.keyManager = keyManager;
            this.alias = alias;
        }

        @Override
        public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
            return this.keyManager.chooseEngineClientAlias(strings, principals, sslEngine);
        }

        @Override
        public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
            if (this.alias == null) {
                return this.keyManager.chooseEngineServerAlias(s, principals, sslEngine);
            }
            return this.alias;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return this.keyManager.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return this.keyManager.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return this.keyManager.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return this.keyManager.getClientAliases(keyType, issuers);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return this.keyManager.getPrivateKey(alias);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return this.keyManager.getServerAliases(keyType, issuers);
        }
    }
}



