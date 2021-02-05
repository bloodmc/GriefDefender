/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.configuration.category;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MessageCategory extends ConfigCategory {

    @Setting(value = "locale", comment = "Set the locale to use for GD messages. (Default: en_US)\n" +
            "Available languages: de_DE, en_US, es_ES, fr_FR, pl_PL, ru_RU, zh_CN, zh_HK. The data is stored under assets in jar.\n" +
            "Note: The language code must be lowercase and the country code must be uppercase.")
    public String locale = "en_US";

    @Setting(value = "enter-exit-show-gd-prefix", comment = "Whether GD prefix should be shown in enter/exit claim messages. (Default: true)")
    public boolean enterExitShowGdPrefix = true;

    @Setting(value = "enter-exit-chat-type", comment = "The default chat type to use when sending enter/claim messages to a player.\n" + 
            "(0 = Chat, 1 = ActionBar, 2 = Title)/nNote: ActionBar is only available in MC 1.11+")
    public int enterExitChatType = 0;
}
