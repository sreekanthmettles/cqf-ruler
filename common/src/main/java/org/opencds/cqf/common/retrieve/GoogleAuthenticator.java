/*
 * Copyright 2020 meena.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencds.cqf.common.retrieve;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.security.PrivateKey;
import com.auth0.jwt.algorithms.Algorithm;
import java.io.FileInputStream;
import com.auth0.jwt.JWT;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;

/**
 *
 * @author meena
 */
public class GoogleAuthenticator {
    
    public static void main(String args[]) {
        System.out.println("Testing");
        
    }

    public String getToken() {
        String token = "";
        try {
            GoogleCredential credential
                    = GoogleCredential.fromStream(new FileInputStream("/Users/meena/Downloads/FHIR-981f292d3c96.json"));
            PrivateKey privateKey = credential.getServiceAccountPrivateKey();
            String privateKeyId = credential.getServiceAccountPrivateKeyId();

            long now = System.currentTimeMillis();

            Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey)privateKey);
            String signedJwt = JWT.create()
                    .withKeyId(privateKeyId)
                    .withIssuer("sreekanth@fhir-274221.iam.gserviceaccount.com")
                    .withSubject("sreekanth@fhir-274221.iam.gserviceaccount.com")
                    .withAudience("https://healthcare.googleapis.com/")
                    .withIssuedAt(new Date(now))
                    .withExpiresAt(new Date(now + 3600 * 1000L))
                    .sign(algorithm);
            token = signedJwt;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

}
