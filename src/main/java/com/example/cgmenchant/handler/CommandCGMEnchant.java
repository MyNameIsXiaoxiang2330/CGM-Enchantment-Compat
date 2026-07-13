/*
 * CGM Enchantment Addon — /cgmen command
 * Copyright (C) 2026 CGM Enchantment Addon Team
 */
package com.example.cgmenchant.handler;

import com.example.cgmenchant.Reference;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.*;

public class CommandCGMEnchant extends CommandBase {

    private static final Set<UUID> textDisabled = new HashSet<>();
    private static final Set<UUID> chatMode = new HashSet<>(); // true=聊天栏, false=UI上方
    public static boolean isTextEnabled(EntityPlayer player) {
        return !textDisabled.contains(player.getUniqueID());
    }

    /**
     * 发送台词（自动判断模式和开关）。
     * text 为完整的台词文本（含颜色和前缀，由 DialogueManager 组装），
     * 本方法直接发送不加任何额外前缀。
     */
    public static void sendDialogue(EntityPlayer player, String text) {
        if (textDisabled.contains(player.getUniqueID())) return;
        if (chatMode.contains(player.getUniqueID())) {
            player.sendMessage(new TextComponentString(text));
        } else {
            player.sendStatusMessage(new TextComponentString(text), true);
        }
    }

    // 台词数据已迁移至 DialogueManager (config/cgmenchant/dialogues.json)
    // DIALOGUE_PARTS 数组已移除

