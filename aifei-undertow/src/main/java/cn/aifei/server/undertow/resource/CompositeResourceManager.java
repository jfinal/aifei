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

package cn.aifei.server.undertow.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;

/**
 * CompositeResourceManager
 */
public class CompositeResourceManager implements ResourceManager {

    protected ResourceManager[] resourceManagers = new ResourceManager[0];

    public boolean isEmpty() {
        return resourceManagers.length == 0;
    }

    public void add(ResourceManager resourceManager) {
        List<ResourceManager> list = new ArrayList<ResourceManager>();
        for (ResourceManager rm : resourceManagers) {
            list.add(rm);
        }
        list.add(resourceManager);

        this.resourceManagers = list.toArray(new ResourceManager[list.size()]);
    }

    public Resource getResource(String path) throws IOException {
        for (ResourceManager rm : resourceManagers) {
            Resource ret = rm.getResource(path);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public boolean isResourceChangeListenerSupported() {
        for (ResourceManager rm : resourceManagers) {
            if (rm.isResourceChangeListenerSupported()) {
                return true;
            }
        }
        return false;
    }

    public void registerResourceChangeListener(ResourceChangeListener listener) {
        for (ResourceManager rm : resourceManagers) {
            if (rm.isResourceChangeListenerSupported()) {
                rm.registerResourceChangeListener(listener);
            }
        }
    }

    public void removeResourceChangeListener(ResourceChangeListener resourceChangeListener) {
        for (ResourceManager rm : resourceManagers) {
            if (rm.isResourceChangeListenerSupported()) {
                rm.removeResourceChangeListener(resourceChangeListener);
            }
        }
    }

    public void close() throws IOException {
        for (ResourceManager rm : resourceManagers) {
            rm.close();
        }
    }
}


