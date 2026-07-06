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

package com.example.cgmenchant.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

/**
 * <h1>ASM 字节码转换器 — CGM 的两个精准手术</h1>
 *
 * <h2>被修改的 CGM 类</h2>
 * <table border="1">
 *   <tr><th>目标类</th><th>修改内容</th><th>修改原因</th><th>影响的附魔</th></tr>
 *   <tr>
 *     <td>{@code com.mrcrayfish.guns.item.ItemGun}</td>
 *     <td>注入方法 {@code getItemEnchantability()}</td>
 *     <td>CGM 0.15.3 的 ItemGun 未覆写此方法，默认返回 0，导致原版附魔台拒绝为该物品附魔</td>
 *     <td><b>全部 11 个附魔</b></td>
 *   </tr>
 *   <tr>
 *     <td>{@code com.mrcrayfish.guns.event.CommonEvents$ReloadTracker}</td>
 *     <td>替换 {@code isWeaponFull()} 的弹容量读取路径</td>
 *     <td>原方法从静态 Gun 配置读取 maxAmmo，无法感知附魔在运行时的动态修改</td>
 *     <td><b>超容量 (Over Capacity)</b></td>
 *   </tr>
 * </table>
 *
 * <h2>关键版本信息</h2>
 * <ul>
 *   <li>CGM 版本: v0.15.3</li>
 *   <li>ItemGun 路径: com.mrcrayfish.guns.item.ItemGun（非旧版 common 包）</li>
 *   <li>fire() 签名: static (World, EntityPlayer, ItemStack)</li>
 *   <li>reload() 不存在于 ItemGun，通过 CommonEvents + 网络包实现</li>
 *   <li>枪械数据: Gun.serverGun 对象的 public 字段（非 NBT）</li>
 * </ul>
 *
 * <h2>为什么不直接用 Mixin？</h2>
 * <p>
 * 理论上 Mixin 是更优雅的方案。但在 1.12.2 Forge 生态中：
 * <ul>
 *   <li>Mixin 需要额外安装 MixinBootstrap 作为前置模组</li>
 *   <li>CGM 本身不使用 Mixin，引入 MixinBootstrap 会增加用户安装负担</li>
 *   <li>CoreMod（FMLCorePlugin）是 1.12.2 时代的标准做法，无需额外前置</li>
 *   <li>本 CoreMod 只做两处最小化手术，每处操作极其精确，降低了冲突概率</li>
 * </ul>
 * </p>
 *
 * @author CGM Enchantment Addon Team
 * @since 0.0.1
 * @see GunTransformer
 */
public class GunClassTransformer implements IClassTransformer {

    /** CGM ItemGun 的 MCP 全限定名 */
    private static final String ITEMGUN = "com.mrcrayfish.guns.item.ItemGun";

    /** CGM CommonEvents 的内部类 ReloadTracker 的 MCP 全限定名 */
    private static final String RELOADER = "com.mrcrayfish.guns.event.CommonEvents$ReloadTracker";