    @Override public String getName() { return "cgmen"; }
    @Override public String getUsage(ICommandSender s) { return "/cgmen [help|text]"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos pos) {
        if (args.length == 1) {
            return net.minecraft.command.CommandBase.getListOfStringsMatchingLastWord(args, "help", "text");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("help")) {
                return net.minecraft.command.CommandBase.getListOfStringsMatchingLastWord(args,
                    "accelerator", "puncturing", "collateral", "fire_starter",
                    "arc_light", "high_explosive", "lightweight", "over_capacity",
                    "quick_hands", "reclaimed", "trigger_finger",
                    "fellbullet", "fellbullet_piercer",
                    "Soleme_lament");
            }
            if (args[0].equalsIgnoreCase("text")) {
                return net.minecraft.command.CommandBase.getListOfStringsMatchingLastWord(args, "on", "off", "chat", "action", "ReadText");
            }
        }
        return super.getTabCompletions(server, sender, args, pos);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(sender, args);
            return;
        }
        if (args[0].equalsIgnoreCase("text")) {
            handleText(sender, args);
            return;
        }
        sender.sendMessage(new TextComponentString("§c未知指令。使用 §e/cgmen help §c查看帮助"));
    }

    // ========== Help ==========

    private void showHelp(ICommandSender sender, String[] args) {
        if (args.length >= 2) {
            String q = args[1].toLowerCase();
            String help = getEnchantHelp(q);
            if (help != null) {
                for (String l : help.split("\n")) sender.sendMessage(new TextComponentString(l));
                return;
            }
            sender.sendMessage(new TextComponentString("§c未找到附魔: " + args[1]));
            return;
        }
        sender.sendMessage(new TextComponentString("§6§l=== " + Reference.MOD_NAME + " ==="));
        sender.sendMessage(new TextComponentString("§e/cgmen help <附魔名> §7— 查看附魔说明"));
        sender.sendMessage(new TextComponentString("§e/cgmen text §7— 查看台词状态"));
        sender.sendMessage(new TextComponentString("§e/cgmen text on/off §7— 台词开关"));
        sender.sendMessage(new TextComponentString("§e/cgmen text chat/action §7— 切换显示位置"));
        sender.sendMessage(new TextComponentString("§e/cgmen text ReadText §7— 预览台词"));
        sender.sendMessage(new TextComponentString(""));
        sender.sendMessage(new TextComponentString("§7当前台词: " + (getTextStatus(sender))));
    }

    // ========== Text toggle ==========

    private void handleText(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) { sender.sendMessage(new TextComponentString("§cPlayer only.")); return; }
        EntityPlayer p = (EntityPlayer) sender;
        UUID id = p.getUniqueID();

        if (args.length >= 2) {
            String a = args[1].toLowerCase();
            if (a.equals("true") || a.equals("on")) {
                textDisabled.remove(id);
                sender.sendMessage(new TextComponentString("§a台词已开启"));
                return;
            }
            if (a.equals("false") || a.equals("off")) {
                textDisabled.add(id);
                sender.sendMessage(new TextComponentString("§c台词已关闭"));
                return;
            }
            if (a.equals("chat") || a.equals("聊天")) {
                textDisabled.remove(id);
                chatMode.add(id);
                sender.sendMessage(new TextComponentString("§a台词→聊天栏"));
                return;
            }
            if (a.equals("action") || a.equals("actionbar") || a.equals("ui")) {
                textDisabled.remove(id);
                chatMode.remove(id);
                sender.sendMessage(new TextComponentString("§a台词→UI上方"));
                return;
            }
            // 预览台词（从 DialogueManager 取随机台词）
            if (a.equals("readtext") || a.equals("preview")) {
                String line = DialogueManager.getRandom("fellbullet");
                sender.sendMessage(new TextComponentString("§7[台词预览] §r" + line));
                return;
            }
        }
        // 无参数→显示状态
        String mode = chatMode.contains(id) ? "聊天栏" : "UI上方";
        String status = textDisabled.contains(id) ? "§c关闭" : "§a开启";
        sender.sendMessage(new TextComponentString("§7台词: " + status + " §7| 位置: " + mode));
    }

    private String getTextStatus(ICommandSender s) {
        return (s instanceof EntityPlayer && textDisabled.contains(((EntityPlayer)s).getUniqueID())) ? "§c关闭" : "§a开启";
    }

    // ========== Enchantment help ==========

    private String getEnchantHelp(String q) {
        if (q.equals("accelerator") || q.contains("加速"))
            return "§6=== 加速器 ===\n§e最大等级: §7II\n§e效果: §7弹速+15%/级，伤害+10%/级\n§e冲突: §7无特殊冲突";
        if (q.equals("puncturing") || q.contains("穿甲"))
            return "§6=== 穿甲弹 ===\n§e最大等级: §7IV\n§e效果: §7忽视护甲25%/级\n§e冲突: §7无特殊冲突";
        if (q.equals("collateral") || q.contains("间接"))
            return "§6=== 间接伤害 ===\n§e最大等级: §7III\n§e效果: §7子弹穿透多目标+白色烟花粒子\n§e冲突: §7高爆弹、纵火者";
        if (q.equals("fire_starter") || q.contains("纵火"))
            return "§6=== 纵火者 ===\n§e最大等级: §7I\n§e效果: §7点燃目标，爆炸扩散火焰\n§e冲突: §7高爆弹、间接伤害";
        if (q.equals("arc_light") || q.contains("弧光"))
            return "§6=== 弧光引导 === [§b原创§r]\n§e最大等级: §7III\n§e效果: §7闪电+魔法伤害，充能苦力怕\n§e冲突: §7无特殊冲突";
        if (q.equals("high_explosive") || q.contains("高爆"))
            return "§6=== 高爆弹 === [§b原创§r]\n§e最大等级: §7V\n§e效果: §7爆炸范围/伤害随武器类型变化\n§e冲突: §7纵火者、间接伤害";
        if (q.equals("lightweight") || q.contains("轻装"))
            return "§6=== 轻装上阵 ===\n§e最大等级: §7I\n§e效果: §7开镜移速+20%，散布减小\n§e冲突: §7无特殊冲突\n§e限制: §7榴弹发射器、火箭筒无法装此附魔";
        if (q.equals("over_capacity") || q.contains("超容量"))
            return "§6=== 超容量 ===\n§e最大等级: §7III\n§e效果: §7弹匣容量+50%/级\n§e冲突: §7无特殊冲突";
        if (q.equals("quick_hands") || q.contains("熟练"))
            return "§6=== 熟练手 ===\n§e最大等级: §7II\n§e效果: §7装弹间隔-3 tick/级\n§e冲突: §7无特殊冲突";
        if (q.equals("trigger_finger") || q.contains("快速") || q.contains("扳机"))
            return "§6=== 快速扳机 ===\n§e最大等级: §7III\n§e效果: §7每级减4 tick射速，最低1 tick\n§e冲突: §7无特殊冲突";
        if (q.equals("reclaimed") || q.contains("勤俭") || q.contains("弹药"))
            return "§6=== 勤俭节约 ===\n§e最大等级: §7III\n§e效果: §7命中目标后33%/50%/87.5%返还弹药\n§e冲突: §7无特殊冲突";
        if (q.equals("fellbullet") || q.contains("地霰"))
            return "§6=== 凶弹-地霰形 === [§dEGO§r]\n§e最大等级: §7IV (宝藏附魔)\n§e效果: §7击杀时地面同心圆收缩→360°逐波散射\n§e冲突: §7除轻装上阵、加速器、勤俭节约外，与其余全部冲突\n§e限制: §7仅可附魔于霰弹枪和步枪";
        if (q.equals("fellbullet_piercer") || q.contains("贯霰"))
            return "§6=== 凶弹-贯霰形 === [§dEGO§r]\n§e最大等级: §7IV (宝藏附魔)\n§e效果: §7击杀时背后3D红环→锥形散射+贯穿\n§e伤害: §7枪械原始伤害；霰弹×2~3倍\n§e冲突: §7除轻装上阵、加速器、勤俭节约外，与其余全部冲突\n§e限制: §7仅可附魔于霰弹枪和步枪";
        if (q.equals("Soleme_lament") || q.contains("庄严") || q.contains("哀悼"))
            return "§6=== 庄严哀悼 (Solemn Mourning) === [§dEGO§r]\n§e最大等级: §7V (宝藏附魔)\n§e效果: §7命中施加「抗性削弱」，每级+0.5倍受伤（V级+2.5倍）\n§e必中: §7穿透凋灵远程免疫/末影人闪避\n§e弹道: §7黑白烟交替粒子拖尾\n§e冲突: §7除轻装上阵、加速器、勤俭节约外，与其余全部冲突\n§e限制: §7仅可附魔于手枪和加特林";
        return null;
    }
}
