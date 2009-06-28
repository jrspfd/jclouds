/**
 *
 * Copyright (C) 2009 Global Cloud Specialists, Inc. <info@globalcloudspecialists.com>
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 */
package org.jclouds.util;

import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.jclouds.http.HttpResponse;
import org.jclouds.logging.Logger;

/**
 * // TODO: Adrian: Document this!
 * 
 * @author Adrian Cole
 */
public class Utils {
   public static final String UTF8_ENCODING = "UTF-8";

   @Resource
   protected static Logger logger = Logger.NULL;

   public static String urlEncode(String in) {
      try {
         return URLEncoder.encode(in, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new IllegalStateException("Bad encoding on input: " + in, e);
      }
   }

   /**
    * Content stream may need to be read. However, we should always close the http stream.
    */
   public static byte[] closeConnectionButKeepContentStream(HttpResponse response) {
      if (response.getContent() != null) {
         try {
            byte[] data = IOUtils.toByteArray(response.getContent());
            response.setContent(new ByteArrayInputStream(data));
            return data;
         } catch (IOException e) {
            logger.error(e, "Error consuming input");
         } finally {
            IOUtils.closeQuietly(response.getContent());
         }
      }
      return null;
   }

   public static URI parseEndPoint(String hostHeader) {
      URI redirectURI = URI.create(hostHeader);
      String scheme = redirectURI.getScheme();

      checkState(redirectURI.getScheme().startsWith("http"), String.format(
               "header %s didn't parse an http scheme: [%s]", hostHeader, scheme));
      int port = redirectURI.getPort() > 0 ? redirectURI.getPort() : redirectURI.getScheme()
               .equals("https") ? 443 : 80;
      String host = redirectURI.getHost();
      checkState(!host.matches("[/]"), String.format(
               "header %s didn't parse an http host correctly: [%s]", hostHeader, host));
      URI endPoint = URI.create(String.format("%s://%s:%d", scheme, host, port));
      return endPoint;
   }

   public static URI replaceHostInEndPoint(URI endPoint, String host) {
      return URI.create(endPoint.toString().replace(endPoint.getHost(), host));
   }

   /**
    * 
    * @param <E>
    *           Exception type you'd like rethrown
    * @param e
    *           Exception you are inspecting
    * @throws E
    */
   public static void rethrowIfRuntime(Exception e) {
      if (e instanceof ExecutionException) {
         Throwable nested = e.getCause();
         if (nested instanceof Error)
            throw (Error) nested;
         e = (Exception) nested;
      }

      if (e instanceof RuntimeException) {
         throw (RuntimeException) e;
      }
   }

   /**
    * 
    * @param <E>
    *           Exception type you'd like rethrown
    * @param e
    *           Exception you are inspecting
    * @throws E
    */
   @SuppressWarnings("unchecked")
   public static <E extends Exception> void rethrowIfRuntimeOrSameType(Exception e) throws E {
      if (e instanceof ExecutionException) {
         Throwable nested = e.getCause();
         if (nested instanceof Error)
            throw (Error) nested;
         e = (Exception) nested;
      }

      if (e instanceof RuntimeException) {
         throw (RuntimeException) e;
      } else {
         try {
            throw (E) e;
         } catch (ClassCastException throwAway) {
            // using cce as there's no way to do instanceof E in current java
         }
      }
   }

   public static String toStringAndClose(InputStream input) throws IOException {
      try {
         return IOUtils.toString(input);
      } finally {
         IOUtils.closeQuietly(input);
      }
   }

   /**
    * Encode the given string with the given encoding, if possible. If the encoding fails with
    * {@link UnsupportedEncodingException}, log a warning and fall back to the system's default
    * encoding.
    * 
    * @see {@link String#getBytes(String)}
    * @see {@link String#getBytes()} - used as fall-back.
    * 
    * @param str
    * @param encoding
    * @return
    */
   public static byte[] encodeString(String str, String encoding) {
      try {
         return str.getBytes(encoding);
      } catch (UnsupportedEncodingException e) {
         logger.warn(e, "Failed to encode string to bytes with encoding " + encoding
                  + ". Falling back to system's default encoding");
         return str.getBytes();
      }
   }

   /**
    * Encode the given string with the UTF-8 encoding, the sane default. In the very unlikely event
    * the encoding fails with {@link UnsupportedEncodingException}, log a warning and fall back to
    * the system's default encoding.
    * 
    * @param str
    * @return
    */
   public static byte[] encodeString(String str) {
      return encodeString(str, UTF8_ENCODING);
   }

   /**
    * Decode the given string with the given encoding, if possible. If the decoding fails with
    * {@link UnsupportedEncodingException}, log a warning and fall back to the system's default
    * encoding.
    * 
    * @param bytes
    * @param encoding
    * @return
    */
   public static String decodeString(byte[] bytes, String encoding) {
      try {
         return new String(bytes, encoding);
      } catch (UnsupportedEncodingException e) {
         logger.warn(e, "Failed to decode bytes to string with encoding " + encoding
                  + ". Falling back to system's default encoding");
         return new String(bytes);
      }
   }

   /**
    * Decode the given string with the UTF-8 encoding, the sane default. In the very unlikely event
    * the encoding fails with {@link UnsupportedEncodingException}, log a warning and fall back to
    * the system's default encoding.
    * 
    * @param str
    * @return
    */
   public static String decodeString(byte[] bytes) {
      return decodeString(bytes, UTF8_ENCODING);
   }

}