    /**
     * FML 在加载每个类时回调此方法。
     * 我们只拦截两个 CGM 类，其余原样放行。
     *
     * @param name            原始类名（含斜杠，如 com/example/Foo）
     * @param transformedName MCP 反混淆后的类名（用点分隔，如 com.example.Foo）
     * @param basicClass      原始字节码（可能为 null，表示未找到该类）
     * @return 修改后的字节码，或原始字节码（未匹配时）
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        if (ITEMGUN.equals(transformedName)) return addEnchantability(basicClass);
        if (RELOADER.equals(transformedName)) return patchIsWeaponFull(basicClass);

        return basicClass;
    }

    // ===================================================================
    //  注入 A: ItemGun.getItemEnchantability() → 30
    // ===================================================================

    /**
     * <h3>注入 {@code getItemEnchantability()} 方法</h3>
     *
     * <h4>问题</h4>
     * <p>
     * 原版 Minecraft 的附魔台在处理物品时调用 {@code Item.getItemEnchantability()}。
     * CGM 的 ItemGun 继承自 Item，不覆写此方法 → 默认返回 0。
     * 原版附魔逻辑：{@code enchantability == 0} 意味着该物品不可附魔。
     * 结果：玩家无法在生存模式附魔台中对枪使用附魔书。
     * </p>
     *
     * <h4>解决方案</h4>
     * <p>
     * 在 ItemGun 类的末尾注入以下方法：
     * <pre>{@code
     * public int getItemEnchantability() {
     *     return 30;  // 等同铁质工具：平衡「可附魔但不至于太容易出高级附魔」
     * }
     * }</pre>
     * </p>
     *
     * <h4>附魔能力值参考</h4>
     * <table border="1">
     *   <tr><th>工具类型</th><th>enchantability</th></tr>
     *   <tr><td>木质 / 皮革</td><td>15</td></tr>
     *   <tr><td>石质 / 锁链</td><td>5</td></tr>
     *   <tr><td><b>铁质 → 枪（本模组选择）</b></td><td><b>30</b></td></tr>
     *   <tr><td>金质</td><td>22</td></tr>
     *   <tr><td>钻石</td><td>10</td></tr>
     *   <tr><td>书</td><td>1</td></tr>
     * </table>
     * <p>
     * 选择 30（铁质）的理由：枪是主力武器，附魔成功率应该合理但不过高。
     * </p>
     *
     * <h4>ASM 实现细节</h4>
     * <p>
     * 在 ClassVisitor.visitEnd() 中追加方法，确保不破坏任何已有方法。
     * 字节码等效于：
     * <pre>
     * BIPUSH 30    // 将常量 30 压入操作数栈
     * IRETURN      // 返回 int
     * </pre>
     * </p>
     *
     * @param bc ItemGun 的原始字节码
     * @return 注入了 getItemEnchantability() 的字节码
     */
    private byte[] addEnchantability(byte[] bc) {
        ClassReader cr = new ClassReader(bc);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public void visitEnd() {
                // ★ 必须在 visitEnd() 中追加，此时所有原始方法已写入完毕
                super.visitEnd();

                // public int getItemEnchantability()
                MethodVisitor mv = cw.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "getItemEnchantability",
                    "()I",       // 无参数，返回 int
                    null,        // 无泛型签名
                    null         // 无异常声明
                );
                mv.visitCode();
                mv.visitIntInsn(Opcodes.BIPUSH, 30);   // 压入 30
                mv.visitInsn(Opcodes.IRETURN);          // 返回
                mv.visitMaxs(1, 1);                     // 栈深度 1，局部变量表 1 (this)
                mv.visitEnd();
            }
        }, 0);
        return cw.toByteArray();
    }

    // ===================================================================
    //  注入 B: ReloadTracker.isWeaponFull() — 弹容量路径替换
    // ===================================================================

    /**
     * <h3>替换 {@code isWeaponFull()} 的弹容量读取路径</h3>
     *
     * <h4>问题背景</h4>
     * <p>
     * CGM 的换弹系统在工作流程中是这样运转的：
     * <ol>
     *   <li>玩家按 R 键 → 客户端发网络包 → 服务端标记玩家为 RELOADING 状态</li>
     *   <li>每 tick: ReloadTracker 检查 {@code isWeaponFull()}：
     *       <ul>
     *         <li>如果返回 true → 当前弹药已达上限 → 停止换弹</li>
     *         <li>如果返回 false → 继续装填</li>
     *       </ul>
     *   </li>
     *   <li>每次装填成功: AmmoCount + 1</li>
     *   <li>装填完成: 清除 RELOADING 标记</li>
     * </ol>
     * </p>
     *
     * <h4>原始 {@code isWeaponFull()} 的逻辑（伪代码）</h4>
     * <pre>{@code
     * boolean isWeaponFull(NBTTagCompound tag) {
     *     // 1. 从静态 Gun 配置对象读取弹容量上限
     *     int maxAmmo = this.gun.general.maxAmmo;   // ← 问题在这里！
     *
     *     // 2. 从 NBT 读取当前弹药
     *     int ammo = tag.getInteger("AmmoCount");
     *
     *     // 3. 比较
     *     return ammo >= maxAmmo;
     * }
     * }</pre>
     *
     * <p>
     * <b>问题核心：</b>{@code this.gun.general.maxAmmo} 是直接从 Gun 配置对象读取的<b>静态值</b>，
     * 这个值在枪的 JSON 配置文件中定义，<b>不会随附魔效果而变化</b>。
     * </p>
     *
     * <p>
     * 当"超容量"附魔需要将弹容量从 30 提升到 45（+50%）时：
     * <ul>
     *   <li>GunStateHandler 通过反射修改了 Gun.general.maxAmmo（Java 层面生效）</li>
     *   <li>GunStateHandler 同时写入 tag["MaxAmmo"] = 45（NBT 层面标记）</li>
     *   <li><b>但 isWeaponFull() 依然读取 this.gun.general.maxAmmo</b> ← 这个字段链指向的是
     *       某个缓存的 Gun 对象引用，不一定是 GunStateHandler 修改的那个实例</li>
     *   <li>结果：isWeaponFull() 返回 true（因为 30 ≤ 原值 30 的引用），换弹提前停止</li>
     * </ul>
     * </p>
     *
     * <h4>解决方案</h4>
     * <p>
     * 将 isWeaponFull() 中的弹容量读取路径从<b>字段链访问</b>改为<b>NBT 读取</b>：
     * </p>
     *
     * <table border="1">
     *   <tr><th></th><th>原始字节码</th><th>替换后字节码</th></tr>
     *   <tr>
     *     <td>操作</td>
     *     <td>
     *       aload_0 (this)<br>
     *       getfield gun<br>
     *       getfield general<br>
     *       getfield maxAmmo<br>
     *       → 静态配置值
     *     </td>
     *     <td>
     *       aload_1 (tag 参数)<br>
     *       ldc "MaxAmmo"<br>
     *       invokevirtual NBTTagCompound.getInteger(String)<br>
     *       → NBT 动态值（由 GunStateHandler 维护）
     *     </td>
     *   </tr>
     *   <tr>
     *     <td>字节码</td>
     *     <td>
     *       25 (aload_0)<br>
     *       B4 00XX (getfield gun)<br>
     *       B4 00YY (getfield general)<br>
     *       B4 00ZZ (getfield maxAmmo)
     *     </td>
     *     <td>
     *       2B (aload_1)<br>
     *       12 LL (ldc "MaxAmmo")<br>
     *       B6 01WW (invokevirtual getInteger)
     *     </td>
     *   </tr>
     * </table>
     *
     * <h4>SRG 名称说明</h4>
     * <p>
     * 替换后的方法调用使用 SRG 名称 {@code func_74762_e} 而非 MCP 名称 {@code getInteger}，
     * 原因：ForgeGradle 在构建时会将 SRG 名称自动映射到 MCP。使用 SRG 名称可以确保
     * 在 MCP 映射版本不同时也能正确工作（stable_39 中 func_74762_e → getInteger）。
     * </p>
     *
     * <h4>ASM 实现细节</h4>
     * <p>
     * 使用 MethodVisitor 拦截模式逐条追踪字节码指令：
     * <ol>
     *   <li>检测 {@code aload_0} → 标记 seenAload0 = true</li>
     *   <li>检测紧跟的 {@code getfield "gun"} → 标记 gunField = true</li>
     *   <li>跳过 {@code getfield "general"}（它是中间字段，不产出有用值）</li>
     *   <li>检测 {@code getfield "maxAmmo"} → <b>这是替换点！</b>
     *       <ul>
     *         <li>不写入原始 getfield 指令</li>
     *         <li>改为写 aload_1 → ldc "MaxAmmo" → invokevirtual getInteger</li>
     *       </ul>
     *   </li>
     *   <li>任何非上述模式的指令 → 重置状态机，原样写入</li>
     * </ol>
     * </p>
     *
     * <h4>为什么不用 Mixin 的 @ModifyConstant / @Redirect？</h4>
     * <p>
     * 理论上 Mixin 的 {@code @Redirect} 可以重定向 getfield 调用。
     * 但 CGM 0.15.3 不依赖 Mixin，引入 MixinBootstrap 作为前置会增加用户安装复杂度。
     * 本 ASM 方案虽然代码量大，但实现完全自包含，零外部依赖。
     * </p>
     *
     * @param bc ReloadTracker 的原始字节码
     * @return 修改了 isWeaponFull() 弹容量读取路径的字节码
     */
    private byte[] patchIsWeaponFull(byte[] bc) {
        ClassReader cr = new ClassReader(bc);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String sig, String[] exc) {
                MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);

                // 只处理 isWeaponFull()Z 方法
                if (!"isWeaponFull".equals(name) || !"()Z".equals(desc)) return mv;

                return new MethodVisitor(Opcodes.ASM5, mv) {

                    // ===== 状态机：追踪 aload_0 → getfield gun → getfield general → getfield maxAmmo =====
                    boolean seenAload0 = false;    // 刚看到 aload_0
                    boolean gunField = false;      // 已确认是 gun 字段的 getfield

                    /**
                     * 步骤 1: 检测 aload_0（this 引用）。
                     * 如果是 slot 0 的 ALOAD，标记 seenAload0。
                     */
                    @Override
                    public void visitVarInsn(int op, int v) {
                        if (op == Opcodes.ALOAD && v == 0) {
                            seenAload0 = true;
                            return;  // 不写入，等待确认是否为目标字段链
                        }
                        seenAload0 = false;
                        super.visitVarInsn(op, v);
                    }

                    /**
                     * 步骤 2 & 3: 检测字段访问链。
                     *
                     * 目标链: this.gun.general.maxAmmo
                     *          ↑     ↑        ↑
                     *   步骤2: 检测 "gun"  步骤3: 跳过 "general"  步骤4: 替换 "maxAmmo"
                     *
                     * 注意: 如果 seenAload0 为 true 但字段不是 "gun"，
                     *       说明是另一条 aload_0 → getfield XXX 的路径，
                     *       需要补写入 aload_0（之前被吞掉了）然后正常处理。
                     */
                    @Override
                    public void visitFieldInsn(int op, String owner, String fName, String fDesc) {
                        // --- 步骤 2: 检测 this.gun ---
                        if (op == Opcodes.GETFIELD && seenAload0) {
                            if ("gun".equals(fName)) {
                                // 确认: 这就是 this.gun → 进入下一层追踪
                                gunField = true;
                                seenAload0 = false;
                                return;  // 吞掉此 getfield，不写入
                            }
                            // 不是 "gun" 字段 → 误判，补写入 aload_0 + 正常写入 getfield
                            seenAload0 = false;
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitFieldInsn(op, owner, fName, fDesc);
                            return;
                        }

                        // --- 步骤 3 & 4: 在 gun 字段之后的字段访问 ---
                        if (op == Opcodes.GETFIELD && gunField) {
                            if ("general".equals(fName)) {
                                // 步骤 3: gun.general → 跳过此 getfield，继续等 maxAmmo
                                return;
                            }
                            if ("maxAmmo".equals(fName)) {
                                // ★★★ 步骤 4: 替换点 ★★★
                                // 原始: getfield maxAmmo (读取静态配置值)
                                // 替换: 方法参数 tag.getInteger("MaxAmmo")
                                gunField = false;

                                // aload_1  → 加载方法第一个参数 (NBTTagCompound tag)
                                super.visitVarInsn(Opcodes.ALOAD, 1);
                                // ldc "MaxAmmo" → 字符串常量
                                super.visitLdcInsn("MaxAmmo");
                                // invokevirtual NBTTagCompound.func_74762_e(String)int
                                // func_74762_e 是 getInteger 的 SRG 名称
                                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                    "net/minecraft/nbt/NBTTagCompound",
                                    "func_74762_e",            // SRG: getInteger
                                    "(Ljava/lang/String;)I",   // 参数 String，返回 int
                                    false);
                                return;
                            }
                        }

                        // 不匹配任何目标模式 → 重置状态机，正常写入
                        seenAload0 = false;
                        gunField = false;
                        super.visitFieldInsn(op, owner, fName, fDesc);
                    }

                    // ===== 以下 visit* 方法重置状态机（任何非目标模式的指令都打破追踪链）=====

                    @Override public void visitInsn(int op) {
                        seenAload0 = false;
                        super.visitInsn(op);
                    }

                    @Override public void visitIntInsn(int op, int v) {
                        seenAload0 = false;
                        super.visitIntInsn(op, v);
                    }

                    @Override public void visitLdcInsn(Object c) {
                        seenAload0 = false;
                        super.visitLdcInsn(c);
                    }

                    @Override public void visitMethodInsn(int op, String o, String n,
                                                          String d, boolean i) {
                        seenAload0 = false;
                        super.visitMethodInsn(op, o, n, d, i);
                    }

                    @Override public void visitJumpInsn(int op, Label l) {
                        seenAload0 = false;
                        super.visitJumpInsn(op, l);
                    }

                    @Override public void visitLabel(Label l) {
                        seenAload0 = false;
                        super.visitLabel(l);
                    }
                };
            }
        }, 0);
        return cw.toByteArray();
    }
}
