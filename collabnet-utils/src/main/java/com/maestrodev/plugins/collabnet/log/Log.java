package com.maestrodev.plugins.collabnet.log;

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
 * Simple log target so that diagnostic messages can be sent to any type of log capture.
 */
public interface Log {
    /**
     * Log a message at DEBUG level.
     *
     * @param msg the message to log
     */
    void debug(String msg);

    /**
     * Log a message at INFO level.
     *
     * @param msg the message to log
     */
    void info(String msg);
}
