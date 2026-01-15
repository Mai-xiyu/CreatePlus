package org.xiyu.yee.createplus.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ChatMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChat(String message, CallbackInfo ci) {
        // 只处理以.give开头的命令，让/give保持原样
        if (message.startsWith(".give ")) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // 检查是否在创造模式
            if (!mc.player.isCreative()) {
                mc.player.displayClientMessage(Component.literal("§c你必须在创造模式下才能使用此命令"), false);
                ci.cancel();
                return;
            }

            // 解析命令参数
            String fullCommand = message.substring(6).trim();
            String[] args = parseCommand(fullCommand);
            if (args == null || args.length < 2) {
                mc.player.displayClientMessage(Component.literal("§c用法: .give <玩家> <物品>[{nbt}] [数量]"), false);
                ci.cancel();
                return;
            }

            try {
                // 获取玩家、物品和数量
                String targetPlayer = args[0];
                String itemWithNbt = args[1];
                int count = args.length > 2 ? Integer.parseInt(args[2]) : 1;

                // 检查是否有原版权限
                boolean hasPermission = mc.player.hasPermissions(2);

                if (hasPermission) {
                    // 有权限，使用原版give命令
                    String vanillaCommand = String.format("give %s %s %d", targetPlayer, itemWithNbt, count);
                    mc.player.connection.sendCommand(vanillaCommand);
                } else {
                    // 无权限，只允许给自己物品
                    if (!targetPlayer.equals(mc.player.getName().getString()) && !targetPlayer.equals("@s")) {
                        mc.player.displayClientMessage(Component.literal("§c无权限给予其他玩家物品"), false);
                        ci.cancel();
                        return;
                    }

                    // 使用NBT方式给予物品
                    ItemStack stack = createItemWithNBT(itemWithNbt, count);
                    if (stack != null) {
                        boolean success = mc.player.getInventory().add(stack);
                        if (success) {
                            mc.player.displayClientMessage(Component.literal(
                                String.format("§a已给予 %s %d 个 %s", 
                                    mc.player.getName().getString(),
                                    count, 
                                    stack.getDisplayName().getString())
                            ), false);
                        } else {
                            mc.player.displayClientMessage(Component.literal("§c物品栏已满"), false);
                        }
                    } else {
                        mc.player.displayClientMessage(Component.literal("§c无效的物品ID或NBT"), false);
                    }
                }
            } catch (NumberFormatException e) {
                mc.player.displayClientMessage(Component.literal("§c无效的数量"), false);
            } catch (Exception e) {
                mc.player.displayClientMessage(Component.literal("§c命令执行失败: " + e.getMessage()), false);
            }

            // 取消原始消息发送
            ci.cancel();
        }
    }

    private String[] parseCommand(String command) {
        try {
            String[] result = new String[3];
            int curIndex = 0;
            StringBuilder current = new StringBuilder();
            boolean inNbt = false;
            int nbtDepth = 0;

            for (char c : command.toCharArray()) {
                if (curIndex >= 3) break;

                if (c == '{' && !inNbt) {
                    inNbt = true;
                    nbtDepth++;
                    current.append(c);
                } else if (inNbt) {
                    current.append(c);
                    if (c == '{') nbtDepth++;
                    if (c == '}') {
                        nbtDepth--;
                        if (nbtDepth == 0) inNbt = false;
                    }
                } else if (c == ' ' && !inNbt) {
                    if (current.length() > 0) {
                        result[curIndex++] = current.toString();
                        current = new StringBuilder();
                    }
                } else {
                    current.append(c);
                }
            }

            if (current.length() > 0 && curIndex < 3) {
                result[curIndex] = current.toString();
            }

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack createItemWithNBT(String itemWithNbt, int count) {
        try {
            // 分离物品ID和NBT
            String itemId;
            CompoundTag nbt = null;

            int nbtStart = itemWithNbt.indexOf('{');
            if (nbtStart != -1) {
                itemId = itemWithNbt.substring(0, nbtStart);
                String nbtString = itemWithNbt.substring(nbtStart);
                nbt = TagParser.parseTag(nbtString);
            } else {
                itemId = itemWithNbt;
            }

            // 创建物品
            var item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
            if (item == null) return null;

            ItemStack stack = new ItemStack(item, count);
            if (nbt != null) {
                stack.setTag(nbt);
            }

            return stack;
        } catch (CommandSyntaxException | IllegalArgumentException e) {
            return null;
        }
    }
} 