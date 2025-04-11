package by.command8913.itemscroller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod("itemscroller")
public class ItemScroller {
    private ContainerScreen<?> containerScreen;
    public static boolean mousePressed = false;
    public static boolean shiftPressed = false;
    public static boolean ctrlPressed = false;
    public static boolean altPressed = false;

    private long lastMoveTime = 0;
    private static long MIN_DELAY = 50;
    private static long MAX_DELAY = 75;
    private long currentDelay = MIN_DELAY;

    public ItemScroller() {
        MinecraftForge.EVENT_BUS.addListener(this::onRender);
        MinecraftForge.EVENT_BUS.addListener(this::onMousePress);
        MinecraftForge.EVENT_BUS.addListener(this::onKeyPress);
        MinecraftForge.EVENT_BUS.addListener(this::onMouseRelease);
        MinecraftForge.EVENT_BUS.addListener(this::onClientChat);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onClientChat(ClientChatEvent event) {
        String message = event.getMessage();

        if (message.startsWith(".speed ")) {
            handleSpeedCommand(event, message);
        }
        else if (message.equalsIgnoreCase(".help")) {
            handleHelpCommand(event);
        }
    }

    private void handleSpeedCommand(ClientChatEvent event, String message) {
        event.setCanceled(true);
        String[] parts = message.split(" ");
        if (parts.length != 2) {
            sendChatMessage("§6Использование: §e.speed <задержка> §7(например: §e.speed 30§7)");
            return;
        }

        try {
            int delay = Integer.parseInt(parts[1]);
            if (delay <= 0) {
                sendChatMessage("§cОшибка: §fЗадержка должна быть больше 0");
                return;
            }

            MIN_DELAY = delay;
            MAX_DELAY = (int)(delay * 1.5);
            sendChatMessage(String.format("§aЗадержка установлена на §e%dмс",delay, MIN_DELAY, MAX_DELAY));
        } catch (NumberFormatException e) {
            sendChatMessage("§cОшибка: §fНекорректный формат числа");
        }
    }

    private void handleHelpCommand(ClientChatEvent event) {
        event.setCanceled(true);
        sendChatMessage("§6=== Помощь по ItemScroller ===");
        sendChatMessage("§e.speed <задержка> §7- Установить задержку");
        sendChatMessage("§eShift + ЛКМ §7- Быстрое перемещение предметов");
        sendChatMessage("§eCtrl + Shift + ЛКМ §7- Переместить все предметы");
        sendChatMessage("§eAlt + Shift + ЛКМ §7- Переместить одинаковые предметы");
        sendChatMessage("§6Автор мода: §ecommand8913");
    }

    private void onMousePress(GuiScreenEvent.MouseClickedEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu != player.inventoryMenu && this.containerScreen != null) {
            Slot activeSlot = this.containerScreen.getSlotUnderMouse();
            if (activeSlot != null) {
                if (ctrlPressed && !altPressed && shiftPressed) {
                    this.moveAll(null, activeSlot.index);
                } else if (altPressed && shiftPressed) {
                    this.moveAll(activeSlot.getItem(), activeSlot.index);
                }
            }
        }
    }

    private void onMouseRelease(InputEvent.MouseInputEvent event) {
        if (event.getButton() == 0) {
            mousePressed = event.getAction() == 1;
        }
    }

    private void onKeyPress(InputEvent.KeyInputEvent event) {
        if (Minecraft.getInstance().player != null) {
            int key = event.getKey();
            boolean isPress = event.getAction() != 0;
            if (key == 340) {
                shiftPressed = isPress;
            } else if (key == 341) {
                ctrlPressed = isPress;
            } else if (key == 342) {
                altPressed = isPress;
            }
        }
    }

    private void onRender(GuiContainerEvent.DrawForeground event) {
        this.containerScreen = (ContainerScreen<?>) event.getGuiContainer();
        Slot activeSlot = this.containerScreen.getSlotUnderMouse();
        if (activeSlot != null) {
            if (altPressed && shiftPressed && mousePressed) {
                this.moveAll(activeSlot.getItem(), activeSlot.index);
            } else if (mousePressed && shiftPressed) {
                this.moveItem(activeSlot);
            }
        }
    }

    private void moveAll(ItemStack item, int slotIndex) {
        if (System.currentTimeMillis() - lastMoveTime < currentDelay) return;

        List<Slot> slotList = this.containerScreen.getMenu().slots;
        int start = (slotIndex < slotList.size() - 36) ? 0 : slotList.size() - 36;
        int end = (slotIndex < slotList.size() - 36) ? slotList.size() - 36 : slotList.size();

        boolean moved = false;
        if (item == null) {
            for (int i = start; i < end && !moved; i++) {
                moved = tryMoveItem(slotList.get(i));
            }
        } else {
            String targetId = item.getDescriptionId();
            for (int i = start; i < end && !moved; i++) {
                Slot slot = slotList.get(i);
                if (slot.getItem().getDescriptionId().equals(targetId)) {
                    moved = tryMoveItem(slot);
                }
            }
        }

        if (moved) {
            currentDelay = MIN_DELAY + (long)(Math.random() * (MAX_DELAY - MIN_DELAY));
            lastMoveTime = System.currentTimeMillis();
        }
    }

    private boolean tryMoveItem(Slot slot) {
        if (System.currentTimeMillis() - lastMoveTime >= currentDelay) {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    slot.index,
                    1,
                    ClickType.QUICK_MOVE,
                    mc.player
            );
            return true;
        }
        return false;
    }

    private void moveItem(Slot slot) {
        if (System.currentTimeMillis() - lastMoveTime >= currentDelay) {
            tryMoveItem(slot);
            currentDelay = MIN_DELAY + (long)(Math.random() * (MAX_DELAY - MIN_DELAY));
            lastMoveTime = System.currentTimeMillis();
        }
    }

    private void sendChatMessage(String message) {
        Minecraft.getInstance().gui.getChat().addMessage(new StringTextComponent(message));
    }
}