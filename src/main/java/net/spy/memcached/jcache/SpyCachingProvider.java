/**
 *  Copyright 2011 Terracotta, Inc.
 *  Copyright 2011 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.spy.memcached.jcache;

import javax.cache.CacheManager;
import javax.cache.OptionalFeature;
import javax.cache.spi.CachingProvider;

/**
 */
public class SpyCachingProvider implements CachingProvider {
    /**
     * {@inheritDoc}
     */
    @Override
    public CacheManager createCacheManager(ClassLoader classLoader, String name) {
        if (name == null) {
            throw new NullPointerException("CacheManager name not specified");
        }
        String servers = System.getProperty("spymemcachedservers");
        if(servers == null || servers.trim().length() == 0){
        	throw new NullPointerException("spymemcachedservers system property not specified");
        }
        return new SpyCacheManager(servers,name, classLoader);
    }

    /**
     * {@inheritDoc}
     *
     * The RI implementation uses the thread's context ClassLoader.
     */
    @Override
    public ClassLoader getDefaultClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * {@inheritDoc}
     *
     * The RI supports {@link OptionalFeature#ANNOTATIONS} and
     * {@link OptionalFeature#STORE_BY_REFERENCE}.
     * It does not support {@link OptionalFeature#TRANSACTIONS}
     */
    @Override
    public boolean isSupported(OptionalFeature optionalFeature) {
        switch (optionalFeature) {
            case ANNOTATIONS:
                return true;
            case TRANSACTIONS:
                return false;
            case STORE_BY_REFERENCE:
                return true;
            default:
                return false;
        }
    }

}
