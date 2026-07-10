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

    /** 发送台词（自动判断模式） */
    public static void sendDialogue(EntityPlayer player, String text) {
        if (textDisabled.contains(player.getUniqueID())) return;
        String msg = "§c凶弹§7》 " + text;
        if (chatMode.contains(player.getUniqueID())) {
            player.sendMessage(new TextComponentString(msg));
        } else {
            player.sendStatusMessage(new TextComponentString(msg), true);
        }
    }

    /** 返回随机台词（供附魔触发时调用） */
    public static String getRandomDialogue() {
        return DIALOGUE_PARTS[new java.util.Random().nextInt(DIALOGUE_PARTS.length)];
    }

    @Override public String getName() { return "cgmen"; }
    @Override public String getUsage(ICommandSender s) { return "/cgmen [help|text]"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    private static final String[] DIALOGUE_PARTS = {
        "§c逃跑是没用的。四散而去的子弹，不论在哪儿都能华丽地击碎你的脑壳。",
        "§c一发子弹完全不够……直到枪声和烟雾……把一切记忆埋葬为止……!",
        "§c我能感知到沉重的枪管压于胃肠之上。让这凶恶的弹丸贯穿他们……!",
        "§c消失四散的记忆或是对他们的窥视，都没有必要……只需射击，一直射击!",
        "§c无需为行将就木者惋惜。……若能寻得，终将……那般。",
        "§c我与我之友人皆为此奋斗，将要为我所贯穿的同僚亦…… ",
        "§c漆黑的血块将伴随清脆的枪响四散开来。所射出的子弹亦会裂为数股，将他们悉数贯穿。如此一来，同僚们的牺牲便有其价值了。",
        "§c不……哪怕一开始共为同僚，如今这记忆也早已模糊了！",
        "§c无论是这枪，抑或是这褴褛……都在时刻向我低语。不要瞄准敌人，而是瞄准那些仍铭记着爱的人。",
        "§c要将其摧毁，射击，射...?……我刚才说到哪里了？ ",
        "§c可你们……却依然记得所爱之人，这就是代价",
        "§c我要把你们的脑袋全部打爆。一个不留。",
        "§c我已经什么都想不起来了。连其是否存在过都模糊不清。",
        "§c从我耳边滚开！",
        "§c这里是战场，而我的枪必将打爆谁。",
        "§c因此我随意将枪口指向任何人都没问题。",
        "§c这把霰弹枪无论从哪儿开火，都能像就在面前射击一般，华丽地轰碎呢？",
        "§c说明你仍然记得，并且还会永远铭记下去吧。",
        "§c让你的所爱之人记住，你只不过是个脑袋被击飞的受害者。",
        "§c那就是散落的靶子之一了。",
        "§c既然如此，得连那脑袋也一起打爆才行。",
        "§c你瞥见了那个春日。那对我而言也是难得的回忆。 ",
        "§c不要因过去而做出错误判断。会令我不禁想要破坏一切。",
        "§c为了追寻大义，些许牺牲在所难免。 ",
        "§c怀揣着迈向新时代的心。",
        "§c些许必要的牺牲。",
        "§c请为了大义而牺牲吧。",
        "§c前方射击",
        "§c后方射击",
        "§c极致的悲剧，唯有自己体会方能真正知其痛苦。",
        "§c贯穿",
        "§c开火...无论何时，无论何地！",
        "§c第二次的回忆。",
        "§c既是无法避免的终焉，至少以所爱的那双手…",
        "§c相遇乃走向失去的约定。",
        "§c失去了什么的世界里面，会开出怎样的花呢？",
        "§c他们将会指点着这以友人之血点缀的褴褛而嘲弄吧。愚不可及。",
        "§c即便握起■■，我也终究不愿打开。一位友人或曾这么说过。已太迟了，太迟了……",
        "§c嗯，如此便已足够。所期的目标已然达成。",
        "§c归途不会过于喧嚷，故也应将令人舒适。",
        "§c如此射击贯穿。 ",
        "§c至终，向无底深渊中沉沦吧。 ",
        "§c自魔法阵分割撕裂而来的子弹",
        "§c撕裂回忆的子弹",
        "§c浸透战友之血的子弹",
        "§c新时代即将到来。",      
        "§c请为崭新的理想献身吧。",
};

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
                    "fellbullet", "fellbullet_piercer");
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
            // 预览台词
            if (a.equals("readtext") || a.equals("preview")) {
                String line = DIALOGUE_PARTS[sender.getEntityWorld().rand.nextInt(DIALOGUE_PARTS.length)];
                sender.sendMessage(new TextComponentString("§7[§c凶弹§7] §r" + line));
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
        return null;
    }
}
