/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.security.sasl.scram;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.password.spec.ClearPasswordSpec;
import org.wildfly.security.password.spec.ScramDigestPasswordSpec;
import org.wildfly.security.sasl.WildFlySasl;
import org.wildfly.security.sasl.test.BaseTestCase;
import org.wildfly.security.sasl.test.ClientCallbackHandler;
import org.wildfly.security.sasl.test.ServerCallbackHandler;
import org.wildfly.security.sasl.util.AbstractSaslParticipant;

import static org.wildfly.security.password.interfaces.ScramDigestPassword.ALGORITHM_SCRAM_SHA_1;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class BasicScramSelfTest extends BaseTestCase {

    private static final Provider provider = new WildFlyElytronProvider();
    private static final Map<String, Object> EMPTY = Collections.<String, Object>emptyMap();

    @BeforeClass
    public static void registerPasswordProvider() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Security.insertProviderAt(provider, 2);
                return null;
            }
        });
    }

    @AfterClass
    public static void removePasswordProvider() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                Security.removeProvider(provider.getName());
                return null;
            }
        });
    }

    @Test
    public void testAuthenticationSha1ClearPassword() throws Exception {
        ServerCallbackHandler serverHandler = new ServerCallbackHandler("user", "pencil".toCharArray());
        ClientCallbackHandler clientHandler = new ClientCallbackHandler("user", "pencil".toCharArray());
        testAuthentication(Scram.SCRAM_SHA_1, serverHandler, clientHandler, "user", EMPTY, EMPTY);
    }

    @Test
    public void testAuthenticationSha1ClearCredentialPassword() throws Exception {
        ServerCallbackHandler serverHandler = new ServerCallbackHandler("user", "clear", new ClearPasswordSpec("pencil".toCharArray()));
        ClientCallbackHandler clientHandler = new ClientCallbackHandler("user", "pencil".toCharArray());
        testAuthentication(Scram.SCRAM_SHA_1, serverHandler, clientHandler, "user", EMPTY, EMPTY);
    }

    @Test
    public void testAuthenticationSha1ClearCredential() throws Exception {
        ServerCallbackHandler serverHandler = new ServerCallbackHandler("user", "clear", new ClearPasswordSpec("pencil".toCharArray()));
        ClientCallbackHandler clientHandler = new ClientCallbackHandler("user", null, "clear", new ClearPasswordSpec("pencil".toCharArray()));
        testAuthentication(Scram.SCRAM_SHA_1, serverHandler, clientHandler, "user", EMPTY, EMPTY);
    }

    @Test
    public void testAuthenticationSha1ScramCredential() throws Exception {
        byte[] digest = new byte[]{(byte)0x1d, (byte)0x96, (byte)0xee, (byte)0x3a, (byte)0x52, (byte)0x9b, (byte)0x5a, (byte)0x5f, (byte)0x9e, (byte)0x47, (byte)0xc0, (byte)0x1f, (byte)0x22, (byte)0x9a, (byte)0x2c, (byte)0xb8, (byte)0xa6, (byte)0xe1, (byte)0x5f, (byte)0x7d};
        byte[] salt = new byte[]{(byte)0x41, (byte)0x25, (byte)0xc2, (byte)0x47, (byte)0xe4, (byte)0x3a, (byte)0xb1, (byte)0xe9, (byte)0x3c, (byte)0x6d, (byte)0xff, (byte)0x76};
        ServerCallbackHandler serverHandler = new ServerCallbackHandler("user", ALGORITHM_SCRAM_SHA_1, new ScramDigestPasswordSpec(digest, salt, 4096));
        ClientCallbackHandler clientHandler = new ClientCallbackHandler("user", "pencil".toCharArray());
        testAuthentication(Scram.SCRAM_SHA_1, serverHandler, clientHandler, "user", EMPTY, EMPTY);
    }

    @Test
    public void testAuthenticationSha1ScramCredentialBindingData() throws Exception {
        byte[] digest = new byte[]{(byte)0x1d, (byte)0x96, (byte)0xee, (byte)0x3a, (byte)0x52, (byte)0x9b, (byte)0x5a, (byte)0x5f, (byte)0x9e, (byte)0x47, (byte)0xc0, (byte)0x1f, (byte)0x22, (byte)0x9a, (byte)0x2c, (byte)0xb8, (byte)0xa6, (byte)0xe1, (byte)0x5f, (byte)0x7d};
        byte[] salt = new byte[]{(byte)0x41, (byte)0x25, (byte)0xc2, (byte)0x47, (byte)0xe4, (byte)0x3a, (byte)0xb1, (byte)0xe9, (byte)0x3c, (byte)0x6d, (byte)0xff, (byte)0x76};
        ServerCallbackHandler serverHandler = new ServerCallbackHandler("user", ALGORITHM_SCRAM_SHA_1, new ScramDigestPasswordSpec(digest, salt, 4096));
        ClientCallbackHandler clientHandler = new ClientCallbackHandler("user", "pencil".toCharArray());
        serverHandler.setBinding("type1", new byte[]{(byte)0xFE, (byte)0xDC, (byte)0x10});
        clientHandler.setBinding("type1", new byte[]{(byte)0xFE, (byte)0xDC, (byte)0x10});
        testAuthentication(Scram.SCRAM_SHA_1_PLUS, serverHandler, clientHandler, "user", EMPTY, EMPTY);
    }

    @Test
    public void testAuthenticationSha1ScramCredentialBindingDataRequired() throws Exception {
        byte[] digest = new byte[]{(byte)0x1d, (byte)0x96, (byte)0xee, (byte)0x3a, (byte)0x52, (byte)0x9b, (byte)0x5a, (byte)0x5f, (byte)0x9e, (byte)0x47, (byte)0xc0, (byte)0x1f, (byte)0x22, (byte)0x9a, (byte)0x2c, (byte)0xb8, (byte)0xa6, (byte)0xe1, (byte)0x5f, (byte)0x7d};
        byte[] salt = new byte[]{(byte)0x41, (byte)0x25, (byte)0xc2, (byte)0x47, (byte)0xe4, (byte)0x3a, (byte)0xb1, (byte)0xe9, (byte)0x3c, (byte)0x6d, (byte)0xff, (byte)0x76};
        ServerCallbackHandler serverHandler = new ServerCallbackHandler("user", ALGORITHM_SCRAM_SHA_1, new ScramDigestPasswordSpec(digest, salt, 4096));
        ClientCallbackHandler clientHandler = new ClientCallbackHandler("user", "pencil".toCharArray());
        serverHandler.setBinding("same-type2", new byte[]{(byte)0xFE, (byte)0xDC, (byte)0x12});
        clientHandler.setBinding("same-type2", new byte[]{(byte)0xFE, (byte)0xDC, (byte)0x12});
        Map<String, String> props = new HashMap<String, String>();
        props.put(WildFlySasl.CHANNEL_BINDING_REQUIRED, "true");
        testAuthentication(Scram.SCRAM_SHA_1_PLUS, serverHandler, clientHandler, "user", props, props);
    }

    public void testAuthentication(String mechanism, CallbackHandler serverHandler, CallbackHandler clientHandler, String authorizationId, Map<String, ?> serverProps, Map<String, ?> clientProps) throws Exception {
        final SaslServerFactory serverFactory = obtainSaslServerFactory(ScramSaslServerFactory.class);
        assertNotNull(serverFactory);
        final SaslClientFactory clientFactory = obtainSaslClientFactory(ScramSaslClientFactory.class);
        assertNotNull(clientFactory);

        final SaslServer saslServer = serverFactory.createSaslServer(mechanism, "test", "localhost", serverProps, serverHandler);
        assertNotNull(saslServer);
        assertTrue(saslServer instanceof ScramSaslServer);
        final SaslClient saslClient = clientFactory.createSaslClient(new String[] { mechanism }, authorizationId, "test", "localhost", clientProps, clientHandler);
        assertNotNull(saslClient);
        assertTrue(saslClient instanceof ScramSaslClient);
        byte[] message = AbstractSaslParticipant.NO_BYTES;
        do {
            message = saslClient.evaluateChallenge(message);
            if (message == null) break;
            message = saslServer.evaluateResponse(message);
        } while (message != null);
        assertTrue(saslServer.isComplete());
        assertTrue(saslClient.isComplete());
    }
}
