package com.maestrodev.plugins.collabnet;

/*
 * Copyright 2012 MaestroDev
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

/**
 * An exception when attempting to find a particular resource using the SOAP APIs that could not be found.
 */
public class ResourceNotFoundException extends Exception {
    /**
     * Create the exception with a message indicating what resource could not be found.
     *
     * @param msg the message
     */
    public ResourceNotFoundException(String msg) {
        super(msg);
    }
}
