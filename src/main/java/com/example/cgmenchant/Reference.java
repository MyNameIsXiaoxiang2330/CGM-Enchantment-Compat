/*
 * CGM Enchantment Addon — 为 MrCrayfish's Gun Mod 添加 11 种枪械附魔
 * Copyright (C) 2026 CGM Enchantment Addon Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.cgmenchant;

public class Reference {
    public static final String MOD_ID = "cgmenchant";
    public static final String MOD_NAME = "MrCrayfish\u7684\u67AA: \u9644\u9B54\u517C\u5BB9"; // MrCrayfish的枪: 附魔兼容
    public static final String VERSION = "0.0.7.012";

    /** CGM 主模组 ID */
    public static final String CGM_MOD_ID = "cgm";

    /** CGM ItemGun 类全限定名（用于反射判断，注意：非 final 以确保不被编译器内联） */
    public static String CGM_ITEM_GUN_CLASS = "com.mrcrayfish.guns.item.ItemGun";
}
