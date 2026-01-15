package org.xiyu.yee.createplus.features;

        import net.minecraft.client.Minecraft;
        import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
        import net.minecraft.world.item.ItemStack;

        public class InfinitePlacement extends CreativePlusFeature {
            public InfinitePlacement() {
                super("infinite_placement", "创造模式下物品无限放置");
            }

            @Override
            public void onEnable() {}

            @Override
            public void onDisable() {}

            @Override
            public void onTick() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    ItemStack mainHand = mc.player.getMainHandItem();
                    if (!mainHand.isEmpty() && mainHand.getCount() != 64) {
                        mainHand.setCount(64);
                        mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket(mc.player.getInventory().selected, mainHand));
                    }
                }
            }
        }